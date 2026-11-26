package org.beerbower.flatinafresh.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsernameIgnoreCase(String username);

    List<AppUser> findByFamilyIdOrderByCreatedAt(Long familyId);

    long countByFamilyId(Long familyId);
}
