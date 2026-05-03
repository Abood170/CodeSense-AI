package com.example.codereviewer.service;

import com.example.codereviewer.dto.GeminiRequest;
import com.example.codereviewer.dto.GeminiResponse;
import com.example.codereviewer.dto.GithubReviewRequest;
import com.example.codereviewer.dto.ReviewHistoryDto;
import com.example.codereviewer.dto.ReviewRequest;
import com.example.codereviewer.dto.ReviewResponse;
import com.example.codereviewer.model.ReviewHistory;
import com.example.codereviewer.model.User;
import com.example.codereviewer.repository.ReviewHistoryRepository;
import jakarta.annotation.PostConstruct;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeReviewService {

    private final WebClient.Builder webClientBuilder;
    private final ReviewHistoryRepository reviewHistoryRepository;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        webClient = webClientBuilder.build();
    }

    public ReviewResponse review(ReviewRequest request, User user) {
        String prompt = buildPrompt(request.getCode(), request.getLanguage());

        GeminiRequest geminiRequest = new GeminiRequest(
                List.of(new GeminiRequest.Content(
                        List.of(new GeminiRequest.Part(prompt))
                ))
        );

        GeminiResponse geminiResponse;
        try {
            geminiResponse = webClient
                    .post()
                    .uri(geminiApiUrl + "?key=" + geminiApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(geminiRequest)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Gemini API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Gemini API returned " + e.getStatusCode() + ": " + e.getStatusText());
        } catch (Exception e) {
            log.error("Failed to call Gemini API", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to reach Gemini API");
        }

        String rawText = extractText(geminiResponse);
        ReviewResponse reviewResponse = parseReviewResponse(rawText);

        saveHistory(request.getCode(), rawText, request.getLanguage(), user);

        return reviewResponse;
    }

    public ReviewResponse reviewFromGithub(GithubReviewRequest request, User user) {
        String githubUrl = request.getGithubUrl();
        if (githubUrl == null || !githubUrl.contains("github.com/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GitHub URL");
        }

        String rawUrl = githubUrl
                .replace("github.com", "raw.githubusercontent.com")
                .replace("/blob/", "/");

        String filename = githubUrl.substring(githubUrl.lastIndexOf('/') + 1).split("\\?")[0];
        String language = detectLanguage(filename);

        String code;
        try {
            code = webClient.get()
                    .uri(rawUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("GitHub fetch error {}: {}", e.getStatusCode(), rawUrl);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not fetch file from GitHub: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to fetch from GitHub: {}", rawUrl, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not reach GitHub");
        }

        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "The file appears to be empty");
        }

        ReviewRequest reviewRequest = new ReviewRequest();
        reviewRequest.setCode(code);
        reviewRequest.setLanguage(language);
        return review(reviewRequest, user);
    }

    private String detectLanguage(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".java"))  return "java";
        if (lower.endsWith(".py"))    return "python";
        if (lower.endsWith(".js"))    return "javascript";
        if (lower.endsWith(".ts"))    return "typescript";
        if (lower.endsWith(".kt"))    return "kotlin";
        if (lower.endsWith(".go"))    return "go";
        if (lower.endsWith(".rs"))    return "rust";
        if (lower.endsWith(".cpp") || lower.endsWith(".cc") || lower.endsWith(".cxx")) return "c++";
        if (lower.endsWith(".cs"))    return "c#";
        if (lower.endsWith(".php"))   return "php";
        if (lower.endsWith(".rb"))    return "ruby";
        if (lower.endsWith(".swift")) return "swift";
        return null;
    }

    private String buildPrompt(String code, String language) {
        return """
                You are an expert code reviewer. Review the following %s code and respond ONLY with a valid JSON object \
                (no markdown, no code fences, no extra text) in this exact format:
                {
                  "summary": "brief overall summary of the code",
                  "bugs": ["bug description 1", "bug description 2"],
                  "security": ["security issue 1", "security issue 2"],
                  "performance": ["performance issue 1"],
                  "suggestions": ["suggestion 1", "suggestion 2"],
                  "overallScore": 7.5,
                  "fixedCode": "minimally fixed version of the code"
                }

                Use empty arrays [] if there are no issues in a category. \
                overallScore must be a number between 0 and 10. \
                For fixedCode: only fix the actual bugs found. Do NOT restructure, reformat, rename, or add anything extra. \
                Keep every unchanged line exactly as-is. On each line you changed, append a short inline comment \
                in the form: // FIXED: <one-line reason>. No other comments.

                Code to review:
                %s
                """.formatted(language != null ? language : "code", code);
    }

    private String extractText(GeminiResponse response) {
        if (response == null
                || response.candidates() == null
                || response.candidates().isEmpty()) {
            return "{}";
        }
        GeminiResponse.Candidate candidate = response.candidates().getFirst();
        if (candidate.content() == null
                || candidate.content().parts() == null
                || candidate.content().parts().isEmpty()) {
            return "{}";
        }
        return candidate.content().parts().getFirst().text();
    }

    private ReviewResponse parseReviewResponse(String rawText) {
        try {
            String json = stripMarkdownFences(rawText);
            return objectMapper.readValue(json, ReviewResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", rawText, e);
            ReviewResponse fallback = new ReviewResponse();
            fallback.setSummary("Could not parse review response.");
            fallback.setBugs(List.of());
            fallback.setSecurity(List.of());
            fallback.setPerformance(List.of());
            fallback.setSuggestions(List.of());
            fallback.setOverallScore(0);
            fallback.setFixedCode("");
            return fallback;
        }
    }

    private String stripMarkdownFences(String text) {
        if (text == null) return "{}";
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline != -1 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    public List<ReviewHistoryDto> getReviews(User user) {
        return reviewHistoryRepository.findAllByUserOrderByCreatedAtDesc(user).stream()
                .map(h -> {
                    ReviewResponse parsed = parseReviewResponse(h.getReview());
                    return ReviewHistoryDto.builder()
                            .id(h.getId())
                            .language(h.getLanguage())
                            .createdAt(h.getCreatedAt())
                            .overallScore(parsed.getOverallScore())
                            .summary(parsed.getSummary())
                            .build();
                })
                .toList();
    }

    private void saveHistory(String code, String rawReview, String language, User user) {
        ReviewHistory history = ReviewHistory.builder()
                .code(code)
                .review(rawReview)
                .language(language)
                .user(user)
                .build();
        reviewHistoryRepository.save(history);
    }
}