package org.beerbower.flatinafresh.recipe;

import lombok.RequiredArgsConstructor;
import org.beerbower.flatinafresh.scan.ImageProcessor;
import org.beerbower.flatinafresh.scan.ScanProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Attach photos or card scans to an existing recipe, or remove them. */
@RestController
@RequestMapping("/api/recipes/{id}/images")
@RequiredArgsConstructor
public class RecipeImageController {
    private final RecipeService service;
    private final ImageProcessor imageProcessor;
    private final ScanProperties scanProperties;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<Long> upload(@PathVariable Long id, @RequestParam("images") List<MultipartFile> files)
            throws IOException {
        List<Long> ids = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            byte[] jpeg = imageProcessor.toJpeg(imageProcessor.read(file.getBytes()),
                    scanProperties.storedImagePx(), 0.82f);
            ids.add(service.appendImage(id, "image/jpeg", jpeg));
        }
        return ids;
    }

    @DeleteMapping("/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @PathVariable Long imageId) {
        service.deleteImage(id, imageId);
    }
}
