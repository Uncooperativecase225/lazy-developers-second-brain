package com.lazybrain.dto;

public final class SystemDtos {
    private SystemDtos() {}
    public record StatusResponse(String application, String database, String ollama, String model, String mode) {}
}
