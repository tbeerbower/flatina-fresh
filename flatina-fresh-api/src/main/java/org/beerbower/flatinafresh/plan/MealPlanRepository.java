package org.beerbower.flatinafresh.plan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface MealPlanRepository extends JpaRepository<MealPlanEntry, Long> {
    @Query("""
            select e from MealPlanEntry e join fetch e.recipe
            where e.planDate between ?1 and ?2
            order by e.planDate, e.id
            """)
    List<MealPlanEntry> findInRange(LocalDate start, LocalDate end);
}
