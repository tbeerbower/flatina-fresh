package org.beerbower.flatinafresh.recipe;

import lombok.RequiredArgsConstructor;
import org.beerbower.flatinafresh.recipe.RecipeDtos.IngredientDto;
import org.beerbower.flatinafresh.recipe.RecipeDtos.RecipeDto;
import org.beerbower.flatinafresh.recipe.RecipeDtos.RecipeSummaryDto;
import org.beerbower.flatinafresh.recipe.RecipeDtos.StepDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecipeService {
    private final RecipeRepository recipes;
    private final RecipeImageRepository images;

    @Transactional(readOnly = true)
    public List<RecipeSummaryDto> list(String query) {
        String q = StringUtils.hasText(query) ? query.trim() : null;
        return (q == null ? recipes.findAllOrderByTitle() : recipes.search(q)).stream().map(r -> {
            List<Long> imageIds = images.findIdsByRecipeId(r.getId());
            return new RecipeSummaryDto(r.getId(), r.getTitle(), r.getSubtitle(), r.getPrepMinutes(),
                    r.getTotalMinutes(), r.getCalories(), r.getTags(), imageIds.isEmpty() ? null : imageIds.get(0));
        }).toList();
    }

    @Transactional(readOnly = true)
    public RecipeDto get(Long id) {
        return toDto(find(id));
    }

    @Transactional
    public RecipeDto create(RecipeDto dto) {
        Recipe recipe = new Recipe();
        apply(recipe, dto);
        return toDto(recipes.save(recipe));
    }

    @Transactional
    public RecipeDto update(Long id, RecipeDto dto) {
        Recipe recipe = find(id);
        apply(recipe, dto);
        return toDto(recipes.save(recipe));
    }

    @Transactional
    public void delete(Long id) {
        recipes.delete(find(id));
    }

    @Transactional
    public void addImage(Long recipeId, int position, String contentType, byte[] data) {
        RecipeImage image = new RecipeImage();
        image.setRecipeId(recipeId);
        image.setPosition(position);
        image.setContentType(contentType);
        image.setData(data);
        images.save(image);
    }

    /** Appends an image after the recipe's existing ones. */
    @Transactional
    public Long appendImage(Long recipeId, String contentType, byte[] data) {
        find(recipeId);
        RecipeImage image = new RecipeImage();
        image.setRecipeId(recipeId);
        image.setPosition(images.findMaxPosition(recipeId) + 1);
        image.setContentType(contentType);
        image.setData(data);
        return images.save(image).getId();
    }

    @Transactional
    public void deleteImage(Long recipeId, Long imageId) {
        images.delete(images.findByIdAndRecipeId(imageId, recipeId)
                .orElseThrow(() -> new NotFoundException("Image not found")));
    }

    @Transactional(readOnly = true)
    public boolean existsByTitle(String title) {
        return recipes.existsByTitleIgnoreCase(title.trim());
    }

    @Transactional(readOnly = true)
    public RecipeImage getImage(Long recipeId, Long imageId) {
        RecipeImage image = images.findByIdAndRecipeId(imageId, recipeId)
                .orElseThrow(() -> new NotFoundException("Image not found"));
        image.getData(); // load lazy column inside the transaction
        return image;
    }

    Recipe find(Long id) {
        return recipes.findById(id).orElseThrow(() -> new NotFoundException("Recipe " + id + " not found"));
    }

    private void apply(Recipe recipe, RecipeDto dto) {
        recipe.setTitle(dto.title().trim());
        recipe.setSubtitle(dto.subtitle());
        recipe.setDescription(dto.description());
        recipe.setSource(dto.source());
        recipe.setPrepMinutes(dto.prepMinutes());
        recipe.setTotalMinutes(dto.totalMinutes());
        recipe.setCalories(dto.calories());
        recipe.setTags(dto.tags());
        recipe.setEquipment(dto.equipment());
        recipe.setNotes(dto.notes());

        recipe.getIngredients().clear();
        List<IngredientDto> ingredients = dto.ingredients() == null ? List.of() : dto.ingredients();
        for (int i = 0; i < ingredients.size(); i++) {
            IngredientDto in = ingredients.get(i);
            RecipeIngredient ing = new RecipeIngredient();
            ing.setRecipe(recipe);
            ing.setPosition(i);
            ing.setName(in.name().trim());
            ing.setQuantity2(in.quantity2());
            ing.setQuantity4(in.quantity4());
            ing.setUnit(StringUtils.hasText(in.unit()) ? in.unit().trim() : null);
            ing.setAllergens(in.allergens());
            ing.setPantry(in.pantry());
            recipe.getIngredients().add(ing);
        }

        recipe.getSteps().clear();
        List<StepDto> steps = dto.steps() == null ? List.of() : dto.steps();
        for (int i = 0; i < steps.size(); i++) {
            StepDto s = steps.get(i);
            RecipeStep step = new RecipeStep();
            step.setRecipe(recipe);
            step.setPosition(i);
            step.setTitle(s.title());
            step.setInstructions(s.instructions());
            recipe.getSteps().add(step);
        }
    }

    private RecipeDto toDto(Recipe r) {
        List<IngredientDto> ingredients = r.getIngredients().stream()
                .map(i -> new IngredientDto(i.getId(), i.getName(), i.getQuantity2(), i.getQuantity4(), i.getUnit(),
                        i.getAllergens(), i.isPantry()))
                .toList();
        List<StepDto> steps = r.getSteps().stream()
                .map(s -> new StepDto(s.getId(), s.getTitle(), s.getInstructions()))
                .toList();
        List<Long> imageIds = r.getId() == null ? List.of() : images.findIdsByRecipeId(r.getId());
        return new RecipeDto(r.getId(), r.getTitle(), r.getSubtitle(), r.getDescription(), r.getSource(),
                r.getPrepMinutes(), r.getTotalMinutes(), r.getCalories(), r.getTags(), r.getEquipment(), r.getNotes(),
                ingredients, steps, imageIds);
    }
}
