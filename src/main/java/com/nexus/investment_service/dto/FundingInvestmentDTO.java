package com.nexus.investment_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class FundingInvestmentDTO {

    @NotBlank
    private String investorId;

    @Min(1)
    private double amount; // investment amount

    public String getInvestorId() { return investorId; }
    public void setInvestorId(String investorId) { this.investorId = investorId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}

