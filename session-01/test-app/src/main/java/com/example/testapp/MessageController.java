package com.example.testapp;

import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageRepository repository;

    public MessageController(MessageRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<MessageResponse> list() {
        return repository.findAll(Sort.by("id")).stream().map(MessageResponse::from).toList();
    }

    @GetMapping("/{id}")
    public MessageResponse get(@PathVariable Long id) {
        return repository.findById(id)
                .map(MessageResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse create(@Valid @RequestBody MessageRequest request) {
        return MessageResponse.from(repository.save(new Message(request.text())));
    }
}
