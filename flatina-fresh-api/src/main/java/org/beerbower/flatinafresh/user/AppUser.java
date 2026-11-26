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

    /** The name the app calls this person. Not unique, and not used to sign in. */
    private String username;
    /** Lowercased; unique. Null only for accounts made before emails, until they add one. */
    private String email;
    private String passwordHash;
    private String displayName;
    private boolean admin;
    /** Every user belongs to one family, whose meal plan they share. */
    private Long familyId;
    private LocalDateTime createdAt = LocalDateTime.now();

    /** How the app addresses this person. */
    public String name() {
        return displayName == null || displayName.isBlank() ? username : displayName;
    }

    /** Sets the name shown in the app (kept in both columns). */
    public void rename(String name) {
        username = name;
        displayName = name;
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
