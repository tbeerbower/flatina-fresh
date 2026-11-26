package org.beerbower.flatinafresh.plan;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.beerbower.flatinafresh.recipe.NotFoundException;
import org.beerbower.flatinafresh.recipe.Recipe;
import org.beerbower.flatinafresh.recipe.RecipeRepository;
import org.beerbower.flatinafresh.recipe.RecipeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/plan")
@RequiredArgsConstructor
public class MealPlanController {
    private final MealPlanRepository entries;
    private final RecipeRepository recipes;
    private final RecipeService recipeService;

    public record EntryDto(Long id, LocalDate date, Long recipeId, String recipeTitle, String recipeSubtitle,
                           Integer totalMinutes, Integer calories, String protein, Long coverImageId,
                           int servings) {
        static EntryDto of(MealPlanEntry e, Map<Long, Long> covers) {
            Recipe r = e.getRecipe();
            return new EntryDto(e.getId(), e.getPlanDate(), r.getId(), r.getTitle(), r.getSubtitle(),
                    r.getTotalMinutes(), r.getCalories(), r.getProtein(), covers.get(r.getId()), e.getServings());
        }
    }

    /** The meals planned in the week starting on {@code start} (a Monday). */
    public record WeekDto(LocalDate start, List<EntryDto> entries) {
    }

    public record EntryRequest(@NotNull LocalDate date, Long recipeId, @Min(1) @Max(12) Integer servings) {
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<EntryDto> list(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        checkRange(start, end);
        Map<Long, Long> covers = recipeService.coverImageIds();
        return entries.findInRange(start, end).stream().map(e -> EntryDto.of(e, covers)).toList();
    }

    /**
     * {@code count} consecutive weeks starting with the week that contains {@code start}, each with its meals,
     * including weeks with none. Feeds the weeks carousel.
     */
    @GetMapping("/weeks")
    @Transactional(readOnly = true)
    public List<WeekDto> weeks(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                               @RequestParam(defaultValue = "9") int count) {
        if (count < 1 || count > 60) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose between 1 and 60 weeks");
        }
        LocalDate first = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate last = first.plusWeeks(count).minusDays(1);
        Map<Long, Long> covers = recipeService.coverImageIds();
        Map<LocalDate, List<EntryDto>> byWeek = new TreeMap<>();
        for (int i = 0; i < count; i++) {
            byWeek.put(first.plusWeeks(i), new ArrayList<>());
        }
        for (MealPlanEntry e : entries.findInRange(first, last)) {
            LocalDate monday = e.getPlanDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            byWeek.get(monday).add(EntryDto.of(e, covers));
        }
        return byWeek.entrySet().stream().map(w -> new WeekDto(w.getKey(), w.getValue())).toList();
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
        return EntryDto.of(entries.save(entry), recipeService.coverImageIds());
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
        return EntryDto.of(entry, recipeService.coverImageIds());
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
