package com.nexus.investment_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "funding_requests")
public class FundingRequest {
    @Id
    private String id;

    private String title;
    private double requiredAmount;

    // Tracks the total amount invested so far, initialized to 0.0
    private double currentFunded;

    // The ID of the user who raised the request (Funder)
    private String funderId;

    // Status can be PENDING, APPROVED, SETTLED, CANCELLED
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime deadline;

    // --- Getters and Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public double getRequiredAmount() { return requiredAmount; }
    public void setRequiredAmount(double requiredAmount) { this.requiredAmount = requiredAmount; }

    public double getCurrentFunded() { return currentFunded; }
    public void setCurrentFunded(double currentFunded) { this.currentFunded = currentFunded; }

    public String getFunderId() { return funderId; }
    public void setFunderId(String funderId) { this.funderId = funderId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
}