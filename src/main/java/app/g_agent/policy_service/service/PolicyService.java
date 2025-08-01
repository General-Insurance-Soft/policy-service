package app.g_agent.policy_service.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.g_agent.policy_service.dto.PolicyDocumentDto;
import app.g_agent.policy_service.dto.PolicyDto;
import app.g_agent.policy_service.model.Policy;
import app.g_agent.policy_service.model.PolicyDocument;
import app.g_agent.policy_service.repository.PolicyDocumentRepository;
import app.g_agent.policy_service.repository.PolicyRepository;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class PolicyService {

    private static final Logger logger = LoggerFactory.getLogger(PolicyService.class);

    private PolicyRepository policyRepository;
    private PolicyDocumentRepository policyDocumentRepository;
    private JwtService jwtService;

    public PolicyService(PolicyRepository policyRepository, PolicyDocumentRepository policyDocumentRepository,
            JwtService jwtService) {
        this.policyRepository = policyRepository;
        this.policyDocumentRepository = policyDocumentRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public void deletePolicy(HttpServletRequest request, Long id) throws Exception {
        Optional<Policy> policyOpt = policyRepository.findById(id);

        if (policyOpt.isPresent()) {
            policyRepository.delete(policyOpt.get());
        } else {
            throw new Exception("The policy cannot be found");
        }
    }

    public PolicyDto getPolicyById(HttpServletRequest request, Long id) throws Exception {
        Optional<Policy> policyOpt = policyRepository.findById(id);

        if (policyOpt.isPresent()) {
            Policy policy = policyOpt.get();
            PolicyDto policyDto = new PolicyDto();
            policyDto.setId(policy.getId());
            policyDto.setCertificateNumber(policy.getCertificateNumber());
            policyDto.setPolicyNumber(policy.getPolicyNumber());
            policyDto.setProposalId(policy.getProposalId());
            policyDto.setInsuranceCompanyId(policy.getInsuranceCompanyId());
            policyDto.setPolicyTypeId(policy.getPolicyTypeId());
            policyDto.setStartDate(policy.getStartDate());
            policyDto.setEndDate(policy.getEndDate());
            policyDto.setCompanyId(policy.getCompanyId());
            policyDto.setContactId(policy.getContactId());
            policyDto.setUpdatedBy(policy.getUpdatedBy());
            policyDto.setCreatedAt(policy.getCreatedAt());
            policyDto.setUpdatedAt(policy.getUpdatedAt());

            Set<PolicyDocumentDto> policyDocumentDtos = policy.getPolicyDocuments().stream().map(document -> {
                PolicyDocumentDto documentDto = new PolicyDocumentDto();
                documentDto.setId(document.getId());
                documentDto.setFolderName(document.getFolderName());
                documentDto.setDocumentName(document.getDocumentName());
                documentDto.setBlobUrl(document.getBlobUrl());
                documentDto.setUpdatedBy(document.getUpdatedBy());
                documentDto.setCreatedAt(document.getCreatedAt());
                return documentDto;
            }).collect(Collectors.toSet());

            policyDto.setPolicyDocuments(policyDocumentDtos);

            return policyDto;
        } else {
            throw new Exception("The policy does not exist");
        }
    }

    @Transactional
    public void createPolicy(HttpServletRequest request, PolicyDto policyDto) throws Exception {
        Policy policy = new Policy();

        Long userId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "user-id").toString());
        Long orgId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "organization-id").toString());
        logger.info("user ID: ==============================>" + userId);
        policy.setInsuranceCompanyId(policyDto.getInsuranceCompanyId());
        policy.setPolicyTypeId(policyDto.getPolicyTypeId());
        policy.setCertificateNumber(policyDto.getCertificateNumber());
        policy.setProposalId(policyDto.getProposalId());
        policy.setPolicyNumber(policyDto.getPolicyNumber());
        policy.setStartDate(policyDto.getStartDate());
        policy.setEndDate(policyDto.getEndDate());
        policy.setCompanyId(orgId);
        policy.setContactId(policyDto.getContactId());
        policy.setUpdatedBy(Long.valueOf(userId));

        if (policyDto.getPolicyDocuments() != null) {
            Set<PolicyDocument> policyDocuments = new HashSet<>();
            policyDto.getPolicyDocuments().forEach(documentDto -> {
                PolicyDocument document = new PolicyDocument();
                document.setFolderName(documentDto.getFolderName());
                document.setDocumentName(documentDto.getDocumentName());
                document.setBlobUrl(documentDto.getBlobUrl());
                document.setUpdatedBy(Long.valueOf(userId));
                document.setPolicy(policy); // Set the policy reference
                policyDocuments.add(document);
            });
            policy.setPolicyDocuments(policyDocuments);
        }

        try {
            policyRepository.save(policy);
            policyDocumentRepository.saveAll(policy.getPolicyDocuments()); // Save the policy documents
        } catch (DataIntegrityViolationException ex) {
            if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                logger.info("Policy error ==========> id: " + ex.getMessage());
                throw new Exception("This policy already exists.");
            }
            throw ex; // Rethrow if not related to constraint violation
        }
    }

    @Transactional
    public void updatePolicy(HttpServletRequest request, PolicyDto policyDto, Long id) throws Exception {
        Optional<Policy> policyOpt = policyRepository.findById(id);

        if (policyOpt.isEmpty()) {
            throw new Exception("The policy cannot be found");
        }

        Policy policy = policyOpt.get();

        Long userId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "user-id").toString());

        policy.setInsuranceCompanyId(policyDto.getInsuranceCompanyId());
        policy.setPolicyTypeId(policyDto.getPolicyTypeId());
        policy.setCertificateNumber(policyDto.getCertificateNumber());
        policy.setPolicyNumber(policyDto.getPolicyNumber());
        policy.setProposalId(policyDto.getProposalId());
        policy.setStartDate(policyDto.getStartDate());
        policy.setEndDate(policyDto.getEndDate());
        policy.setContactId(policyDto.getContactId());
        policy.setUpdatedBy(Long.valueOf(userId));

        try {
            policyRepository.save(policy);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                logger.info("Policy error ==========> id: " + ex.getMessage());
                throw new Exception("This policy already exists.");
            }
            throw ex; // Rethrow if not related to constraint violation
        }
    }

    @Transactional
    public List<PolicyDto> getPolicys(HttpServletRequest request) throws Exception {
        List<Policy> policys = policyRepository.findAll();

        return policys.stream().map(policy -> {
            PolicyDto policyDto = new PolicyDto();
            policyDto.setId(policy.getId());
            policyDto.setInsuranceCompanyId(policy.getInsuranceCompanyId());
            policyDto.setPolicyTypeId(policy.getPolicyTypeId());
            policyDto.setCertificateNumber(policy.getCertificateNumber());
            policyDto.setPolicyNumber(policy.getPolicyNumber());
            policyDto.setProposalId(policy.getProposalId());
            policyDto.setStartDate(policy.getStartDate());
            policyDto.setEndDate(policy.getEndDate());
            policyDto.setCompanyId(policy.getCompanyId());
            policyDto.setContactId(policy.getContactId());
            policyDto.setUpdatedBy(policy.getUpdatedBy());
            policyDto.setCreatedAt(policy.getCreatedAt());
            policyDto.setUpdatedAt(policy.getUpdatedAt());

            Set<PolicyDocumentDto> policyDocumentDtos = policy.getPolicyDocuments().stream().map(document -> {
                PolicyDocumentDto documentDto = new PolicyDocumentDto();
                documentDto.setId(document.getId());
                documentDto.setFolderName(document.getFolderName());
                documentDto.setDocumentName(document.getDocumentName());
                documentDto.setBlobUrl(document.getBlobUrl());
                documentDto.setUpdatedBy(document.getUpdatedBy());
                documentDto.setCreatedAt(document.getCreatedAt());
                return documentDto;
            }).collect(Collectors.toSet());

            policyDto.setPolicyDocuments(policyDocumentDtos);

            return policyDto;
        }).collect(Collectors.toList());
    }

}