package com.example.codereviewer.controller;

import com.example.codereviewer.dto.GithubReviewRequest;
import com.example.codereviewer.dto.ReviewHistoryDto;
import com.example.codereviewer.dto.ReviewRequest;
import com.example.codereviewer.dto.ReviewResponse;
import com.example.codereviewer.model.User;
import com.example.codereviewer.repository.UserRepository;
import com.example.codereviewer.service.CodeReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CodeReviewController {

    private final CodeReviewService codeReviewService;
    private final UserRepository userRepository;

    @PostMapping("/review")
    public ResponseEntity<ReviewResponse> review(@RequestBody ReviewRequest request) {
        User user = currentUser();
        return ResponseEntity.ok(codeReviewService.review(request, user));
    }

    @PostMapping("/review/github")
    public ResponseEntity<ReviewResponse> reviewFromGithub(@RequestBody GithubReviewRequest request) {
        return ResponseEntity.ok(codeReviewService.reviewFromGithub(request, currentUser()));
    }

    @GetMapping("/reviews")
    public ResponseEntity<List<ReviewHistoryDto>> getReviews() {
        return ResponseEntity.ok(codeReviewService.getReviews(currentUser()));
    }

    private User currentUser() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
