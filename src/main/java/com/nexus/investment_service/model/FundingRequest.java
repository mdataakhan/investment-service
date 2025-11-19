package com.nexus.investment_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "funding_requests")
public class FundingRequest {
    @Id
    private String id;

    private String title;
    private double requiredAmount;
    private double currentFunded;
    private String funderId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime deadline;

    // List of investor IDs who have invested
    private List<String> investorIds;

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

    public List<String> getInvestorIds() { return investorIds; }
    public void setInvestorIds(List<String> investorIds) { this.investorIds = investorIds; }

    // Convenience to add an investor safely
    public void addInvestorId(String investorId) {
        if (this.investorIds == null) {
            this.investorIds = new ArrayList<>();
        }
        if (!this.investorIds.contains(investorId)) {
            this.investorIds.add(investorId);
        }
    }
}
