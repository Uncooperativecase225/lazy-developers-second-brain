package com.lazybrain.controller;

import com.lazybrain.dto.AskDtos;
import com.lazybrain.service.ProjectQueryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/ask")
public class AskController {
    private final ProjectQueryService query;
    public AskController(ProjectQueryService query) { this.query = query; }
    @PostMapping public AskDtos.Response ask(@PathVariable Long projectId, @Valid @RequestBody AskDtos.Request request) { return query.ask(projectId, request.question()); }
}
