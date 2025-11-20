package com.nexus.investment_service.controller;

import com.nexus.investment_service.dto.FundingRequestCreationDTO;
import com.nexus.investment_service.dto.FundingRequestUpdateDTO;
import com.nexus.investment_service.dto.FundingInvestmentDTO;
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
            @RequestHeader("X-Funder-Id") String funderId,
            @RequestBody FundingRequestCreationDTO requestDTO) {

        FundingRequest newRequest = fundingRequestService.createFundingRequest(
                funderId,
                requestDTO
        );

        return new ResponseEntity<>(newRequest, HttpStatus.CREATED);
    }

    /**
     * Endpoint to retrieve a specific funding request by its ID.
     * METHOD: GET
     * PATH: /api/v1/funding-requests/{requestId}
     */
    @GetMapping("/{requestId}")
    public ResponseEntity<FundingRequest> getFundingRequestById(@PathVariable String requestId) {

        // Delegate the lookup to the service
        FundingRequest request = fundingRequestService.getFundingRequestById(requestId);

        // If the service throws a NotFound exception, Spring handles the 404 response.
        return ResponseEntity.ok(request);
    }

    /**
     * Endpoint to update an existing funding request.
     * Only the original funder should be able to update their request.
     * METHOD: PUT
     * PATH: /api/v1/funding-requests/{requestId}
     */
    @PutMapping("/{requestId}")
    public ResponseEntity<FundingRequest> updateFundingRequest(
            @PathVariable String requestId,
            @RequestHeader("X-Funder-Id") String funderId,
            @Valid @RequestBody FundingRequestUpdateDTO updateDTO) {

        // Delegate the update logic to the service
        FundingRequest updatedRequest = fundingRequestService.updateFundingRequest(
                requestId,
                funderId,
                updateDTO
        );

        return ResponseEntity.ok(updatedRequest);
    }

    /**
     * Endpoint for an investor to invest in a funding request.
     * METHOD: POST
     * PATH: /api/v1/funding-requests/{requestId}/investment
     */
    @PostMapping("/{requestId}/investment")
    public ResponseEntity<FundingRequest> investInFundingRequest(
            @PathVariable String requestId,
            @RequestBody FundingInvestmentDTO investmentDTO) {
        FundingRequest updated = fundingRequestService.investInFundingRequest(requestId, investmentDTO);
        return ResponseEntity.ok(updated);
    }
}