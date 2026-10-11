package com.example.testapp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageRequest(@NotBlank @Size(max = 200) String text) {
}
