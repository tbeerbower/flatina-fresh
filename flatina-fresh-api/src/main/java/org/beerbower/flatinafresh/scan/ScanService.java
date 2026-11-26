package org.beerbower.flatinafresh.scan;

import lombok.RequiredArgsConstructor;
import org.beerbower.flatinafresh.recipe.RecipeDtos.IngredientDto;
import org.beerbower.flatinafresh.recipe.RecipeDtos.RecipeDto;
import org.beerbower.flatinafresh.recipe.RecipeDtos.StepDto;
import org.beerbower.flatinafresh.recipe.RecipeService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Turns uploaded card scans into a saved recipe (with the scans attached) for the user to review. */
@Service
@RequiredArgsConstructor
public class ScanService {
    static final int MAX_IMAGES = 4;

    private final RecipeCardReader reader;
    private final ImageProcessor imageProcessor;
    private final RecipeService recipeService;
    private final ScanProperties properties;

    public RecipeDto scan(List<MultipartFile> files) {
        List<MultipartFile> images = files == null ? List.of() : files.stream().filter(f -> !f.isEmpty()).toList();
        if (images.isEmpty()) {
            throw new InvalidScanException("Upload at least one image of the recipe card.");
        }
        if (images.size() > MAX_IMAGES) {
            throw new InvalidScanException("Upload at most " + MAX_IMAGES + " images per card.");
        }

        List<byte[]> forModel = new ArrayList<>();
        List<byte[]> forStorage = new ArrayList<>();
        for (MultipartFile file : images) {
            BufferedImage image;
            try {
                image = imageProcessor.read(file.getBytes());
            } catch (IOException e) {
                throw new InvalidScanException("Could not read upload " + file.getOriginalFilename());
            }
            forModel.add(imageProcessor.toJpeg(image, properties.maxImagePx(), 0.88f));
            forStorage.add(imageProcessor.toJpeg(image, properties.storedImagePx(), 0.82f));
        }

        ScannedRecipe scanned = reader.read(forModel);
        RecipeDto created = recipeService.create(toRecipe(scanned));
        for (int i = 0; i < forStorage.size(); i++) {
            recipeService.addImage(created.id(), i, "image/jpeg", forStorage.get(i));
        }
        return recipeService.get(created.id());
    }

    static RecipeDto toRecipe(ScannedRecipe s) {
        List<IngredientDto> ingredients = Stream.concat(
                        nullSafe(s.ingredients()).stream().map(i -> toIngredient(i, false)),
                        nullSafe(s.pantryItems()).stream().map(i -> toIngredient(i, true)))
                .filter(i -> i.name() != null && !i.name().isBlank())
                .toList();
        List<StepDto> steps = nullSafe(s.steps()).stream()
                .filter(st -> st.instructions() != null && !st.instructions().isBlank())
                .map(st -> new StepDto(null, st.title(), st.instructions()))
                .toList();
        String title = s.title() == null || s.title().isBlank() ? "Untitled recipe" : s.title().trim();
        return new RecipeDto(null, title, s.subtitle(), s.description(), "HelloFresh", s.prepMinutes(),
                s.totalMinutes(), s.calories(), join(s.tags()), join(s.equipment()), s.notes(), ingredients, steps,
                List.of());
    }

    private static IngredientDto toIngredient(ScannedRecipe.Item i, boolean pantry) {
        return new IngredientDto(null, i.name() == null ? null : i.name().trim(), i.quantity2(), i.quantity4(),
                i.unit(), i.allergens(), pantry);
    }

    private static String join(List<String> values) {
        List<String> list = nullSafe(values).stream().filter(v -> v != null && !v.isBlank()).map(String::trim).toList();
        return list.isEmpty() ? null : String.join(", ", list);
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }
}
