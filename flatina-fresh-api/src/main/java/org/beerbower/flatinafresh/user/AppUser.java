package org.beerbower.flatinafresh.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_user")
@Getter
@Setter
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String passwordHash;
    private String displayName;
    private boolean admin;
    /** Every user belongs to one family, whose meal plan they share. */
    private Long familyId;
    private LocalDateTime createdAt = LocalDateTime.now();
}
