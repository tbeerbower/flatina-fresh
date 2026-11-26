package org.beerbower.flatinafresh.scan;

import lombok.RequiredArgsConstructor;
import org.beerbower.flatinafresh.recipe.RecipeDtos.RecipeDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/recipes/scan")
@RequiredArgsConstructor
public class ScanController {
    private final ScanService scanService;

    /** Upload the front and back of a card; returns the new recipe for review. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeDto scan(@RequestParam("images") List<MultipartFile> images) {
        return scanService.scan(images);
    }
}
