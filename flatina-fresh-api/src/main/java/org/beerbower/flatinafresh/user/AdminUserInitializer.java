package org.beerbower.flatinafresh.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Creates the first admin account from ADMIN_USERNAME / ADMIN_PASSWORD if it doesn't exist yet, and makes sure
 * that account stays an admin. After that, the password is changed in the app, not here.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements ApplicationRunner {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String username;

    @Value("${app.admin.password}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            log.info("ADMIN_PASSWORD not set; skipping admin account setup");
            return;
        }
        AppUser user = users.findByUsernameIgnoreCase(username).orElseGet(() -> {
            AppUser u = new AppUser();
            u.setUsername(username);
            u.setDisplayName(username);
            log.info("Creating account {}", username);
            return u;
        });
        boolean changed = !user.isAdmin();
        user.setAdmin(true);
        if (user.getPasswordHash() == null) {
            user.setPasswordHash(passwordEncoder.encode(password));
            changed = true;
        }
        if (changed) {
            users.save(user);
        }
    }
}
