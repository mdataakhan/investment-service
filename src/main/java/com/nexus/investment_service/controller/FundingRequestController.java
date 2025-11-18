package com.nexus.investment_service.controller;

import com.nexus.investment_service.dto.FundingRequestCreationDTO;
import com.nexus.investment_service.model.FundingRequest;
import com.nexus.investment_service.service.FundingRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/funding-requests")
public class FundingRequestController {

    private final FundingRequestService fundingRequestService;

    public FundingRequestController(FundingRequestService fundingRequestService) {
        this.fundingRequestService = fundingRequestService;
    }

    /**
     * Endpoint for a user with the FUNDER role to raise a new funding request.
     * METHOD: POST
     * PATH: /api/v1/funding-requests
     */
    @PostMapping
    public ResponseEntity<FundingRequest> createFundingRequest(
            // The Funder ID is extracted from the authenticated user's security context
            // Using a RequestHeader as a mock extraction for demonstration
            @RequestHeader("X-Funder-Id") String funderId,

            // The request body containing the funding details
            @RequestBody FundingRequestCreationDTO requestDTO) {

        // Delegate to the service layer
        FundingRequest newRequest = fundingRequestService.createFundingRequest(
                funderId,
                requestDTO
        );

        // Return the created request object with HTTP 201 Created status
        return new ResponseEntity<>(newRequest, HttpStatus.CREATED);
    }
}