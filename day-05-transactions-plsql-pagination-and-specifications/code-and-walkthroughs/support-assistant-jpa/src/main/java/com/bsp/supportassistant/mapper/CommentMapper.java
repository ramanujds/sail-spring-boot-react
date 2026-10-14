package com.bsp.supportassistant.mapper;

import com.bsp.supportassistant.dto.CommentRequest;
import com.bsp.supportassistant.dto.CommentResponse;
import com.bsp.supportassistant.model.Comment;
import com.bsp.supportassistant.model.Ticket;

import java.time.LocalDateTime;
import java.util.List;

public class CommentMapper {

    public static Comment toEntity(CommentRequest request, Ticket ticket) {
        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setAuthor(request.author());
        comment.setText(request.text());
        comment.setCreatedAt(LocalDateTime.now());
        return comment;
    }

    public static CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getAuthor(),
                comment.getText(),
                comment.getCreatedAt());
    }

    public static List<CommentResponse> toResponseList(List<Comment> comments) {
        return comments.stream()
                .map(CommentMapper::toResponse)
                .toList();
    }
}
