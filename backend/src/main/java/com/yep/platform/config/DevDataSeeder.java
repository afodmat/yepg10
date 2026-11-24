package com.yep.platform.config;

import com.yep.platform.user.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dev-only demo accounts. Password comes from YEP_DEV_SEED_PASSWORD; nothing is
 * hard-coded or logged.
 */
@Component
@org.springframework.context.annotation.Profile("dev")
@org.springframework.core.annotation.Order(1)
public class DevDataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final PasswordEncoder encoder;
    private final String password;

    public DevDataSeeder(UserRepository users, ProfileRepository profiles, PasswordEncoder encoder,
            @Value("${yep.dev-seed-password}") String password) {
        this.users = users;
        this.profiles = profiles;
        this.encoder = encoder;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (password.isBlank()) {
            log.warn("YEP_DEV_SEED_PASSWORD not set: skipping demo accounts");
            return;
        }
        seed("admin@yep.local", Role.ADMIN, "Grace", "Admin", "Uganda", 33);
        seed("facilitator@yep.local", Role.FACILITATOR, "Joseph", "Mukasa", "Uganda", 34);
        seed("mentor@yep.local", Role.MENTOR, "Sarah", "Nakato", "Uganda", 35);
        seed("amina@yep.local", Role.PARTICIPANT, "Amina", "Kato", "Uganda", 24);
        seed("peter@yep.local", Role.PARTICIPANT, "Peter", "Ssemakula", "Uganda", 26);
        seed("brian@yep.local", Role.PARTICIPANT, "Brian", "Okello", "Uganda", 27);
        seed("wanjiru@yep.local", Role.PARTICIPANT, "Wanjiru", "Kamau", "Kenya", 25);
    }

    private void seed(String email, Role role, String first, String last, String country, int age) {
        if (users.findByEmailIgnoreCase(email).isPresent())
            return;
        var u = users.save(new User(email, encoder.encode(password), role, UserStatus.ACTIVE));
        profiles.save(new Profile(u, first, last, null, country, (short) age));
        log.info("Seeded dev account {} ({})", email, role);
    }
}
