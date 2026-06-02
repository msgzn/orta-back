package org.example.ortaback.controller;

import org.example.ortaback.dto.request.MarkReadRequestDto;
import org.example.ortaback.dto.request.SendMessageRequestDto;
import org.example.ortaback.dto.response.ConversationSummaryDto;
import org.example.ortaback.dto.response.EventMessageDto;
import org.example.ortaback.dto.response.PageDto;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.enums.MessageType;
import org.example.ortaback.persistence.models.enums.UserRole;
import org.example.ortaback.service.ChatService;
import org.example.ortaback.testsupport.MockMvcTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatControllerTest extends MockMvcTestSupport {

    private final ChatService chatService = mock(ChatService.class);
    private MockMvc mvc;

    private final User currentUser = User.builder()
            .id(1L).email("u@example.com")
            .firstName("U").lastName("U")
            .hashedPassword("BCRYPT").role(UserRole.USER)
            .build();

    @BeforeEach
    void setUp() {
        mvc = buildMockMvc(new ChatController(chatService));
        authenticate(currentUser);
    }

    private EventMessageDto msg(long id, String text) {
        return new EventMessageDto(id, 100L, 1L, "U U", null, MessageType.USER, text,
                LocalDateTime.of(2026, 6, 1, 18, 0));
    }

    @Test
    void listMessages_returnsPage() throws Exception {
        when(chatService.listMessages(eq(100L), any(), any(), eq(30), any()))
                .thenReturn(new PageDto<>(List.of(msg(2, "hi")), 0, 30, 1, 1, false, false));

        mvc.perform(get("/api/v1/events/100/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].text").value("hi"))
                .andExpect(jsonPath("$.content[0].type").value("USER"));
    }

    @Test
    void listMessages_nonMember_returns403() throws Exception {
        when(chatService.listMessages(eq(100L), any(), any(), eq(30), any()))
                .thenThrow(new AccessDeniedException("not a participant"));

        mvc.perform(get("/api/v1/events/100/messages"))
                .andExpect(status().isForbidden());
    }

    @Test
    void send_validBody_returns201() throws Exception {
        when(chatService.sendMessage(eq(100L), eq("hello"), any())).thenReturn(msg(5, "hello"));

        mvc.perform(post("/api/v1/events/100/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequestDto("hello"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.text").value("hello"));
    }

    @Test
    void send_blankText_returns400() throws Exception {
        mvc.perform(post("/api/v1/events/100/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequestDto("   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.text").exists());
    }

    @Test
    void markRead_returns204() throws Exception {
        mvc.perform(post("/api/v1/events/100/messages/read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MarkReadRequestDto(42L))))
                .andExpect(status().isNoContent());

        verify(chatService).markRead(eq(100L), eq(42L), any());
    }

    @Test
    void markRead_missingId_returns400() throws Exception {
        mvc.perform(post("/api/v1/events/100/messages/read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.lastMessageId").exists());
    }

    @Test
    void myConversations_returnsArray() throws Exception {
        when(chatService.listMyConversations(any())).thenReturn(List.of(
                new ConversationSummaryDto(100L, "Match", null, "hi",
                        LocalDateTime.of(2026, 6, 1, 18, 0), 3, 5)));

        mvc.perform(get("/api/v1/me/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value(100))
                .andExpect(jsonPath("$[0].unreadCount").value(3))
                .andExpect(jsonPath("$[0].participantsCount").value(5));
    }
}
