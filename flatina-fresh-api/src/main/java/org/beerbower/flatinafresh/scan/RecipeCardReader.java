package org.beerbower.flatinafresh.scan;

import java.util.List;

/** Reads recipe card images (JPEG bytes, front and back) into structured recipe data. */
public interface RecipeCardReader {
    ScannedRecipe read(List<byte[]> jpegImages);
}
