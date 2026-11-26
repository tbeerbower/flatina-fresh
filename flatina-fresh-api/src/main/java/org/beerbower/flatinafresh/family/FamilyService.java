package org.beerbower.flatinafresh.family;

import lombok.RequiredArgsConstructor;
import org.beerbower.flatinafresh.user.AppUser;
import org.beerbower.flatinafresh.user.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Service
@RequiredArgsConstructor
public class FamilyService {
    private final AppUserRepository users;
    private final FamilyRepository families;
    private final Clock clock;

    /** The signed-in user. */
    public AppUser currentUser(Authentication authentication) {
        return users.findById(userId(authentication))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    /** The signed-in user's id (the authentication's name, set by the JWT filter). */
    public static Long userId(Authentication authentication) {
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    /** The signed-in user's family, whose meal plan they see and edit. */
    public Long currentFamilyId(Authentication authentication) {
        return currentUser(authentication).getFamilyId();
    }

    /** Starts a new family with the user as its only member, and saves the user (new or existing). */
    @Transactional
    public AppUser startFamily(AppUser user) {
        Family family = new Family();
        family.setName(defaultName(user));
        family = families.save(family);
        user.setFamilyId(family.getId());
        user = users.save(user);
        family.setCreatedBy(user.getId());
        return user;
    }

    /** Renames the user; a family still called by their old default name ("Old's family") follows the new name. */
    @Transactional
    public void rename(AppUser user, String name) {
        String oldDefault = defaultName(user);
        user.rename(name);
        if (user.getFamilyId() != null) {
            families.findById(user.getFamilyId())
                    .filter(f -> f.getName().equals(oldDefault))
                    .ifPresent(f -> f.setName(defaultName(user)));
        }
    }

    /** Takes the user out of their family into a new one of their own; an emptied family is deleted. */
    @Transactional
    public void moveToOwnFamily(AppUser user) {
        Long old = user.getFamilyId();
        startFamily(user);
        users.flush();
        deleteIfEmpty(old);
    }

    /** Deletes a family with no members left, with its plan and invites. */
    @Transactional
    public void deleteIfEmpty(Long familyId) {
        if (familyId != null && users.countByFamilyId(familyId) == 0) {
            families.deleteById(familyId);
        }
    }

    public Clock clock() {
        return clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /**
     * Only this week and later weeks can be changed. Weeks start on Monday; the server runs on UTC, so a week stays
     * open one extra day to cover Sunday evening in American time zones.
     */
    public boolean isEditable(LocalDate date) {
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return !monday.plusDays(7).isBefore(today());
    }

    public static String defaultName(AppUser user) {
        return user.name().trim() + "'s family";
    }
}
