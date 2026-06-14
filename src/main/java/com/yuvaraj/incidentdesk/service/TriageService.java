package com.yuvaraj.incidentdesk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuvaraj.incidentdesk.domain.IncidentType;
import com.yuvaraj.incidentdesk.domain.Priority;
import com.yuvaraj.incidentdesk.dto.IncidentDtos.TriageRequest;
import com.yuvaraj.incidentdesk.dto.IncidentDtos.TriageResponse;
import com.yuvaraj.incidentdesk.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class TriageService {

    private static final List<String> TYPES = Arrays.stream(IncidentType.values()).map(Enum::name).toList();
    private static final List<String> PRIORITIES = Arrays.stream(Priority.values()).map(Enum::name).toList();

    private static final String SYSTEM_INSTRUCTION = """
            You are an incident triage assistant for a venue and facility operations platform.
            Given an incident's title and optional description, classify it and return a JSON object with exactly these keys:
            - "type": one of %s.
            - "priority": one of %s. Judge by safety impact and urgency — life-safety or security threats are CRITICAL; cosmetic or non-urgent issues are LOW.
            - "summary": a single concise sentence (max 140 characters) restating the incident.
            Respond with ONLY the JSON object — no markdown fences, no commentary.""".formatted(String.join(", ", TYPES), String.join(", ", PRIORITIES));

    private final String apiKey;
    private final String model;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RestClient http = RestClient.create();

    public TriageService(@Value("${app.gemini.api-key}") String apiKey,
                         @Value("${app.gemini.model}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    public boolean isEnabled() {
        return StringUtils.hasText(apiKey);
    }

    public TriageResponse triage(TriageRequest input) {
        if (!isEnabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI triage is not configured");
        }

        String description = StringUtils.hasText(input.description()) ? input.description().trim() : "(none provided)";
        String prompt = "Title: " + input.title() + "\n\nDescription: " + description;

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", SYSTEM_INSTRUCTION))),
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("responseMimeType", "application/json", "temperature", 0.2));

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        String text;
        try {
            String raw = http.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = mapper.readTree(raw);
            text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI request failed");
        }

        if (!StringUtils.hasText(text)) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI returned an empty response");
        }

        JsonNode parsed;
        try {
            parsed = mapper.readTree(stripFences(text));
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI returned an unparseable response");
        }

        IncidentType type = TYPES.contains(parsed.path("type").asText())
                ? IncidentType.valueOf(parsed.path("type").asText())
                : IncidentType.INCIDENT;
        Priority priority = PRIORITIES.contains(parsed.path("priority").asText())
                ? Priority.valueOf(parsed.path("priority").asText())
                : Priority.MEDIUM;
        String summary = parsed.path("summary").asText("");
        if (summary.length() > 140) {
            summary = summary.substring(0, 140);
        }
        return new TriageResponse(type, priority, summary);
    }

    private static String stripFences(String text) {
        return text.trim()
                .replaceFirst("^```(?:json)?", "")
                .replaceFirst("```$", "")
                .trim();
    }
}
