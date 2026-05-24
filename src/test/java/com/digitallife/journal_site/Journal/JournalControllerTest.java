package com.digitallife.journal_site.Journal;

import com.digitallife.journal_site.ChatGptIntegration.ChatGPTController;
import com.digitallife.journal_site.common.SiteTextService;
import com.digitallife.journal_site.communities.CommunityService;
import com.digitallife.journal_site.profile.ProfileRepository;
import com.digitallife.journal_site.user.User;
import com.digitallife.journal_site.user.UserDetailService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JournalControllerTest {

    private final JournalService journalService = mock(JournalService.class);
    private final JournalInsightsService journalInsightsService = mock(JournalInsightsService.class);
    private final UserDetailService userService = mock(UserDetailService.class);
    private final CommunityService communityService = mock(CommunityService.class);
    private final ChatGPTController chatGptController = mock(ChatGPTController.class);
    private final ProfileRepository profileRepository = mock(ProfileRepository.class);
    private final SiteTextService siteTextService = mock(SiteTextService.class);
    private final JournalController controller = new JournalController(
            journalService,
            journalInsightsService,
            userService,
            communityService,
            chatGptController,
            profileRepository,
            siteTextService
    );
    private final Principal principal = () -> "maja";

    @Test
    void showUserJournalsLoadsAllEntriesNewestFirst() {
        User user = new User();
        user.setUsername("maja");
        when(userService.findByUsername("maja")).thenReturn(user);

        JournalEntryListItem item = new JournalEntryListItem(
                42L,
                "A winter note",
                "Snow and coffee.",
                LocalDateTime.of(2026, 2, 12, 10, 30),
                JournalEntry.Visibility.PRIVATE,
                "maja",
                null,
                new JournalEntryData()
        );
        List<JournalEntryListItem> entries = List.of(item);
        when(journalService.findAllEntryItems(user)).thenReturn(entries);

        Model model = new ExtendedModelMap();
        String view = controller.showUserJournals(null, null, null, model, principal, Locale.ENGLISH);

        assertEquals("user/UserJournalEntries", view);
        assertEquals(entries, model.asMap().get("entries"));
        assertEquals(1, model.asMap().get("entryCount"));
    }

    @Test
    void showUserJournalsKeepsOldMonthLinksOnAllEntriesView() {
        User user = new User();
        user.setUsername("maja");
        when(userService.findByUsername("maja")).thenReturn(user);
        when(journalService.findAllEntryItems(user)).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        String view = controller.showUserJournals(1, 2026, null, model, principal, Locale.ENGLISH);

        assertEquals("user/UserJournalEntries", view);
        assertEquals(0, model.asMap().get("entryCount"));
        assertTrue(((List<?>) model.asMap().get("entries")).isEmpty());
    }

    @Test
    void loadJournalEntryImagesReturnsDisplayImagesForOwner() {
        User user = new User();
        user.setId(1L);
        user.setUsername("maja");
        when(userService.findByUsername("maja")).thenReturn(user);

        JournalEntry entry = new JournalEntry();
        entry.setId(10L);
        entry.setUser(user);
        entry.setImageUrl("abc123");
        when(journalService.findJournalEntryById(10L)).thenReturn(entry);

        var response = controller.loadJournalEntryImages(10L, principal);

        assertEquals(200, response.getStatusCode().value());
        Map<String, List<String>> body = response.getBody();
        assertEquals(List.of("data:image/png;base64,abc123"), body.get("images"));
    }

    @Test
    void loadJournalEntryImagesRejectsEntriesOwnedBySomeoneElse() {
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("maja");
        when(userService.findByUsername("maja")).thenReturn(currentUser);

        User owner = new User();
        owner.setId(2L);
        owner.setUsername("lena");

        JournalEntry entry = new JournalEntry();
        entry.setId(10L);
        entry.setUser(owner);
        entry.setImageUrl("abc123");
        when(journalService.findJournalEntryById(10L)).thenReturn(entry);

        var response = controller.loadJournalEntryImages(10L, principal);

        assertEquals(403, response.getStatusCode().value());
    }

    @Test
    void saveViaJsonPassesSkippedMetadataCustomEmotionHandwritingImagesAndAudio() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("maja");
        when(userService.findByUsername("maja")).thenReturn(user);

        JournalEntryData entryData = new JournalEntryData();
        entryData.setMoodScore(4);
        entryData.setCustomEmotion("Curious");
        entryData.setVoiceMemoTranscript("Spoken and edited reflection");

        JournalEntryDTO dto = new JournalEntryDTO();
        dto.setContent("Spoken and edited reflection");
        dto.setImageUrl("data:image/png;base64,handwriting");
        dto.setImageUrls(List.of("data:image/png;base64,handwriting"));
        dto.setVoiceMemoAudioDataUrl("data:audio/webm;base64,abc123");
        dto.setVisibility(JournalEntry.Visibility.PRIVATE);
        dto.setEntryData(entryData);

        JournalEntry saved = new JournalEntry();
        saved.setId(99L);
        when(journalService.saveJournalEntry(
                eq(user),
                eq(null),
                eq("Spoken and edited reflection"),
                eq("data:image/png;base64,handwriting"),
                eq(List.of("data:image/png;base64,handwriting")),
                eq("data:audio/webm;base64,abc123"),
                any(JournalEntryData.class),
                isNull(),
                eq(JournalEntry.Visibility.PRIVATE)
        )).thenReturn(saved);

        var auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("maja");

        var response = controller.saveViaJson(dto, auth, Locale.ENGLISH);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("\"entryId\":99"));
        verify(journalService).saveJournalEntry(
                eq(user),
                eq(null),
                eq("Spoken and edited reflection"),
                eq("data:image/png;base64,handwriting"),
                eq(List.of("data:image/png;base64,handwriting")),
                eq("data:audio/webm;base64,abc123"),
                any(JournalEntryData.class),
                isNull(),
                eq(JournalEntry.Visibility.PRIVATE)
        );
    }

    @Test
    void editJournalEntry() {
    }

    @Test
    void updateJournalEntry() {
    }

    @Test
    void getAllJournalEntries() {
    }

    @Test
    void showImageTimeline() {
    }
}
