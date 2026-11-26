package org.beerbower.flatinafresh.plan;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.beerbower.flatinafresh.recipe.NotFoundException;
import org.beerbower.flatinafresh.recipe.RecipeRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/plan")
@RequiredArgsConstructor
public class MealPlanController {
    private final MealPlanRepository entries;
    private final RecipeRepository recipes;

    public record EntryDto(Long id, LocalDate date, Long recipeId, String recipeTitle, String recipeSubtitle,
                           Integer totalMinutes, int servings) {
        static EntryDto of(MealPlanEntry e) {
            return new EntryDto(e.getId(), e.getPlanDate(), e.getRecipe().getId(), e.getRecipe().getTitle(),
                    e.getRecipe().getSubtitle(), e.getRecipe().getTotalMinutes(), e.getServings());
        }
    }

    public record EntryRequest(@NotNull LocalDate date, Long recipeId, @Min(1) @Max(12) Integer servings) {
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<EntryDto> list(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        checkRange(start, end);
        return entries.findInRange(start, end).stream().map(EntryDto::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public EntryDto add(@Valid @RequestBody EntryRequest request) {
        if (request.recipeId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recipeId is required");
        }
        MealPlanEntry entry = new MealPlanEntry();
        entry.setPlanDate(request.date());
        entry.setRecipe(recipes.findById(request.recipeId())
                .orElseThrow(() -> new NotFoundException("Recipe not found")));
        entry.setServings(request.servings() == null ? 2 : request.servings());
        return EntryDto.of(entries.save(entry));
    }

    @PutMapping("/{id}")
    @Transactional
    public EntryDto update(@PathVariable Long id, @Valid @RequestBody EntryRequest request) {
        MealPlanEntry entry = entries.findById(id).orElseThrow(() -> new NotFoundException("Plan entry not found"));
        entry.setPlanDate(request.date());
        if (request.servings() != null) {
            entry.setServings(request.servings());
        }
        if (request.recipeId() != null && !request.recipeId().equals(entry.getRecipe().getId())) {
            entry.setRecipe(recipes.findById(request.recipeId())
                    .orElseThrow(() -> new NotFoundException("Recipe not found")));
        }
        return EntryDto.of(entry);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        entries.deleteById(id);
    }

    static void checkRange(LocalDate start, LocalDate end) {
        if (end.isBefore(start) || ChronoUnit.DAYS.between(start, end) > 62) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a date range of up to 62 days");
        }
    }
}
