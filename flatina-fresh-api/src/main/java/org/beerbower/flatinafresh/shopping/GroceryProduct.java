package org.beerbower.flatinafresh.shopping;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** What you buy for an ingredient: brand, package size, store item, and whether you always have it. */
@Entity
@Table(name = "grocery_product")
@Getter
@Setter
public class GroceryProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** {@link ShoppingListService#normalizeName} of the ingredient name. */
    private String ingredientKey;
    private String ingredientName;
    private String brand;
    private String productName;
    /** How much one package holds, in {@link #packageUnit} (e.g. 10 fl oz, 1 lb, 10 clove). */
    private BigDecimal packageSize;
    private String packageUnit;
    /** What one package is called: bottle, can, bag, head... */
    private String packageLabel;
    /** Store item number, UPC or product link. */
    private String storeItem;
    /** Always kept at home, so left off the order unless you say otherwise. */
    private boolean staple;
    private LocalDateTime updatedAt = LocalDateTime.now();
}
