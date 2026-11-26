package org.beerbower.flatinafresh.shopping;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.beerbower.flatinafresh.recipe.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** The household's saved products ("my products"), shared by everyone who shops from the plan. */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class GroceryProductController {
    private final GroceryProductRepository products;

    public record ProductDto(Long id, String ingredientKey, String ingredientName, String brand, String productName,
                             BigDecimal packageSize, String packageUnit, String packageLabel, String storeItem,
                             boolean staple) {
        static ProductDto of(GroceryProduct p) {
            return new ProductDto(p.getId(), p.getIngredientKey(), p.getIngredientName(), p.getBrand(),
                    p.getProductName(), p.getPackageSize(), p.getPackageUnit(), p.getPackageLabel(),
                    p.getStoreItem(), p.isStaple());
        }
    }

    public record ProductRequest(@NotBlank @Size(max = 200) String ingredientName, @Size(max = 100) String brand,
                                 @Size(max = 200) String productName,
                                 @DecimalMin(value = "0", inclusive = false) BigDecimal packageSize,
                                 @Size(max = 50) String packageUnit, @Size(max = 50) String packageLabel,
                                 @Size(max = 500) String storeItem, boolean staple) {
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<ProductDto> list() {
        return products.findAllByOrderByIngredientNameAsc().stream().map(ProductDto::of).toList();
    }

    /** Saves the product for an ingredient, replacing the one already saved for it. */
    @PostMapping
    @Transactional
    public ProductDto save(@Valid @RequestBody ProductRequest request) {
        String key = ShoppingListService.normalizeName(request.ingredientName());
        if (key.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ingredientName is required");
        }
        GroceryProduct p = products.findByIngredientKey(key).orElseGet(GroceryProduct::new);
        p.setIngredientKey(key);
        apply(p, request);
        return ProductDto.of(products.save(p));
    }

    @PutMapping("/{id}")
    @Transactional
    public ProductDto update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        GroceryProduct p = products.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
        String key = ShoppingListService.normalizeName(request.ingredientName());
        if (!key.equals(p.getIngredientKey())) {
            products.findByIngredientKey(key).ifPresent(other -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "There's already a product saved for " + other.getIngredientName());
            });
            p.setIngredientKey(key);
        }
        apply(p, request);
        return ProductDto.of(p);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        products.deleteById(id);
    }

    private static void apply(GroceryProduct p, ProductRequest r) {
        p.setIngredientName(r.ingredientName().trim());
        p.setBrand(blankToNull(r.brand()));
        p.setProductName(blankToNull(r.productName()));
        p.setPackageSize(r.packageSize());
        p.setPackageUnit(r.packageSize() == null ? null : Units.normalize(r.packageUnit()));
        p.setPackageLabel(blankToNull(r.packageLabel()));
        p.setStoreItem(blankToNull(r.storeItem()));
        p.setStaple(r.staple());
        p.setUpdatedAt(LocalDateTime.now());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
