package com.ibtrader.application.service;

import com.ibtrader.domain.port.outbound.ExecutionPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service to manage execution policies.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionPolicyService {

    private final ExecutionPolicyRepository executionPolicyRepository;

    public String getPolicy(String policyName) {
        return executionPolicyRepository.getPolicy(policyName)
                .orElse("IMMEDIATE");
    }
}
