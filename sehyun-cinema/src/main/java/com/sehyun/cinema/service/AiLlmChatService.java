package com.sehyun.cinema.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AI 상담: 규칙(키워드) 우선 → 미매칭 시 OpenAI·Claude·Gemini 순서로 시도.
 * GPT: 지시 준수·일반 Q&A, Claude: 한국어·맥락, Gemini: 속도·비용 (설정 순서에 따라 폴백 체인).
 */
@Service
public class AiLlmChatService {

    private static final String SYSTEM = """
        당신은 '세현시네마' 영화관 온라인 상담원입니다.
        예매·취소·환불·상영시간표·멤버십·IMAX/4DX·주차·분실물 등을 짧고 친절하게 한국어로 안내합니다.
        환각을 피하고, 모르면 FAQ·1:1 문의·고객센터(010-3331-0292) 안내를 권합니다.
        응답은 불필요한 영어 제목 없이 본문만 출력합니다.""";

    private final CustomerService customerService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    @Value("${sehyun.chat.llm.enabled:true}")
    private boolean llmEnabled;

    @Value("${sehyun.chat.openai.api-key:}")
    private String openaiKey;

    @Value("${sehyun.chat.anthropic.api-key:}")
    private String anthropicKey;

    @Value("${sehyun.chat.google.api-key:}")
    private String googleKey;

    /** 쉼표 구분: openai, anthropic, google (별칭 gpt, claude, gemini 허용) */
    @Value("${sehyun.chat.order:openai,anthropic,google}")
    private String providerOrder;

    @Value("${sehyun.chat.openai.model:gpt-4o-mini}")
    private String openaiModel;

    @Value("${sehyun.chat.anthropic.model:claude-3-5-haiku-20241022}")
    private String anthropicModel;

    @Value("${sehyun.chat.google.model:gemini-2.0-flash}")
    private String googleModel;

    public AiLlmChatService(CustomerService customerService, ObjectMapper objectMapper) {
        this.customerService = customerService;
        this.objectMapper = objectMapper;
    }

    public record ChatAnswer(String text, String provider) {}

    public ChatAnswer answer(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return new ChatAnswer("무엇이든 질문해 주세요! 😊", "rule");
        }
        String trimmed = rawMessage.trim();
        Optional<String> rule = customerService.keywordReply(trimmed);
        if (rule.isPresent()) {
            return new ChatAnswer(rule.get(), "rule");
        }
        if (!llmEnabled) {
            return new ChatAnswer(customerService.unknownKeywordFallback(), "fallback");
        }
        boolean anyKey = (openaiKey != null && !openaiKey.isBlank())
                || (anthropicKey != null && !anthropicKey.isBlank())
                || (googleKey != null && !googleKey.isBlank());
        if (!anyKey) {
            return new ChatAnswer(customerService.unknownKeywordFallback(), "fallback");
        }
        for (String id : parseOrder()) {
            Optional<String> llm = switch (normalizeId(id)) {
                case "openai" -> callOpenAi(trimmed);
                case "anthropic" -> callAnthropic(trimmed);
                case "google" -> callGemini(trimmed);
                default -> Optional.empty();
            };
            if (llm.isPresent()) {
                String text = llm.get().strip();
                if (!text.isEmpty()) {
                    return new ChatAnswer(text, normalizeId(id));
                }
            }
        }
        return new ChatAnswer(
                customerService.unknownKeywordFallback() + "\n\n"
                        + "(연결된 AI 서버 응답에 실패했습니다. 잠시 후 다시 시도해 주세요.)",
                "fallback");
    }

    private List<String> parseOrder() {
        return Arrays.stream(providerOrder.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String normalizeId(String id) {
        String s = id.toLowerCase(Locale.ROOT);
        if ("gpt".equals(s)) {
            return "openai";
        }
        if ("claude".equals(s)) {
            return "anthropic";
        }
        if ("gemini".equals(s)) {
            return "google";
        }
        return s;
    }

    private Optional<String> callOpenAi(String userMessage) {
        if (openaiKey == null || openaiKey.isBlank()) {
            return Optional.empty();
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", openaiModel);
            ArrayNode messages = body.putArray("messages");
            messages.addObject().put("role", "system").put("content", SYSTEM);
            messages.addObject().put("role", "user").put("content", userMessage);
            String json = restClient.post()
                    .uri("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + openaiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.toString())
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(json);
            String text = root.path("choices").path(0).path("message").path("content").asText(null);
            return Optional.ofNullable(text);
        } catch (RestClientException | java.io.IOException e) {
            return Optional.empty();
        }
    }

    private Optional<String> callAnthropic(String userMessage) {
        if (anthropicKey == null || anthropicKey.isBlank()) {
            return Optional.empty();
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", anthropicModel);
            body.put("max_tokens", 1024);
            body.put("system", SYSTEM);
            ArrayNode messages = body.putArray("messages");
            messages.addObject().put("role", "user").put("content", userMessage);
            String json = restClient.post()
                    .uri("https://api.anthropic.com/v1/messages")
                    .header("x-api-key", anthropicKey)
                    .header("anthropic-version", "2023-06-01")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.toString())
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(json);
            JsonNode content = root.path("content");
            if (content.isArray() && content.size() > 0) {
                String text = content.get(0).path("text").asText(null);
                return Optional.ofNullable(text);
            }
            return Optional.empty();
        } catch (RestClientException | java.io.IOException e) {
            return Optional.empty();
        }
    }

    private Optional<String> callGemini(String userMessage) {
        if (googleKey == null || googleKey.isBlank()) {
            return Optional.empty();
        }
        try {
            String model = googleModel.contains("/") ? googleModel : "models/" + googleModel;
            ObjectNode body = objectMapper.createObjectNode();
            body.putObject("systemInstruction").putArray("parts").addObject().put("text", SYSTEM);
            ArrayNode contents = body.putArray("contents");
            ObjectNode turn = contents.addObject();
            turn.put("role", "user");
            turn.putArray("parts").addObject().put("text", userMessage);
            String url = "https://generativelanguage.googleapis.com/v1beta/" + model + ":generateContent?key="
                    + googleKey;
            String json = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.toString())
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(json);
            JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
            if (parts.isArray() && parts.size() > 0) {
                String text = parts.get(0).path("text").asText(null);
                return Optional.ofNullable(text);
            }
            return Optional.empty();
        } catch (RestClientException | java.io.IOException e) {
            return Optional.empty();
        }
    }
}
