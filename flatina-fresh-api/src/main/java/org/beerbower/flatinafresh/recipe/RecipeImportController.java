package org.beerbower.flatinafresh.recipe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.beerbower.flatinafresh.recipe.RecipeDtos.RecipeDto;
import org.beerbower.flatinafresh.scan.ImageProcessor;
import org.beerbower.flatinafresh.scan.ScanProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;

/**
 * Bulk import of recipes from a JSON file (an array of recipes, each optionally naming its image files in
 * "images"), with the image files uploaded alongside. Recipes whose title already exists are skipped, so the
 * same bundle can be imported twice safely.
 */
@RestController
@RequestMapping("/api/admin/recipes/import")
@RequiredArgsConstructor
public class RecipeImportController {
    private final RecipeService service;
    private final ObjectMapper objectMapper;
    private final ImageProcessor imageProcessor;
    private final ScanProperties scanProperties;

    public record ImportResult(List<String> imported, List<String> skipped, List<String> missingImages) {
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importRecipes(@RequestParam("recipes") MultipartFile recipesJson,
                                      @RequestParam(value = "images", required = false) List<MultipartFile> images)
            throws IOException {
        JsonNode root = objectMapper.readTree(recipesJson.getBytes());
        if (root == null || !root.isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expected a JSON array of recipes");
        }
        Map<String, MultipartFile> imagesByName = new HashMap<>();
        if (images != null) {
            for (MultipartFile image : images) {
                if (image.getOriginalFilename() != null) {
                    imagesByName.put(baseName(image.getOriginalFilename()), image);
                }
            }
        }

        List<String> imported = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (JsonNode node : root) {
            ObjectNode recipeNode = (ObjectNode) node.deepCopy();
            JsonNode imageNames = recipeNode.remove("images");
            RecipeDto dto = objectMapper.treeToValue(recipeNode, RecipeDto.class);
            if (dto.title() == null || dto.title().isBlank()) {
                continue;
            }
            if (service.existsByTitle(dto.title())) {
                skipped.add(dto.title());
                continue;
            }
            RecipeDto created = service.create(dto);
            imported.add(created.title());
            if (imageNames != null) {
                for (JsonNode name : imageNames) {
                    MultipartFile file = imagesByName.get(baseName(name.asText()));
                    if (file == null) {
                        missing.add(name.asText());
                        continue;
                    }
                    byte[] jpeg = imageProcessor.toJpeg(imageProcessor.read(file.getBytes()),
                            scanProperties.storedImagePx(), 0.82f);
                    service.appendImage(created.id(), "image/jpeg", jpeg);
                }
            }
        }
        return new ImportResult(imported, skipped, missing);
    }

    private static String baseName(String path) {
        String name = path.replace('\\', '/');
        return name.substring(name.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
    }
}
