package app.g_agent.policy_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.g_agent.policy_service.model.Policy;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
    Optional<Policy> findById(Long id);

    Optional<Policy> findByIdAndCompanyId(Long id, Long companyId);

    Optional<List<Policy>> findByCompanyId(Long companyId);
}