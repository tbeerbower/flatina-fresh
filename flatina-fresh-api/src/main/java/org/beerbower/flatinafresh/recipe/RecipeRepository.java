package org.beerbower.flatinafresh.recipe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    @Query("""
            select r from Recipe r
            where lower(r.title) like lower(concat('%', :q, '%'))
               or lower(r.subtitle) like lower(concat('%', :q, '%'))
               or lower(r.tags) like lower(concat('%', :q, '%'))
            order by lower(r.title)
            """)
    List<Recipe> search(@Param("q") String q);

    @Query("select r from Recipe r order by lower(r.title)")
    List<Recipe> findAllOrderByTitle();

    boolean existsByTitleIgnoreCase(String title);

    List<Recipe> findByProteinIsNull();

    /** [recipeId, name] of every non-pantry ingredient, in card order. */
    @Query("select i.recipe.id, i.name from RecipeIngredient i where i.pantry = false order by i.recipe.id, i.position")
    List<Object[]> findIngredientNames();

    /** [recipeId, times planned, last planned date] for every recipe on the meal plan. */
    @Query("select e.recipe.id, count(e), max(e.planDate) from MealPlanEntry e group by e.recipe.id")
    List<Object[]> findPlanStats();
}
