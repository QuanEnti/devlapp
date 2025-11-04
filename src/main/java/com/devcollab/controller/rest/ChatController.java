package com.devcollab.controller.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Value("${GEMINI_API_KEY}")
    private String geminiApiKey;

    @PostMapping("/send")
    public Map<String, String> chat(@RequestBody Map<String, String> payload) {
        String userMsg = payload.get("message");

        if (userMsg == null || userMsg.isBlank()) {
            return Map.of("reply", "Bạn hãy nhập tin nhắn nhé!");
        }

        try {
            String prompt = """
                    Bạn là trợ lý AI trong ứng dụng quản lý dự án DevCollab.
                    - Trả lời ngắn gọn, thân thiện, dễ hiểu.
                    - Không dùng markdown hoặc ký tự **, không xuống dòng quá nhiều.
                    - Nếu người dùng hỏi về cách tạo project/task/mô tả → hãy hướng dẫn ngắn gọn.
                    - Nếu câu hỏi ngoài phạm vi → vẫn trả lời lịch sự.

                    Người dùng: %s
                    """.formatted(userMsg);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + geminiApiKey;

            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> content = Map.of("parts", List.of(textPart));
            Map<String, Object> body = Map.of("contents", List.of(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            String reply = "Mình chưa hiểu ý bạn.";
            if (response.getBody() != null && response.getBody().containsKey("candidates")) {
                List candidates = (List) response.getBody().get("candidates");
                if (!candidates.isEmpty()) {
                    Map first = (Map) candidates.get(0);
                    Map contentMap = (Map) first.get("content");
                    List parts = (List) contentMap.get("parts");
                    if (!parts.isEmpty()) {
                        Map textMap = (Map) parts.get(0);
                        reply = textMap.get("text").toString();
                    }
                }
            }

            return Map.of("reply", reply.trim());

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("reply", "⚠️ Lỗi khi kết nối AI: " + e.getMessage());
        }
    }
}
