package com.nexus.investment_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class FundingRequestCreationDTO {

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Required amount is mandatory")
    @DecimalMin(value = "100.00", inclusive = true, message = "Amount must be at least 100.00")
    private Double requiredAmount;

    @NotNull(message = "Deadline is required")
    @Future(message = "Deadline must be in the future")
    private LocalDateTime deadline;

    // --- Getters and Setters ---

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Double getRequiredAmount() {
        return requiredAmount;
    }

    public void setRequiredAmount(Double requiredAmount) {
        this.requiredAmount = requiredAmount;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }
}