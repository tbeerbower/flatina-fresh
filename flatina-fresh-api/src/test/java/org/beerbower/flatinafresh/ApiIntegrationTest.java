package org.beerbower.flatinafresh;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.beerbower.flatinafresh.family.FamilyService;
import org.beerbower.flatinafresh.scan.RecipeCardReader;
import org.beerbower.flatinafresh.scan.ScannedRecipe;
import org.beerbower.flatinafresh.user.AdminUserInitializer;
import org.beerbower.flatinafresh.user.AppUser;
import org.beerbower.flatinafresh.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
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
    @Autowired
    AppUserRepository users;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    FamilyService familyService;

    String token;

    @BeforeEach
    void login() throws Exception {
        token = loginAs("admin@example.com", "test-password");
    }

    String loginAs(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    @Test
    void rejectsUnauthenticatedAndBadPasswords() throws Exception {
        mvc.perform(get("/api/recipes")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@example.com\",\"password\":\"nope\"}")).andExpect(status().isUnauthorized());
        // The admin account has an email, so its username no longer signs in
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin\",\"password\":\"test-password\"}")).andExpect(status().isUnauthorized());
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
                assertThat(n.get("protein").asText()).isEqualTo("vegetarian");
                assertThat(n.get("ingredients")).hasSize(2);
            }
        }

        plan("2026-11-30", tacosId, 4);
        plan("2026-12-01", pieId, 2);
        plan("2026-12-09", pieId, 2); // outside the week

        JsonNode summaries = json.readTree(mvc.perform(get("/api/recipes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        for (JsonNode n : summaries) {
            if (n.get("id").asLong() == tacosId) {
                assertThat(n.get("protein").asText()).isEqualTo("pork");
                assertThat(n.get("ingredients")).hasSize(2); // pantry olive oil left out
                assertThat(n.get("timesPlanned").asLong()).isEqualTo(1);
            }
            if (n.get("id").asLong() == pieId) {
                assertThat(n.get("timesPlanned").asLong()).isEqualTo(2);
                assertThat(n.get("lastPlanned").asText()).isEqualTo("2026-12-09");
            }
        }

        // Weeks start on Monday; a mid-week start date snaps back to it, and empty weeks are included
        JsonNode weeks = json.readTree(mvc.perform(get("/api/plan/weeks?start=2026-12-02&count=3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(weeks).hasSize(3);
        assertThat(weeks.get(0).get("start").asText()).isEqualTo("2026-11-30");
        assertThat(weeks.get(0).get("entries")).hasSize(2);
        assertThat(weeks.get(0).get("entries").get(0).get("coverImageId").isNull()).isFalse();
        assertThat(weeks.get(1).get("entries")).hasSize(1);
        assertThat(weeks.get(2).get("entries")).isEmpty();
        mvc.perform(get("/api/plan/weeks?start=2026-12-02&count=61").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

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
        assertThat(find(items, "Sour Cream").get("product").isNull()).isTrue();

        // Save products: sour cream in quarter-cup tubs, and onions as a staple
        String saved = mvc.perform(post("/api/products").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ingredientName":"sour creams","brand":"Daisy","productName":"Sour Cream",
                                 "packageSize":0.25,"packageUnit":"cups","packageLabel":"tub","storeItem":"12345"}
                                """))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(saved).get("ingredientKey").asText()).isEqualTo("sour cream");
        assertThat(json.readTree(saved).get("packageUnit").asText()).isEqualTo("cup");
        mvc.perform(post("/api/products").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredientName\":\"Yellow Onion\",\"staple\":true}"))
                .andExpect(status().isOk());
        // Saving the same ingredient again replaces it rather than adding a second
        mvc.perform(post("/api/products").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ingredientName":"Sour Cream","brand":"Daisy","productName":"Sour Cream",
                                 "packageSize":0.25,"packageUnit":"cup","packageLabel":"tub"}
                                """))
                .andExpect(status().isOk());
        JsonNode products = json.readTree(mvc.perform(get("/api/products").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(products).hasSize(2);

        items = json.readTree(mvc.perform(get("/api/shopping-list?start=2026-11-30&end=2026-12-06")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("items");
        // 6 tbsp in quarter-cup tubs: 2 tubs
        assertThat(find(items, "Sour Cream").get("packages").asInt()).isEqualTo(2);
        assertThat(find(items, "Sour Cream").get("product").get("brand").asText()).isEqualTo("Daisy");
        assertThat(find(items, "Yellow Onion").get("staple").asBoolean()).isTrue();
        assertThat(find(items, "Yellow Onion").get("aisle").asText()).isEqualTo("Pantry");
        assertThat(find(items, "Yellow Onion").get("packages").isNull()).isTrue();

        mvc.perform(delete("/api/products/" + products.get(0).get("id").asLong())
                .header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());
        mvc.perform(post("/api/products").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"ingredientName\":\" \"}"))
                .andExpect(status().isBadRequest());
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
                        .content("{\"email\":\"flatina@example.com\",\"username\":\"flatina\",\"password\":\"secret-pass\",\"admin\":false}"))
                .andExpect(status().isCreated());
        String userToken = loginAs("FLATINA@example.com ", "secret-pass");
        mvc.perform(get("/api/recipes").header("Authorization", "Bearer " + userToken)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void familyMembersShareThePlanAndInvitesCreateAccounts() throws Exception {
        MockMultipartFile recipesFile = new MockMultipartFile("recipes", "recipes.json", "application/json",
                "[{\"title\":\"Family Soup\",\"ingredients\":[],\"steps\":[]}]".getBytes());
        mvc.perform(multipart("/api/admin/recipes/import").file(recipesFile).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        long soupId = 0;
        for (JsonNode n : getJson("/api/recipes", token)) {
            if (n.get("title").asText().equals("Family Soup")) {
                soupId = n.get("id").asLong();
            }
        }
        long entryId = json.readTree(mvc.perform(post("/api/plan").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2027-01-04\",\"recipeId\":" + soupId + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();

        // Invite link: anyone with it can see the family's name and create an account in it, once
        JsonNode invite = json.readTree(mvc.perform(post("/api/family/invites").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String link = "/api/invites/" + invite.get("token").asText();
        assertThat(getJson("/api/family", token).get("invites")).hasSize(1);
        JsonNode info = json.readTree(mvc.perform(get(link)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(info.get("familyName").asText()).isEqualTo("admin's family");
        mvc.perform(post(link + "/accept").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"spouse\",\"password\":\"spouse-pass\"}"))
                .andExpect(status().isBadRequest()); // email is required
        mvc.perform(post(link + "/accept").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ADMIN@example.com\",\"username\":\"spouse\",\"password\":\"spouse-pass\"}"))
                .andExpect(status().isConflict()); // email taken (in any case); the link is still good
        String accepted = mvc.perform(post(link + "/accept").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"spouse@example.com\",\"username\":\"spouse\",\"password\":\"spouse-pass\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String spouse = json.readTree(accepted).get("token").asText();
        mvc.perform(post(link + "/accept").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"spouse2@example.com\",\"username\":\"spouse2\",\"password\":\"spouse-pass\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get(link)).andExpect(status().isNotFound());

        JsonNode family = getJson("/api/family", spouse);
        assertThat(family.get("members")).hasSize(2);
        assertThat(family.get("invites")).isEmpty();

        // The new member sees and changes the same plan, but not past weeks (today is 2026-11-26)
        assertThat(getJson("/api/plan?start=2027-01-04&end=2027-01-10", spouse)).hasSize(1);
        mvc.perform(put("/api/plan/" + entryId).header("Authorization", "Bearer " + spouse)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"date\":\"2027-01-04\",\"servings\":4}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/plan").header("Authorization", "Bearer " + spouse).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-11-23\",\"recipeId\":" + soupId + "}"))
                .andExpect(status().isCreated()); // this week
        mvc.perform(post("/api/plan").header("Authorization", "Bearer " + spouse).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-11-16\",\"recipeId\":" + soupId + "}"))
                .andExpect(status().isBadRequest()); // last week

        // Someone in another family sees none of it
        mvc.perform(post("/api/admin/users").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"neighbor@example.com\",\"username\":\"spouse\",\"password\":\"neighbor-pass\",\"admin\":false}"))
                .andExpect(status().isCreated()); // names needn't be unique
        String neighbor = loginAs("neighbor@example.com", "neighbor-pass");
        mvc.perform(post("/api/products").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredientName\":\"Family Broth\",\"brand\":\"Ours\",\"staple\":true}"))
                .andExpect(status().isOk());
        for (JsonNode p : getJson("/api/products", neighbor)) {
            assertThat(p.get("ingredientName").asText()).isNotEqualTo("Family Broth");
        }
        assertThat(getJson("/api/plan?start=2027-01-04&end=2027-01-10", neighbor)).isEmpty();
        assertThat(getJson("/api/family", neighbor).get("members")).hasSize(1);
        mvc.perform(delete("/api/plan/" + entryId).header("Authorization", "Bearer " + neighbor))
                .andExpect(status().isNotFound());

        // Removing a member moves them to a family of their own
        long spouseId = 0;
        for (JsonNode m : family.get("members")) {
            if (m.get("username").asText().equals("spouse")) {
                spouseId = m.get("id").asLong();
            }
        }
        mvc.perform(delete("/api/family/members/" + spouseId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        assertThat(getJson("/api/plan?start=2027-01-04&end=2027-01-10", spouse)).isEmpty();
        assertThat(getJson("/api/plan?start=2027-01-04&end=2027-01-10", token)).hasSize(1);
    }

    @Test
    void accountsFromBeforeEmailsSignInWithUsernameUntilTheyAddOne() throws Exception {
        AppUser legacy = new AppUser();
        legacy.setUsername("oldtimer");
        legacy.setDisplayName("Old Timer");
        legacy.setPasswordHash(passwordEncoder.encode("oldtimer-pass"));
        familyService.startFamily(legacy);

        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"OldTimer\",\"password\":\"oldtimer-pass\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode login = json.readTree(body);
        assertThat(login.get("user").get("email").isNull()).isTrue(); // the app asks for one
        String legacyToken = login.get("token").asText();

        mvc.perform(put("/api/auth/me").header("Authorization", "Bearer " + legacyToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/auth/me").header("Authorization", "Bearer " + legacyToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"admin@EXAMPLE.com\"}"))
                .andExpect(status().isConflict());
        JsonNode me = json.readTree(mvc.perform(put("/api/auth/me").header("Authorization", "Bearer " + legacyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"Old.Timer@Example.com\",\"name\":\"Oldie\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(me.get("email").asText()).isEqualTo("old.timer@example.com");
        assertThat(me.get("displayName").asText()).isEqualTo("Oldie");

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"oldtimer\",\"password\":\"oldtimer-pass\"}"))
                .andExpect(status().isUnauthorized());
        String fresh = loginAs("old.timer@example.com", "oldtimer-pass");
        assertThat(getJson("/api/auth/me", fresh).get("displayName").asText()).isEqualTo("Oldie");
    }

    @Test
    void adminEmailSettingIsAddedToTheAdminsAccountFromBeforeEmails() throws Exception {
        AppUser legacy = new AppUser();
        legacy.setUsername("boss");
        legacy.setPasswordHash(passwordEncoder.encode("boss-pass"));
        familyService.startFamily(legacy);

        AdminUserInitializer init = new AdminUserInitializer(users, passwordEncoder, familyService);
        ReflectionTestUtils.setField(init, "username", "boss");
        ReflectionTestUtils.setField(init, "email", "Boss@Example.com");
        ReflectionTestUtils.setField(init, "name", "The Boss");
        ReflectionTestUtils.setField(init, "password", "ignored-pass");
        init.run(null);
        init.run(null); // a second start changes nothing

        JsonNode me = getJson("/api/auth/me", loginAs("boss@example.com", "boss-pass"));
        assertThat(me.get("displayName").asText()).isEqualTo("The Boss");
        assertThat(me.get("admin").asBoolean()).isTrue();
        assertThat(users.findAll().stream().filter(u -> "boss@example.com".equals(u.getEmail())).count()).isEqualTo(1);
    }

    private JsonNode getJson(String url, String bearer) throws Exception {
        return json.readTree(mvc.perform(get(url).header("Authorization", "Bearer " + bearer))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
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
