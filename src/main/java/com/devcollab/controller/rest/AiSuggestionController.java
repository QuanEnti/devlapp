package com.devcollab.controller.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

@RestController
@RequestMapping("/api/ai")
public class AiSuggestionController {

    @Value("${GEMINI_API_KEY}")
    private String geminiApiKey;

    @PostMapping("/suggest-description")
    public Map<String, String> suggestDescription(@RequestBody Map<String, String> payload) {
        String input = payload.get("input");
        String projectName = payload.getOrDefault("projectName", "Project"); // ✅ nhận tên project từ frontend

        if (input == null || input.isBlank()) {
            return Map.of("suggestion", "Hãy nhập một vài từ mô tả dự án để AI có thể gợi ý.");
        }

        try {
            // ✨ Chuẩn bị request tới Gemini API
            String modelName = "gemini-2.0-flash"; // model mới
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName
                    + ":generateContent?key=" + geminiApiKey;

            RestTemplate restTemplate = new RestTemplate();

            Map<String, Object> textPart = Map.of(
                    "text",
                    "Hãy viết một mô tả ngắn gọn, hấp dẫn và thu hút cho dự án có nội dung: "
                            + input
                            + ". Nếu có cụm [Tên dự án] thì thay thế bằng tên thực tế: " + projectName
                            + ". Không cần dùng markdown hoặc dấu **.");
            Map<String, Object> content = Map.of("parts", List.of(textPart));
            Map<String, Object> body = Map.of("contents", List.of(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            // 🔥 Gọi API Gemini
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            // 🧠 Đọc kết quả từ response JSON
            Map<String, Object> result = response.getBody();
            String suggestion = "Không thể sinh gợi ý.";

            if (result != null && result.containsKey("candidates")) {
                List candidates = (List) result.get("candidates");
                if (!candidates.isEmpty()) {
                    Map firstCandidate = (Map) candidates.get(0);
                    Map contentMap = (Map) firstCandidate.get("content");
                    List parts = (List) contentMap.get("parts");
                    if (!parts.isEmpty()) {
                        Map textPartMap = (Map) parts.get(0);
                        suggestion = textPartMap.get("text").toString();
                    }
                }
            }

            // ✨ Làm sạch văn bản
            suggestion = suggestion
                    .replace("[Tên dự án]", projectName) // thay thế bằng tên thật
                    .replaceAll("\\*\\*", "") // xóa dấu **
                    .replaceAll("\\*", "") // xóa dấu *
                    .trim();

            return Map.of("suggestion", suggestion);

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("suggestion", "⚠️ Lỗi khi gọi AI: " + e.getMessage());
        }
    }

    @PostMapping("/detect-intent")
    public Map<String, String> detectIntent(@RequestBody Map<String, String> payload) {
        String input = payload.get("input");
        if (input == null || input.isBlank()) {
            return Map.of("intent", "unknown");
        }

        try {
            String modelName = "gemini-2.0-flash";
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + modelName + ":generateContent?key=" + geminiApiKey;

            RestTemplate restTemplate = new RestTemplate();

            String prompt = """
                    Phân loại ý định của người dùng sau:
                    "%s"
                    Các loại có thể:
                    - create_project: nếu người dùng muốn tạo project, dự án mới.
                    - ask_idea: nếu người dùng muốn gợi ý, mô tả, hoặc tư vấn.
                    - other: nếu không thuộc hai loại trên.
                    Chỉ trả về đúng 1 từ khóa: create_project, ask_idea hoặc other.
                    """.formatted(input);

            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> content = Map.of("parts", List.of(textPart));
            Map<String, Object> body = Map.of("contents", List.of(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            Map<String, Object> result = response.getBody();
            String intent = "other";

            if (result != null && result.containsKey("candidates")) {
                List candidates = (List) result.get("candidates");
                if (!candidates.isEmpty()) {
                    Map first = (Map) candidates.get(0);
                    Map contentMap = (Map) first.get("content");
                    List parts = (List) contentMap.get("parts");
                    if (!parts.isEmpty()) {
                        Map textPartMap = (Map) parts.get(0);
                        intent = textPartMap.get("text").toString().trim().toLowerCase();
                    }
                }
            }

            // chỉ giữ lại 3 giá trị hợp lệ
            if (!List.of("create_project", "ask_idea").contains(intent))
                intent = "other";

            return Map.of("intent", intent);

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("intent", "error");
        }
    }
    @PostMapping("/suggest-business-rule")
    public Map<String, String> suggestBusinessRule(@RequestBody Map<String, String> payload) {
        String input = payload.get("input");
        String projectName = payload.getOrDefault("projectName", "Dự án");
        String description = payload.getOrDefault("description", "");

        if ((input == null || input.isBlank()) && description.isBlank()) {
            return Map.of("suggestion", "Hãy nhập tên hoặc mô tả dự án để AI có thể gợi ý quy tắc nghiệp vụ.");
        }

        try {
            String modelName = "gemini-2.0-flash";
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + modelName + ":generateContent?key=" + geminiApiKey;

            RestTemplate restTemplate = new RestTemplate();

            String prompt = """
                Viết một đoạn **Business Rule (quy tắc nghiệp vụ)** rõ ràng, súc tích cho dự án:
                - Tên dự án: %s
                - Mô tả: %s
                - Gợi ý thêm dựa vào: %s

                Trả về bằng tiếng Việt, dễ hiểu, có thể liệt kê theo gạch đầu dòng.
                Không dùng markdown hoặc ký tự **.
                """.formatted(projectName, description, input);

            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> content = Map.of("parts", List.of(textPart));
            Map<String, Object> body = Map.of("contents", List.of(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            Map<String, Object> result = response.getBody();
            String suggestion = "Không thể sinh gợi ý.";

            if (result != null && result.containsKey("candidates")) {
                List candidates = (List) result.get("candidates");
                if (!candidates.isEmpty()) {
                    Map firstCandidate = (Map) candidates.get(0);
                    Map contentMap = (Map) firstCandidate.get("content");
                    List parts = (List) contentMap.get("parts");
                    if (!parts.isEmpty()) {
                        Map textPartMap = (Map) parts.get(0);
                        suggestion = textPartMap.get("text").toString();
                    }
                }
            }

            suggestion = suggestion
                    .replaceAll("\\*\\*", "")
                    .replaceAll("\\*", "")
                    .trim();

            return Map.of("suggestion", suggestion);

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("suggestion", "⚠️ Lỗi khi gọi AI: " + e.getMessage());
        }
    }


}
