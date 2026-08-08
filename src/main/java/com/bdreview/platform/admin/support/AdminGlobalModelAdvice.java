package com.bdreview.platform.admin.support;

import com.bdreview.platform.auth.User;
import com.bdreview.platform.auth.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Makes the logged-in admin's own profile available to every admin-panel template as ${currentAdmin}. */
@ControllerAdvice(basePackages = "com.bdreview.platform.admin.controller")
public class AdminGlobalModelAdvice {

    private final UserRepository userRepository;

    public AdminGlobalModelAdvice(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute("currentAdmin")
    public User currentAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        try {
            return userRepository.findById(AdminSupport.currentAdminId(authentication)).orElse(null);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
