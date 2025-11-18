package com.nexus.investment_service.repository;

import com.nexus.investment_service.model.FundingRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for CRUD operations on FundingRequest documents.
 */
@Repository
public interface FundingRequestRepository extends MongoRepository<FundingRequest, String> {
}