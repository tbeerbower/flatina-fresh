package org.beerbower.flatinafresh;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.beerbower.flatinafresh.scan.RecipeCardReader;
import org.beerbower.flatinafresh.scan.ScannedRecipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @MockitoBean
    RecipeCardReader cardReader;

    String token;

    @BeforeEach
    void login() throws Exception {
        token = loginAs("admin", "test-password");
    }

    String loginAs(String user, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + user + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    @Test
    void rejectsUnauthenticatedAndBadPasswords() throws Exception {
        mvc.perform(get("/api/recipes")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"nope\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void importPlanAndShop() throws Exception {
        String recipes = """
                [{"title":"Test Tacos","totalMinutes":30,"images":["front.jpg","missing.jpg"],
                  "ingredients":[
                    {"name":"Ground Pork","quantity2":10,"quantity4":20,"unit":"oz"},
                    {"name":"Sour Cream","quantity2":2,"quantity4":4,"unit":"tbsp"},
                    {"name":"Olive Oil","quantity2":2,"quantity4":2,"unit":"tsp","pantry":true}],
                  "steps":[{"title":"Cook","instructions":"Cook it."}]},
                 {"title":"Test Pie","ingredients":[
                    {"name":"Sour Cream","quantity2":2,"quantity4":4,"unit":"TBSP"},
                    {"name":"Yellow Onion","quantity2":1,"quantity4":1}],
                  "steps":[]}]
                """;
        MockMultipartFile recipesFile = new MockMultipartFile("recipes", "recipes.json", "application/json",
                recipes.getBytes());
        MockMultipartFile image = new MockMultipartFile("images", "front.jpg", "image/jpeg", tinyJpeg());
        String result = mvc.perform(multipart("/api/admin/recipes/import").file(recipesFile).file(image)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode r = json.readTree(result);
        assertThat(r.get("imported")).hasSize(2);
        assertThat(r.get("missingImages").get(0).asText()).isEqualTo("missing.jpg");

        // Importing again skips existing titles
        String again = mvc.perform(multipart("/api/admin/recipes/import").file(recipesFile)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(again).get("skipped")).hasSize(2);

        JsonNode list = json.readTree(mvc.perform(get("/api/recipes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        long tacosId = 0, pieId = 0;
        for (JsonNode n : list) {
            if (n.get("title").asText().equals("Test Tacos")) {
                tacosId = n.get("id").asLong();
                assertThat(n.get("coverImageId").isNull()).isFalse();
                mvc.perform(get("/api/recipes/" + tacosId + "/images/" + n.get("coverImageId").asLong())
                        .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
            }
            if (n.get("title").asText().equals("Test Pie")) {
                pieId = n.get("id").asLong();
            }
        }

        plan("2026-11-30", tacosId, 4);
        plan("2026-12-01", pieId, 2);
        plan("2026-12-09", pieId, 2); // outside the week

        JsonNode shopping = json.readTree(mvc.perform(get("/api/shopping-list?start=2026-11-30&end=2026-12-06")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(shopping.get("meals")).hasSize(2);
        JsonNode items = shopping.get("items");
        JsonNode sourCream = find(items, "Sour Cream");
        // 4 tbsp (tacos for 4) + 2 tbsp (pie for 2) = 6 tbsp = 18 tsp -> 0.38 cup
        assertThat(sourCream.get("amounts").get(0).get("unit").asText()).isEqualTo("cup");
        assertThat(sourCream.get("amounts").get(0).get("quantity").decimalValue())
                .isEqualByComparingTo(new BigDecimal("0.38"));
        assertThat(sourCream.get("recipes")).hasSize(2);
        assertThat(find(items, "Ground Pork").get("amounts").get(0).get("quantity").decimalValue())
                .isEqualByComparingTo("20");
        assertThat(find(items, "Ground Pork").get("aisle").asText()).isEqualTo("Meat & Seafood");
        assertThat(find(items, "Olive Oil").get("pantry").asBoolean()).isTrue();
    }

    @Test
    void scanCreatesRecipeWithImages() throws Exception {
        when(cardReader.read(anyList())).thenReturn(new ScannedRecipe("Scanned Burgers", "with Fries", null, 10, 35,
                990, List.of("beef"), List.of("Baking sheet"),
                List.of(new ScannedRecipe.Item("Ground Beef", new BigDecimal("10"), new BigDecimal("20"), "oz", null)),
                List.of(new ScannedRecipe.Item("Vegetable Oil", BigDecimal.ONE, BigDecimal.ONE, "tbsp", null)),
                List.of(new ScannedRecipe.Step("Prep", "Preheat oven.")), null));
        String body = mvc.perform(multipart("/api/recipes/scan")
                        .file(new MockMultipartFile("images", "front.jpg", "image/jpeg", tinyJpeg()))
                        .file(new MockMultipartFile("images", "back.png", "image/png", tinyPng()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode recipe = json.readTree(body);
        assertThat(recipe.get("title").asText()).isEqualTo("Scanned Burgers");
        assertThat(recipe.get("imageIds")).hasSize(2);
        assertThat(recipe.get("ingredients")).hasSize(2);
        assertThat(recipe.get("ingredients").get(1).get("pantry").asBoolean()).isTrue();
    }

    @Test
    void adminCanAddUsersWhoCannotAdminister() throws Exception {
        mvc.perform(post("/api/admin/users").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"flatina\",\"password\":\"secret-pass\",\"admin\":false}"))
                .andExpect(status().isCreated());
        String userToken = loginAs("flatina", "secret-pass");
        mvc.perform(get("/api/recipes").header("Authorization", "Bearer " + userToken)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    private void plan(String date, long recipeId, int servings) throws Exception {
        mvc.perform(post("/api/plan").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"" + date + "\",\"recipeId\":" + recipeId + ",\"servings\":" + servings + "}"))
                .andExpect(status().isCreated());
    }

    private static JsonNode find(JsonNode items, String name) {
        for (JsonNode n : items) {
            if (n.get("name").asText().equals(name)) {
                return n;
            }
        }
        throw new AssertionError("No item " + name + " in " + items);
    }

    private static byte[] tinyJpeg() throws Exception {
        return encode("jpeg");
    }

    private static byte[] tinyPng() throws Exception {
        return encode("png");
    }

    private static byte[] encode(String format) throws Exception {
        BufferedImage img = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, format, out);
        return out.toByteArray();
    }
}
