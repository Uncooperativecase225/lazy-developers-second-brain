package com.lazybrain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AskDtos {
    private AskDtos() {}
    public record Request(@NotBlank @Size(max = 2000) String question) {}
    public record Response(String answer, boolean aiGenerated) {}
}
