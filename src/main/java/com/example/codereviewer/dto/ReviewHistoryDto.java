package com.example.codereviewer.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewHistoryDto {
    private Long id;
    private String language;
    private LocalDateTime createdAt;
    private double overallScore;
    private String summary;
}
