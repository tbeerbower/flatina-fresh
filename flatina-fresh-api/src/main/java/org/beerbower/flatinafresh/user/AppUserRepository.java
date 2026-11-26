package org.beerbower.flatinafresh.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    /** Emails are stored lowercased: pass one through {@link AppUser#normalizeEmail}. */
    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Accounts made before emails, which still sign in with their (then unique) username until they add one. */
    List<AppUser> findByUsernameIgnoreCaseAndEmailIsNull(String username);

    List<AppUser> findByFamilyIdOrderByCreatedAt(Long familyId);

    long countByFamilyId(Long familyId);
}
