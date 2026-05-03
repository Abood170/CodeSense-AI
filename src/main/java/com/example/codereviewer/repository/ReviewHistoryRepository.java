package com.example.codereviewer.repository;

import com.example.codereviewer.model.ReviewHistory;
import com.example.codereviewer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewHistoryRepository extends JpaRepository<ReviewHistory, Long> {
    List<ReviewHistory> findAllByUserOrderByCreatedAtDesc(User user);
}