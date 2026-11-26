package org.beerbower.flatinafresh.recipe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RecipeImageRepository extends JpaRepository<RecipeImage, Long> {
    Optional<RecipeImage> findByIdAndRecipeId(Long id, Long recipeId);

    @Query("select i.id from RecipeImage i where i.recipeId = ?1 order by i.position")
    List<Long> findIdsByRecipeId(Long recipeId);

    /** [recipeId, imageId] for every image, in display order, without loading image data. */
    @Query("select i.recipeId, i.id from RecipeImage i order by i.recipeId, i.position")
    List<Object[]> findAllIdsInOrder();

    @Query("select coalesce(max(i.position), -1) from RecipeImage i where i.recipeId = ?1")
    int findMaxPosition(Long recipeId);
}
