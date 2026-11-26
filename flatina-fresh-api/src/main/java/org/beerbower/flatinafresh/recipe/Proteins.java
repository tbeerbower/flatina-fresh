package org.beerbower.flatinafresh.recipe;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Works out a recipe's main protein from its tags and ingredient names, so recipes can be filtered by it.
 * Tags win over ingredients (a card tagged "vegetarian" is vegetarian even if it uses chicken stock).
 */
public final class Proteins {
    public static final List<String> ALL = List.of("beef", "pork", "chicken", "turkey", "lamb", "seafood",
            "vegetarian");

    private static final Map<String, Pattern> INGREDIENT_PATTERNS = new LinkedHashMap<>();

    static {
        // Order matters: the first match wins, so specific meats come before generic words.
        INGREDIENT_PATTERNS.put("turkey", words("turkey"));
        INGREDIENT_PATTERNS.put("chicken", words("chicken breasts?", "chicken thighs?", "chicken tenders?",
                "chicken cutlets?", "ground chicken", "chicken sausages?", "chicken drumsticks?"));
        INGREDIENT_PATTERNS.put("beef", words("beef(?! (stock|broth|bouillon|base))", "steak", "sirloin", "ribeye", "flank", "brisket"));
        INGREDIENT_PATTERNS.put("pork", words("pork", "bacon", "pancetta", "prosciutto", "chorizo", "ham",
                "italian sausage", "pork sausage", "carnitas"));
        INGREDIENT_PATTERNS.put("lamb", words("lamb"));
        INGREDIENT_PATTERNS.put("seafood", words("shrimp", "salmon", "cod", "tilapia", "tuna", "fish(?! sauce)", "scallops?",
                "barramundi", "trout", "haddock", "mahi", "crab", "mussels", "prawns?"));
    }

    private Proteins() {
    }

    /** The recipe's main protein, or null if nothing points to one. */
    public static String classify(String tags, Collection<String> ingredientNames) {
        String fromTags = fromTags(tags);
        if (fromTags != null) {
            return fromTags;
        }
        for (var e : INGREDIENT_PATTERNS.entrySet()) {
            for (String name : ingredientNames) {
                if (name != null && e.getValue().matcher(name).find()) {
                    return e.getKey();
                }
            }
        }
        // No meat or fish among the ingredients: call it vegetarian if there are ingredients at all
        return ingredientNames.isEmpty() ? null : "vegetarian";
    }

    /** Normalizes a user-entered protein, or null if blank. */
    public static String normalize(String protein) {
        if (protein == null || protein.isBlank()) {
            return null;
        }
        String p = protein.trim().toLowerCase(Locale.ROOT);
        return switch (p) {
            case "veg", "veggie", "vegan", "meatless" -> "vegetarian";
            case "fish", "shrimp" -> "seafood";
            default -> p;
        };
    }

    private static String fromTags(String tags) {
        if (tags == null) {
            return null;
        }
        for (String raw : tags.split(",")) {
            String tag = normalize(raw);
            if (tag != null && ALL.contains(tag)) {
                return tag;
            }
        }
        return null;
    }

    private static Pattern words(String... alternatives) {
        return Pattern.compile("\\b(" + String.join("|", alternatives) + ")\\b", Pattern.CASE_INSENSITIVE);
    }
}
