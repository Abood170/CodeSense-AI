package com.example.codereviewer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewResponse {
    private String summary;
    private List<String> bugs;
    private List<String> security;
    private List<String> performance;
    private List<String> suggestions;
    private double overallScore;
    private String fixedCode;
}