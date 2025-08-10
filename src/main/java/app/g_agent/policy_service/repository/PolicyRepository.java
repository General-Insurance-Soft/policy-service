package app.g_agent.policy_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.g_agent.policy_service.model.Policy;

public interface PolicyRepository extends JpaRepository<Policy, Long>, JpaSpecificationExecutor<Policy> {
    Optional<Policy> findById(Long id);

    Optional<Policy> findByIdAndCompanyId(Long id, Long companyId);

    Optional<List<Policy>> findByCompanyId(Long companyId);

    @Query("SELECT DISTINCT p FROM Policy p LEFT JOIN FETCH p.policyDocuments WHERE p.id IN :ids")
    List<Policy> findAllWithDocumentsByIds(@Param("ids") Set<Long> ids);

    List<Policy> findByContactIdAndCompanyId(Long contactId, Long companyId);

    @Query(value = """
            SELECT * FROM (
                SELECT p.*
                     , sub.policy_count
                FROM (
                    SELECT *,
                           ROW_NUMBER() OVER (PARTITION BY contact_id ORDER BY created_at DESC) AS row_num
                    FROM policy
                    WHERE company_id = :companyId
                ) p
                JOIN (
                    SELECT contact_id, COUNT(*) AS policy_count
                    FROM policy
                    WHERE company_id = :companyId
                    GROUP BY contact_id
                ) sub ON p.contact_id = sub.contact_id
                WHERE p.row_num = 1
            ) result
            """, countQuery = """
            SELECT COUNT(*) FROM (
                SELECT contact_id
                FROM policy
                WHERE company_id = :companyId
                GROUP BY contact_id
            ) AS count_table
            """, nativeQuery = true)
    Page<Object[]> findLatestPoliciesPerContact(Pageable pageable, @Param("companyId") Long companyId);

    @Query("SELECT p FROM Policy p WHERE p.contactId IN :contactIds AND p.companyId = :companyId")
    List<Policy> findByContactIdInAndCompanyId(@Param("contactIds") List<Long> contactIds,
            @Param("companyId") int companyId);
}