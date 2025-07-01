package com.digitallife.journal_site.user;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class RegisterController {

    private final UserDetailService userDetailService;
    private final PasswordEncoder passwordEncoder;

    public RegisterController(UserDetailService uds,
                              PasswordEncoder pe) {
        this.userDetailService = uds;
        this.passwordEncoder   = pe;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("userDto", new UserDto());
        return "register";   // renders register.html
    }

    @PostMapping("/register")
    public String processRegistration(
            @ModelAttribute("userDto") UserDto dto,
            HttpSession session,
            Model model
    ) {
        // (validation & uniqueness checks omitted for brevity…)

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole("USER");
        userDetailService.saveUser(user);

        session.setAttribute("currentUserId", user.getId());

        // auto-login
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetailService.loadUserByUsername(user.getUsername()),
                null,
                userDetailService.loadUserByUsername(user.getUsername()).getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        // ─── **redirect** here instead of forward ───
        return "redirect:/profile/questionnaire";
    }
}
