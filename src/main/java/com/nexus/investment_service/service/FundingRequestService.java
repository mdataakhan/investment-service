package com.nexus.investment_service.service;

import com.nexus.investment_service.dto.FundingRequestCreationDTO;
import com.nexus.investment_service.dto.FundingRequestUpdateDTO;
import com.nexus.investment_service.dto.FundingInvestmentDTO;
import com.nexus.investment_service.dto.UserUpdateRequestDTO;
import com.nexus.investment_service.model.FundingRequest;
import com.nexus.investment_service.repository.FundingRequestRepository;
import com.nexus.investment_service.utils.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;

import static com.nexus.investment_service.utils.Constants.*;

@Service
public class FundingRequestService {

    private static final Logger log = LoggerFactory.getLogger(FundingRequestService.class);

    private final FundingRequestRepository fundingRequestRepository;
    private final WebClient webClient; // injected bean configured in WebClientConfig

    public FundingRequestService(FundingRequestRepository fundingRequestRepository, WebClient webClient) {
        this.fundingRequestRepository = fundingRequestRepository;
        this.webClient = webClient;
    }

    public FundingRequest createFundingRequest(String funderId, FundingRequestCreationDTO dto) {
        log.info("Creating funding request funderId={} title={} requiredAmount={} deadline={} committedReturnAmount={}", funderId, dto.getTitle(), dto.getRequiredAmount(), dto.getDeadline(), dto.getCommittedReturnAmount());
        FundingRequest request = new FundingRequest();
        request.setTitle(dto.getTitle());
        request.setRequiredAmount(dto.getRequiredAmount());
        request.setDeadline(dto.getDeadline());
        request.setCommittedReturnAmount(dto.getCommittedReturnAmount());
        request.setDescription(dto.getDescription());
        request.setInvestorAmounts(new HashMap<>());
        request.setReturnDistributed(false);
        request.setFunderId(funderId);
        request.setCreatedAt(LocalDateTime.now());
        request.setCurrentFunded(0.0);
        request.setStatus(STATUS_OPEN);
        FundingRequest saved = fundingRequestRepository.save(request);
        log.debug("Funding request created id={}", saved.getId());
        return saved;
    }

    public FundingRequest getFundingRequestById(String id) {
        log.debug("Fetching funding request id={}", id);
        return fundingRequestRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Funding request not found id={}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Funding Request not found with ID: " + id);
                });
    }

    public List<FundingRequest> getAllFundingRequests() {
        log.debug("Fetching all funding requests");
        return fundingRequestRepository.findAll();
    }

    public FundingRequest updateFundingRequest(String requestId, String funderId, FundingRequestUpdateDTO dto) {
        log.info("Updating funding request id={} by funderId={}", requestId, funderId);
        FundingRequest existingRequest = getFundingRequestById(requestId);
        Validation.validateOwnership(existingRequest, funderId);
        Validation.validateRequestOpen(existingRequest, "update");

        if (dto.getTitle() != null && !dto.getTitle().isEmpty()) {
            log.debug("Updating title for funding request id={}", requestId);
            existingRequest.setTitle(dto.getTitle());
        }
        if (dto.getDeadline() != null) {
            log.debug("Updating deadline for funding request id={}", requestId);
            existingRequest.setDeadline(dto.getDeadline());
        }
        if (dto.getDescription() != null && !dto.getDescription().isEmpty()) {
            log.debug("Updating description for funding request id={}", requestId);
            existingRequest.setDescription(dto.getDescription());
        }
        if (dto.getCommittedReturnAmount() != null) {
            log.debug("Updating committedReturnAmount for funding request id={}", requestId);
            existingRequest.setCommittedReturnAmount(dto.getCommittedReturnAmount());
        }

        FundingRequest saved = fundingRequestRepository.save(existingRequest);
        log.info("Funding request updated id={}", saved.getId());
        return saved;
    }

    public FundingRequest investInFundingRequest(String requestId, FundingInvestmentDTO investmentDTO) {
        log.info("Investment attempt requestId={} investorId={} walletAdjustment={}", requestId, investmentDTO.getInvestorId(), investmentDTO.getWalletAdjustment());
        FundingRequest request = getFundingRequestById(requestId);
        Validation.validateRequestOpen(request, "invest in");

        double walletAdjustment = investmentDTO.getWalletAdjustment();
        Validation.validateInvestment(request, walletAdjustment);

        String investorId = investmentDTO.getInvestorId();
        UserUpdateRequestDTO userUpdate = new UserUpdateRequestDTO();
        userUpdate.setWalletAdjustment(BigDecimal.valueOf(walletAdjustment)); // negative value for deduction
        userUpdate.setFundingRequestIds(List.of(requestId));

        updateWallet(investorId, userUpdate);

        double investedAmount = Math.abs(walletAdjustment); // walletAdjustment is negative
        request.setCurrentFunded(request.getCurrentFunded() + investedAmount);
        request.updateInvestorAmount(investorId, investedAmount);
        if (request.getCurrentFunded() >= request.getRequiredAmount()) {
            request.setStatus(STATUS_FUNDED);
            log.info("Funding request fully funded id={}", requestId);
        }
        FundingRequest saved = fundingRequestRepository.save(request);
        log.info("Recorded investment requestId={} newCurrentFunded={}", requestId, saved.getCurrentFunded());
        return saved;
    }

    /**
     * Distributes returns (principal + pro-rata committed return) to all investors and marks distributed.
     */
    public FundingRequest distributeReturns(String requestId) {
        FundingRequest request = getFundingRequestById(requestId);
        if (!STATUS_FUNDED.equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot distribute returns for a request that is not FUNDED.");
        }
        if (request.isReturnDistributed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Returns already distributed for this request.");
        }
        if (request.getInvestorAmounts() == null || request.getInvestorAmounts().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No investors to distribute returns to.");
        }
        double required = request.getRequiredAmount();
        double committedReturn = request.getCommittedReturnAmount();
        log.info("Distributing returns requestId={} committedReturnAmount={} investorCount={}", requestId, committedReturn, request.getInvestorAmounts().size());

        request.getInvestorAmounts().forEach((investorId, investedPrincipal) -> {
            double ratio = investedPrincipal / required;
            double returnShare = ratio * committedReturn;
            double totalCredit = investedPrincipal + returnShare;
            UserUpdateRequestDTO creditPayload = new UserUpdateRequestDTO();
            creditPayload.setWalletAdjustment(BigDecimal.valueOf(totalCredit)); // positive credit
            creditPayload.setFundingRequestIds(List.of(requestId));
            log.info("Crediting investorId={} principal={} returnShare={} totalCredit={}", investorId, investedPrincipal, returnShare, totalCredit);
            updateWallet(investorId, creditPayload);
        });

        request.setReturnDistributed(true);
        FundingRequest saved = fundingRequestRepository.save(request);
        log.info("Return distribution complete requestId={}", requestId);
        return saved;
    }


    private void updateWallet(String investorId, UserUpdateRequestDTO userUpdate) {
        try {
            webClient.put()
                    .uri("/" + investorId)
                    .bodyValue(userUpdate)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.debug("Wallet update successful investorId={}", investorId);
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.warn("Wallet update rejected investorId={} statusCode={}", investorId, e.getStatusCode());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wallet update failed.");
            }
            log.error("User service error investorId={} statusCode={} body={}", investorId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "User service error: " + e.getStatusCode());
        }
    }
}

