package com.digitallife.journal_site.common;

import com.digitallife.journal_site.profile.CompanionCatalog;
import com.digitallife.journal_site.profile.Profile;
import com.digitallife.journal_site.profile.ProfileRepository;
import com.digitallife.journal_site.user.User;
import com.digitallife.journal_site.user.UserDetailService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Provides the currently authenticated username to all templates so the
 * navbar can link to the correct profile page.
 */
@ControllerAdvice
public class CurrentUserAdvice {

    private final UserDetailService userDetailService;
    private final ProfileRepository profileRepository;

    public CurrentUserAdvice(UserDetailService userDetailService, ProfileRepository profileRepository) {
        this.userDetailService = userDetailService;
        this.profileRepository = profileRepository;
    }

    @ModelAttribute
    public void addCurrentUserAttributes(Model model, Principal principal, HttpServletRequest request) {
        String username = principal == null ? null : principal.getName();
        Profile profile = null;
        if (username != null && !username.isBlank()) {
            User user = userDetailService.findByUsername(username);
            if (user != null) {
                profile = profileRepository.findByUserId(user.getId()).orElse(null);
            }
        }

        String displayName = resolveDisplayName(username, profile);
        String companionKey = profile == null
                ? CompanionCatalog.find(null).key()
                : CompanionCatalog.find(profile.getCompanionKey()).key();
        String currentPath = request == null || request.getRequestURI() == null || request.getRequestURI().isBlank()
                ? "/"
                : request.getRequestURI();
        Map<String, String> languageLinks = buildLanguageLinks(request);

        model.addAttribute("currentUserContext", new CurrentUserContext(
                username,
                displayName,
                initialFor(displayName),
                companionKey,
                currentPath,
                LocaleContextHolder.getLocale().getLanguage(),
                languageLinks
        ));
        model.addAttribute("currentUsername", username);
        model.addAttribute("currentDisplayName", displayName);
        model.addAttribute("currentInitial", initialFor(displayName));
        model.addAttribute("currentCompanionKey", companionKey);
        model.addAttribute("currentPath", currentPath);
        model.addAttribute("currentLanguage", LocaleContextHolder.getLocale().getLanguage());
        model.addAttribute("languageLinks", languageLinks);
    }

    private String resolveDisplayName(String username, Profile profile) {
        if (profile != null && profile.getName() != null && !profile.getName().isBlank()) {
            return profile.getName();
        }
        return username;
    }

    private String initialFor(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "J";
        }
        return displayName.substring(0, 1).toUpperCase();
    }

    private Map<String, String> buildLanguageLinks(HttpServletRequest request) {
        Map<String, String> links = new LinkedHashMap<>();
        links.put("en", buildLanguageLink(request, Locale.ENGLISH));
        links.put("de", buildLanguageLink(request, Locale.GERMAN));
        links.put("nl", buildLanguageLink(request, Locale.forLanguageTag("nl")));
        return links;
    }

    private String buildLanguageLink(HttpServletRequest request, Locale locale) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(
                request != null && request.getRequestURI() != null && !request.getRequestURI().isBlank()
                        ? request.getRequestURI()
                        : "/"
        );

        if (request != null) {
            request.getParameterMap().forEach((key, values) -> {
                if ("lang".equalsIgnoreCase(key)) {
                    return;
                }
                if (values == null || values.length == 0) {
                    builder.queryParam(key);
                    return;
                }
                for (String value : values) {
                    builder.queryParam(key, value);
                }
            });
        }

        builder.replaceQueryParam("lang", locale.getLanguage());
        return builder.build().toUriString();
    }

    public record CurrentUserContext(
            String username,
            String displayName,
            String initial,
            String companionKey,
            String path,
            String language,
            Map<String, String> languageLinks
    ) {
    }
}
