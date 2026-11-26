package org.beerbower.flatinafresh.family;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FamilyInviteRepository extends JpaRepository<FamilyInvite, Long> {
    Optional<FamilyInvite> findByToken(String token);

    List<FamilyInvite> findByFamilyIdAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAt(Long familyId,
                                                                                        LocalDateTime now);
}
