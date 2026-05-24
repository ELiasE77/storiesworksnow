package com.digitallife.journal_site.user;

import com.digitallife.journal_site.common.SiteTextService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Locale;

@Controller
public class RegisterController {

    private final UserDetailService userDetailService;
    private final PasswordEncoder passwordEncoder;
    private final SiteTextService siteTextService;

    public RegisterController(UserDetailService uds, PasswordEncoder pe, SiteTextService siteTextService) {
        this.userDetailService = uds;
        this.passwordEncoder = pe;
        this.siteTextService = siteTextService;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("userDto", new UserDto());
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(
            @ModelAttribute("userDto") UserDto dto,
            HttpSession session,
            Model model,
            Locale locale
    ) {
        if (dto.getUsername() == null || dto.getUsername().isBlank()) {
            model.addAttribute("registrationError", siteTextService.message("register.error.usernameRequired", locale));
            return "register";
        }

        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            model.addAttribute("registrationError", siteTextService.message("register.error.passwordRequired", locale));
            return "register";
        }

        if (userDetailService.findByUsername(dto.getUsername()) != null) {
            model.addAttribute("registrationError", siteTextService.message("register.error.usernameTaken", locale));
            return "register";
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole("USER");
        userDetailService.saveUser(user);

        session.setAttribute("currentUserId", user.getId());

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetailService.loadUserByUsername(user.getUsername()),
                null,
                userDetailService.loadUserByUsername(user.getUsername()).getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        return "redirect:/profile/questionnaire";
    }
}
