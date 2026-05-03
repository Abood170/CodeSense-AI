package com.example.codereviewer.dto;

import lombok.Data;

@Data
public class ReviewRequest {
    private String code;
    private String language;
}