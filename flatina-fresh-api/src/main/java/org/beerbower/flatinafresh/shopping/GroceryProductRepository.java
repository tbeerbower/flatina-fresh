package org.beerbower.flatinafresh.shopping;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroceryProductRepository extends JpaRepository<GroceryProduct, Long> {
    Optional<GroceryProduct> findByFamilyIdAndIngredientKey(Long familyId, String ingredientKey);

    Optional<GroceryProduct> findByIdAndFamilyId(Long id, Long familyId);

    List<GroceryProduct> findByFamilyIdOrderByIngredientNameAsc(Long familyId);

    List<GroceryProduct> findByFamilyId(Long familyId);
}
