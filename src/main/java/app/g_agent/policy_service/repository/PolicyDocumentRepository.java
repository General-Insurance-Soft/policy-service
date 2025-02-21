package app.g_agent.policy_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.g_agent.policy_service.model.PolicyDocument;

public interface PolicyDocumentRepository extends JpaRepository<PolicyDocument, Long> {
    Optional<PolicyDocument> findById(Long id);

    Optional<List<PolicyDocument>> findByPolicyId(Long policyId);
}