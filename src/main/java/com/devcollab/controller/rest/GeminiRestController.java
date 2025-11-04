package com.devcollab.controller.rest;

import com.devcollab.service.system.GeminiService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class GeminiRestController {

    private final GeminiService geminiService;

    public GeminiRestController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        String reply = geminiService.getGeminiResponse(message);
        return Map.of("reply", reply);
    }
}
