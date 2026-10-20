package com.lazybrain.service;

import com.lazybrain.dto.SystemDtos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import javax.sql.DataSource;
import java.sql.Connection;

@Service
public class SystemStatusService {
    private final DataSource dataSource;
    private final RestClient ollama;
    private final String model;
    public SystemStatusService(DataSource dataSource, @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String ollamaUrl,
                               @Value("${spring.ai.ollama.chat.options.model:qwen3.5:9b}") String model) {
        this.dataSource = dataSource; this.ollama = RestClient.builder().baseUrl(ollamaUrl).build(); this.model = model;
    }
    public SystemDtos.StatusResponse status() {
        String db = "offline";
        try (Connection c = dataSource.getConnection()) { if (c.isValid(2)) db = "connected"; } catch (Exception ignored) {}
        String ai = "offline";
        try { ollama.get().uri("/api/tags").retrieve().toBodilessEntity(); ai = "connected"; } catch (Exception ignored) {}
        return new SystemDtos.StatusResponse("ok", db, ai, model, "local-first");
    }
}
