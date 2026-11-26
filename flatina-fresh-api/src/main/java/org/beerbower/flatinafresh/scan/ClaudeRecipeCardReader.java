package org.beerbower.flatinafresh.scan;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Uses Claude's vision support with a JSON schema output to read HelloFresh-style recipe cards. */
@Slf4j
@Component
public class ClaudeRecipeCardReader implements RecipeCardReader {

    static final String SYSTEM_PROMPT = """
            You read scanned meal-kit recipe cards (usually HelloFresh) and transcribe them into structured data.
            You are given every side of one card: the front has the title, subtitle, a short blurb, prep/total time,
            calories and pictures of the shipped ingredients; the back has the "Bust out" list, the ingredient table
            with 2-person and 4-person amounts, and the numbered steps.

            Rules:
            - Transcribe; don't invent. Use null for anything the card doesn't show.
            - title: the main headline in normal title case (not ALL CAPS). subtitle: the "with ..." line.
            - description: the short blurb on the front (for example the "HELLO ..." box), as one sentence or two.
            - ingredients: every row of the ingredient table, in order. quantity2 is the 2-person amount and quantity4
              the 4-person amount, as decimal numbers (½ = 0.5, ¼ = 0.25, "1 | 1" = 1 and 1). unit is the unit in
              lower case and singular where natural (oz, tbsp, tsp, cup, clove, lb, ml, g, slice, can, pinch); use
              null for counted items with no unit (1 red onion, 2 buns). Put allergen notes such as
              "Contains: Milk, Wheat" (from the front of the card) in allergens, without the word "Contains".
              Footnote markers like * are dropped from names.
            - pantryItems: consumable items from the "Bust out" list (oil, butter, sugar, salt, pepper, flour, eggs...)
              with amounts when shown, like "Vegetable oil (1 TBSP | 1 TBSP)". Salt and pepper usually have no amount.
            - equipment: tools from the "Bust out" list (baking sheet, large pan, small bowls...).
            - steps: each numbered step with its heading (e.g. "Roast Potatoes") as title and the full text as
              instructions, including amounts in parentheses. Keep the card's wording.
            - notes: short extra tips from the card worth keeping (for example "Make it zing" or food safety
              temperatures), each on its own line. null if none.
            - tags: 1 to 4 short lowercase tags such as the main protein, cuisine or dish type ("beef", "burger").
            """;

    private final ScanProperties properties;
    private final ObjectMapper objectMapper;
    private volatile AnthropicClient client;

    public ClaudeRecipeCardReader(ScanProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public ScannedRecipe read(List<byte[]> jpegImages) {
        List<ContentBlockParam> content = new ArrayList<>();
        for (int i = 0; i < jpegImages.size(); i++) {
            content.add(ContentBlockParam.ofText(TextBlockParam.builder()
                    .text("Card image " + (i + 1) + " of " + jpegImages.size() + ":").build()));
            content.add(ContentBlockParam.ofImage(ImageBlockParam.builder()
                    .source(Base64ImageSource.builder()
                            .mediaType(Base64ImageSource.MediaType.IMAGE_JPEG)
                            .data(Base64.getEncoder().encodeToString(jpegImages.get(i)))
                            .build())
                    .build()));
        }
        content.add(ContentBlockParam.ofText(TextBlockParam.builder()
                .text("Transcribe this recipe card.").build()));

        MessageCreateParams params = MessageCreateParams.builder()
                .model(properties.model())
                .maxTokens(16000L)
                .system(SYSTEM_PROMPT)
                .outputConfig(OutputConfig.builder()
                        .effort(OutputConfig.Effort.LOW)
                        .format(JsonOutputFormat.builder().schema(schema()).build())
                        .build())
                .addUserMessageOfContentBlockParams(content)
                .build();

        Message response;
        try {
            response = client().messages().create(params);
        } catch (AnthropicException e) {
            log.warn("Recipe card scan failed", e);
            throw new ScanFailedException("The card reader service failed: " + e.getMessage(), e);
        }

        if (response.stopReason().map(StopReason.REFUSAL::equals).orElse(false)) {
            throw new ScanFailedException("The card reader declined to read these images.");
        }
        if (response.stopReason().map(StopReason.MAX_TOKENS::equals).orElse(false)) {
            throw new ScanFailedException("The card reader ran out of room before finishing. Try again.");
        }

        String json = response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(TextBlock::text)
                .collect(Collectors.joining());
        try {
            return objectMapper.readValue(json, ScannedRecipe.class);
        } catch (JsonProcessingException e) {
            log.warn("Unparseable scan result: {}", json);
            throw new ScanFailedException("The card reader returned data that could not be read.", e);
        }
    }

    private AnthropicClient client() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    if (!StringUtils.hasText(properties.anthropicApiKey())) {
                        throw new ScanFailedException(
                                "Card scanning isn't configured: set ANTHROPIC_API_KEY on the API service.");
                    }
                    client = AnthropicOkHttpClient.builder()
                            .apiKey(properties.anthropicApiKey())
                            .timeout(Duration.ofMinutes(3))
                            .build();
                }
            }
        }
        return client;
    }

    static JsonOutputFormat.Schema schema() {
        Map<String, Object> item = object(Map.of(
                "name", type("string"),
                "quantity2", nullable("number"),
                "quantity4", nullable("number"),
                "unit", nullable("string"),
                "allergens", nullable("string")));
        Map<String, Object> step = object(Map.of(
                "title", nullable("string"),
                "instructions", type("string")));

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("title", type("string"));
        props.put("subtitle", nullable("string"));
        props.put("description", nullable("string"));
        props.put("prepMinutes", nullable("integer"));
        props.put("totalMinutes", nullable("integer"));
        props.put("calories", nullable("integer"));
        props.put("tags", array(type("string")));
        props.put("equipment", array(type("string")));
        props.put("ingredients", array(item));
        props.put("pantryItems", array(item));
        props.put("steps", array(step));
        props.put("notes", nullable("string"));
        Map<String, Object> root = object(props);

        JsonOutputFormat.Schema.Builder builder = JsonOutputFormat.Schema.builder();
        root.forEach((k, v) -> builder.putAdditionalProperty(k, JsonValue.from(v)));
        return builder.build();
    }

    private static Map<String, Object> type(String type) {
        return Map.of("type", type);
    }

    private static Map<String, Object> nullable(String type) {
        return Map.of("type", List.of(type, "null"));
    }

    private static Map<String, Object> array(Map<String, Object> items) {
        return Map.of("type", "array", "items", items);
    }

    private static Map<String, Object> object(Map<String, Object> properties) {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("type", "object");
        o.put("properties", properties);
        o.put("required", List.copyOf(properties.keySet()));
        o.put("additionalProperties", false);
        return o;
    }
}
