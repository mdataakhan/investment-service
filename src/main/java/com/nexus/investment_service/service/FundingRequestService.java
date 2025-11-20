package com.nexus.investment_service.service;

import com.nexus.investment_service.dto.FundingRequestCreationDTO;
import com.nexus.investment_service.dto.FundingRequestUpdateDTO;
import com.nexus.investment_service.dto.FundingInvestmentDTO;
import com.nexus.investment_service.dto.UserUpdateRequestDTO;
import com.nexus.investment_service.model.FundingRequest;
import com.nexus.investment_service.repository.FundingRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.nexus.investment_service.utils.Constants.*;

@Service
public class FundingRequestService {

    private final FundingRequestRepository fundingRequestRepository;
    private final WebClient webClient;

    public FundingRequestService(FundingRequestRepository fundingRequestRepository) {
        this.fundingRequestRepository = fundingRequestRepository;
        this.webClient = WebClient.builder().baseUrl(USER_SERVICE_BASE_URL).build();
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
     * Retrieves all funding requests.
     */
    public List<FundingRequest> getAllFundingRequests() {
        return fundingRequestRepository.findAll();
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
        double walletAdjustment = investmentDTO.getWalletAdjustment();
        if (walletAdjustment >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "walletAdjustment must be negative for deduction.");
        }
        if (Math.abs(walletAdjustment) > remaining) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Investment exceeds remaining required amount. Remaining: " + remaining);
        }
        String investorId = investmentDTO.getInvestorId();
        UserUpdateRequestDTO userUpdate = new UserUpdateRequestDTO();
        userUpdate.setWalletAdjustment(BigDecimal.valueOf(walletAdjustment)); // negative value for deduction
        userUpdate.setFundingRequestIds(List.of(requestId));
        try {
            webClient.put()
                    .uri("/" + investorId)
                    .bodyValue(userUpdate)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient wallet balance.");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "User service error: " + e.getStatusCode());
        }
        request.setCurrentFunded(request.getCurrentFunded() + Math.abs(walletAdjustment));
        request.addInvestorId(investorId);
        if (request.getCurrentFunded() >= request.getRequiredAmount()) {
            request.setStatus(STATUS_FUNDED);
        }
        return fundingRequestRepository.save(request);
    }
}

