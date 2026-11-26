package org.beerbower.flatinafresh.family;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** A household. Its members share one meal plan. */
@Entity
@Table(name = "family")
@Getter
@Setter
public class Family {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Long createdBy;
    private LocalDateTime createdAt = LocalDateTime.now();
}
