package org.beerbower.flatinafresh.plan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MealPlanRepository extends JpaRepository<MealPlanEntry, Long> {
    @Query("""
            select e from MealPlanEntry e join fetch e.recipe
            where e.familyId = ?1 and e.planDate between ?2 and ?3
            order by e.planDate, e.id
            """)
    List<MealPlanEntry> findInRange(Long familyId, LocalDate start, LocalDate end);

    Optional<MealPlanEntry> findByIdAndFamilyId(Long id, Long familyId);
}
