package com.digitallife.journal_site.common;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

/**
 * Provides the currently authenticated username to all templates so the
 * navbar can link to the correct profile page.
 */
@ControllerAdvice
public class CurrentUserAdvice {

    @ModelAttribute("currentUsername")
    public String currentUsername(Principal principal) {
        return principal != null ? principal.getName() : null;
    }
}