package org.beerbower.flatinafresh.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.beerbower.flatinafresh.family.FamilyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * Sets up the first admin from deploy settings, and keeps that account an admin:
 * <ul>
 * <li>The account is the one with ADMIN_EMAIL, or else the pre-email account named ADMIN_USERNAME.</li>
 * <li>If that pre-email account is found and ADMIN_EMAIL is set, the email is added to it (and the name changed to
 * ADMIN_NAME, if set), so it then signs in with the email.</li>
 * <li>If there is no such account, it is created from ADMIN_EMAIL, ADMIN_NAME (or ADMIN_USERNAME) and
 * ADMIN_PASSWORD.</li>
 * </ul>
 * After that, the name, email and password are changed in the app, not here.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements ApplicationRunner {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final FamilyService familyService;

    @Value("${app.admin.username:}")
    private String username;

    @Value("${app.admin.email:}")
    private String email;

    @Value("${app.admin.name:}")
    private String name;

    @Value("${app.admin.password:}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String adminEmail = StringUtils.hasText(email) ? AppUser.normalizeEmail(email) : null;
        Optional<AppUser> existing = Optional.ofNullable(adminEmail).flatMap(users::findByEmail)
                .or(this::legacyAccount);
        AppUser user;
        if (existing.isPresent()) {
            user = existing.get();
            if (user.getEmail() == null && adminEmail != null) {
                user.setEmail(adminEmail);
                if (StringUtils.hasText(name)) {
                    user.rename(name.trim());
                }
                log.info("Added ADMIN_EMAIL to the admin account; it now signs in with that email");
            }
        } else {
            if (adminEmail == null || !StringUtils.hasText(password)) {
                log.info("ADMIN_EMAIL or ADMIN_PASSWORD not set; skipping admin account setup");
                return;
            }
            user = new AppUser();
            user.setEmail(adminEmail);
            user.rename(StringUtils.hasText(name) ? name.trim()
                    : StringUtils.hasText(username) ? username.trim() : "Admin");
            user.setPasswordHash(passwordEncoder.encode(password));
            log.info("Creating the admin account for ADMIN_EMAIL");
        }
        user.setAdmin(true);
        if (user.getFamilyId() == null) {
            familyService.startFamily(user);
        } else {
            users.save(user);
        }
    }

    private Optional<AppUser> legacyAccount() {
        if (!StringUtils.hasText(username)) {
            return Optional.empty();
        }
        List<AppUser> found = users.findByUsernameIgnoreCaseAndEmailIsNull(username.trim());
        return found.size() == 1 ? Optional.of(found.get(0)) : Optional.empty();
    }
}
