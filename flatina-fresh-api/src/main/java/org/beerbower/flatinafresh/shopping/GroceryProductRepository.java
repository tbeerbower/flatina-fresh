package org.beerbower.flatinafresh.shopping;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroceryProductRepository extends JpaRepository<GroceryProduct, Long> {
    Optional<GroceryProduct> findByIngredientKey(String ingredientKey);

    List<GroceryProduct> findAllByOrderByIngredientNameAsc();
}
