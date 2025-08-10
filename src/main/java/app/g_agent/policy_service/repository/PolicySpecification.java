package app.g_agent.policy_service.repository;

import org.springframework.data.jpa.domain.Specification;

import app.g_agent.policy_service.model.Policy;

import jakarta.persistence.criteria.Predicate;

public class PolicySpecification {

    public static Specification<Policy> matchesKeyword(String keyword, int organizationId) {
        return (root, query, builder) -> {
            if ((keyword == null || keyword.trim().isEmpty())) {
                // Prevent returning all policies
                return builder.disjunction();
            }

            Predicate orgPredicate = builder.equal(root.get("companyId"), organizationId);

            if (keyword == null || keyword.trim().isEmpty()) {
                return orgPredicate;
            }

            String likeKeyword = "%" + keyword.toLowerCase() + "%";
            Predicate keywordPredicate = builder.or(
                    builder.like(builder.lower(root.get("policyNumber")), likeKeyword),
                    builder.like(builder.lower(root.get("certificateNumber")), likeKeyword)
            );

            return builder.and(orgPredicate, keywordPredicate);
        };
    }
}
