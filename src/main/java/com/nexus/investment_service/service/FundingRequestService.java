package com.nexus.investment_service.service;

import com.nexus.investment_service.dto.FundingRequestCreationDTO;
import com.nexus.investment_service.dto.FundingRequestUpdateDTO;
import com.nexus.investment_service.dto.FundingInvestmentDTO;
import com.nexus.investment_service.model.FundingRequest;
import com.nexus.investment_service.repository.FundingRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class FundingRequestService {

    private final FundingRequestRepository fundingRequestRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    // Status constants for the Funding Request
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_FUNDED = "FUNDED";
    public static final String STATUS_CLOSED = "CLOSED";

    private final String userServiceBaseUrl = "http://localhost:3000/api/v1/users";

    public FundingRequestService(FundingRequestRepository fundingRequestRepository) {
        this.fundingRequestRepository = fundingRequestRepository;
    }

    /**
     * Creates a new funding request.
     */
    public FundingRequest createFundingRequest(String funderId, FundingRequestCreationDTO dto) {

        FundingRequest request = new FundingRequest();
        request.setTitle(dto.getTitle());
        request.setRequiredAmount(dto.getRequiredAmount());
        request.setDeadline(dto.getDeadline());

        request.setFunderId(funderId);
        request.setCreatedAt(LocalDateTime.now());
        request.setCurrentFunded(0.0);
        request.setStatus(STATUS_OPEN);

        return fundingRequestRepository.save(request);
    }

    /**
     * Retrieves a funding request by its ID.
     */
    public FundingRequest getFundingRequestById(String id) {
        return fundingRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Funding Request not found with ID: " + id));
    }

    /**
     * Updates an existing funding request, ensuring the funder is the owner.
     */
    public FundingRequest updateFundingRequest(String requestId, String funderId, FundingRequestUpdateDTO dto) {

        FundingRequest existingRequest = getFundingRequestById(requestId);

        if (!existingRequest.getFunderId().equals(funderId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "User is not authorized to update this funding request.");
        }

        if (!existingRequest.getStatus().equals(STATUS_OPEN)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot update a funding request that is not OPEN (Current status: " + existingRequest.getStatus() + ").");
        }

        if (dto.getTitle() != null && !dto.getTitle().isEmpty()) {
            existingRequest.setTitle(dto.getTitle());
        }

        if (dto.getDeadline() != null) {
            existingRequest.setDeadline(dto.getDeadline());
        }

        return fundingRequestRepository.save(existingRequest);
    }

    /**
     * Investor invests in a funding request. Validates availability and calls User Service to deduct wallet balance.
     */
    public FundingRequest investInFundingRequest(String requestId, FundingInvestmentDTO investmentDTO) {
        FundingRequest request = getFundingRequestById(requestId);

        if (!STATUS_OPEN.equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Funding request is not open for investments.");
        }

        double remaining = request.getRequiredAmount() - request.getCurrentFunded();
        if (investmentDTO.getAmount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Investment amount must be positive.");
        }
        if (investmentDTO.getAmount() > remaining) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Investment exceeds remaining required amount. Remaining: " + remaining);
        }

        String investorId = investmentDTO.getInvestorId();
        String userUrl = userServiceBaseUrl + "/" + investorId;

        Map<String, Object> userUpdatePayload = new HashMap<>();
        userUpdatePayload.put("walletBalance", investmentDTO.getAmount()); // interpreted by user service as amount to deduct
        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(userUrl + "/deduct-wallet", userUpdatePayload, Void.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "User service rejected wallet deduction.");
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 400) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient wallet balance.");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "User service error: " + e.getStatusCode());
        }

        request.setCurrentFunded(request.getCurrentFunded() + investmentDTO.getAmount());
        request.addInvestorId(investmentDTO.getInvestorId());
        if (request.getCurrentFunded() >= request.getRequiredAmount()) {
            request.setStatus(STATUS_FUNDED);
        }
        return fundingRequestRepository.save(request);
    }
}