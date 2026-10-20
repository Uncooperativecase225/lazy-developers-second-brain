package com.lazybrain.controller;

import com.lazybrain.dto.SystemDtos;
import com.lazybrain.service.SystemStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {
    private final SystemStatusService status;
    public SystemController(SystemStatusService status) { this.status = status; }
    @GetMapping("/status") public SystemDtos.StatusResponse status() { return status.status(); }
}
