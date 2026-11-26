package org.beerbower.flatinafresh.recipe;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Classifies the protein of recipes saved before the protein column existed. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProteinBackfill implements ApplicationRunner {
    private final RecipeRepository recipes;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int updated = 0;
        for (Recipe r : recipes.findByProteinIsNull()) {
            String protein = Proteins.classify(r.getTags(),
                    r.getIngredients().stream().map(RecipeIngredient::getName).toList());
            if (protein != null) {
                r.setProtein(protein);
                updated++;
            }
        }
        if (updated > 0) {
            log.info("Classified the protein of {} recipes", updated);
        }
    }
}
