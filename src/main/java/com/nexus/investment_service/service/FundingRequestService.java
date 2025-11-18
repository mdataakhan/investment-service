package com.nexus.investment_service.service;

import com.nexus.investment_service.dto.FundingRequestCreationDTO;
import com.nexus.investment_service.model.FundingRequest;
import com.nexus.investment_service.repository.FundingRequestRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class FundingRequestService {

    private final FundingRequestRepository fundingRequestRepository;

    // Status constants for the Funding Request (initial state is OPEN)
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_FUNDED = "FUNDED";
    public static final String STATUS_CLOSED = "CLOSED";

    public FundingRequestService(FundingRequestRepository fundingRequestRepository) {
        this.fundingRequestRepository = fundingRequestRepository;
    }

    /**
     * Creates a new funding request based on funder input.
     * * @param funderId The ID of the authenticated user raising the request.
     * @param dto The data transfer object containing request details.
     * @return The newly created and saved FundingRequest entity.
     */
    public FundingRequest createFundingRequest(String funderId, FundingRequestCreationDTO dto) {

        // 1. Map DTO to Entity
        FundingRequest request = new FundingRequest();
        request.setTitle(dto.getTitle());
        request.setRequiredAmount(dto.getRequiredAmount());
        request.setDeadline(dto.getDeadline());

        // 2. Set System/Initial Values
        request.setFunderId(funderId);
        request.setCreatedAt(LocalDateTime.now());
        request.setCurrentFunded(0.0); // No funding yet
        request.setStatus(STATUS_OPEN); // Ready to receive investments

        // 3. Persist to MongoDB
        return fundingRequestRepository.save(request);
    }

    /**
     * Retrieves a funding request by its ID. (Placeholder for future use)
     */
    public FundingRequest getFundingRequestById(String id) {
        return fundingRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Funding Request not found with ID: " + id));
    }
}