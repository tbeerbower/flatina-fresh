package org.beerbower.flatinafresh.shopping;

import lombok.RequiredArgsConstructor;
import org.beerbower.flatinafresh.family.FamilyService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/shopping-list")
@RequiredArgsConstructor
public class ShoppingListController {
    private final ShoppingListService service;
    private final FamilyService familyService;

    @GetMapping
    public ShoppingListService.ShoppingListDto get(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Authentication authentication) {
        if (end.isBefore(start) || ChronoUnit.DAYS.between(start, end) > 62) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a date range of up to 62 days");
        }
        return service.build(familyService.currentFamilyId(authentication), start, end);
    }
}
