package com.yep.platform.user;

import com.yep.platform.security.AuthUser;
import com.yep.platform.user.UserDtos.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Every handler derives the user from the verified token, never from an id sent by the client. */
@RestController
@RequestMapping("/api/users/me")
public class UserController {
    private final UserService service;
    public UserController(UserService service) { this.service = service; }

    @GetMapping public UserResponse me(@AuthenticationPrincipal AuthUser me) { return service.me(me.id()); }
    @GetMapping("/profile") public ProfileResponse profile(@AuthenticationPrincipal AuthUser me) { return service.profile(me.id()); }
    @PutMapping public ProfileResponse update(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody UpdateProfileRequest r) { return service.updateProfile(me.id(), r); }

    @PutMapping("/password") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ChangePasswordRequest r, HttpServletRequest req) {
        service.changePassword(me.id(), r, req.getRemoteAddr());
    }
}
