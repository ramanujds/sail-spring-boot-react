package com.example.testapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MessageController.class)
class MessageControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    MessageRepository repository;

    @Test
    void listReturnsMessages() throws Exception {
        Message message = withId(new Message("first"), 1L);
        when(repository.findAll(any(Sort.class))).thenReturn(List.of(message));

        mvc.perform(get("/api/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].text").value("first"));
    }

    @Test
    void postCreatesMessage() throws Exception {
        when(repository.save(any(Message.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 7L));

        mvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"hello\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.text").value("hello"));
    }

    @Test
    void postRejectsBlankText() throws Exception {
        mvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    private static Message withId(Message message, Long id) {
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }
}
