package com.yep.platform.user;

import java.util.*;
import org.springframework.stereotype.Component;

/** Resolves user ids to display data in one query, so list endpoints avoid N+1 lookups. */
@Component
public class UserDirectory {
    private final ProfileRepository profiles;
    private final UserRepository users;
    public UserDirectory(ProfileRepository profiles, UserRepository users) { this.profiles = profiles; this.users = users; }

    public Map<Long, String> names(Collection<Long> ids) {
        var clean = ids.stream().filter(Objects::nonNull).distinct().toList();
        Map<Long, String> out = new HashMap<>();
        profiles.findAllById(clean).forEach(p -> out.put(p.getUserId(), p.getFirstName() + " " + p.getLastName()));
        return out;
    }
    public Map<Long, String> countries(Collection<Long> ids) {
        var clean = ids.stream().filter(Objects::nonNull).distinct().toList();
        Map<Long, String> out = new HashMap<>();
        profiles.findAllById(clean).forEach(p -> out.put(p.getUserId(), p.getCountry()));
        return out;
    }
    public String name(Long id) { return id == null ? null : names(List.of(id)).get(id); }
    public Optional<User> user(Long id) { return users.findById(id); }
    public Optional<User> byEmail(String email) { return users.findByEmailIgnoreCase(email.trim()); }
}
