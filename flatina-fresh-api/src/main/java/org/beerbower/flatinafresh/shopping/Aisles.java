package org.beerbower.flatinafresh.shopping;

import java.util.List;
import java.util.Locale;

/** Rough store-aisle grouping by keyword, so the list can be shopped in one pass. */
final class Aisles {
    private Aisles() {
    }

    static final String PRODUCE = "Produce";
    static final String MEAT = "Meat & Seafood";
    static final String DAIRY = "Dairy & Eggs";
    static final String BAKERY = "Bakery";
    static final String OTHER = "Pantry & Other";

    static final List<String> ORDER = List.of(PRODUCE, MEAT, DAIRY, BAKERY, OTHER);

    private static final List<String> OTHER_FIRST = List.of("coconut milk", "stock", "broth", "seasoning", "sauce",
            "paste", "vinegar", "powder", "dried", "canned", "concentrate", "spice", "mayonnaise", "ketchup",
            "mustard", "dressing", "jam", "honey");
    private static final List<String> BAKERY_WORDS = List.of("bun", "bread", "tortilla", "roll", "naan", "pita",
            "baguette", "ciabatta", "flatbread", "bagel", "croissant", "panko", "breadcrumbs");
    private static final List<String> MEAT_WORDS = List.of("beef", "chicken", "pork", "sausage", "shrimp", "salmon",
            "turkey", "bacon", "steak", "fish", "tilapia", "cod", "lamb", "chorizo", "prosciutto", "ham", "scallop",
            "meatball", "tenderloin", "thigh", "breast");
    private static final List<String> DAIRY_WORDS = List.of("cheese", "milk", "cream", "butter", "yogurt", "egg",
            "mozzarella", "parmesan", "cheddar", "feta", "ricotta", "monterey jack", "gouda", "mascarpone",
            "creme fraiche", "crème fraîche");
    private static final List<String> PRODUCE_WORDS = List.of("onion", "garlic", "potato", "lemon", "lime",
            "tomato", "bell pepper", "jalapeño", "jalapeno", "poblano", "scallion", "cilantro", "parsley", "basil",
            "thyme", "rosemary", "dill", "mint", "chive", "carrot", "zucchini", "squash", "broccoli", "cauliflower",
            "spinach", "kale", "lettuce", "arugula", "cabbage", "cucumber", "mushroom", "ginger", "shallot", "apple",
            "pear", "avocado", "corn", "green bean", "asparagus", "celery", "sweet potato", "radish", "leek", "orange",
            "berries", "pepper", "greens", "sage", "fennel", "brussels", "bok choy", "eggplant", "peas");

    static String of(String ingredientName) {
        String n = ingredientName.toLowerCase(Locale.ROOT);
        if (containsAny(n, OTHER_FIRST)) {
            return OTHER;
        }
        if (containsAny(n, BAKERY_WORDS)) {
            return BAKERY;
        }
        if (containsAny(n, MEAT_WORDS)) {
            return MEAT;
        }
        if (containsAny(n, DAIRY_WORDS)) {
            return DAIRY;
        }
        if (containsAny(n, PRODUCE_WORDS)) {
            return PRODUCE;
        }
        return OTHER;
    }

    private static boolean containsAny(String text, List<String> words) {
        for (String w : words) {
            if (text.contains(w)) {
                return true;
            }
        }
        return false;
    }
}
