package org.beerbower.flatinafresh.family;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** A single-use link that creates an account in a family. */
@Entity
@Table(name = "family_invite")
@Getter
@Setter
public class FamilyInvite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long familyId;
    private String token;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private Long usedBy;

    public boolean isOpen(LocalDateTime now) {
        return usedAt == null && expiresAt.isAfter(now);
    }
}
