package app.g_agent.policy_service.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.g_agent.policy_service.dto.PolicyDocumentDto;
import app.g_agent.policy_service.model.PolicyDocument;
import app.g_agent.policy_service.repository.PolicyDocumentRepository;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class PolicyDocumentService {

    private static final Logger logger = LoggerFactory.getLogger(PolicyService.class);

    private PolicyDocumentRepository policyDocumentRepository;
    private JwtService jwtService;

    public PolicyDocumentService(PolicyDocumentRepository policyDocumentRepository, JwtService jwtService) {
        this.policyDocumentRepository = policyDocumentRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public void createPolicyDocument(HttpServletRequest request, PolicyDocumentDto policyDocumentDto) throws Exception {
        PolicyDocument policyDocument = new PolicyDocument();

        int userId = (int) jwtService.getTokenValue(jwtService.getJWT(request), "user-id");

        policyDocument.setFolderName(policyDocumentDto.getFolderName());
        policyDocument.setDocumentName(policyDocumentDto.getDocumentName());
        policyDocument.setBlobUrl(policyDocumentDto.getBlobUrl());
        policyDocument.setUpdatedBy(Long.valueOf(userId));

        try {
            policyDocumentRepository.save(policyDocument);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                logger.info("PolicyDocument error ==========> id: " + ex.getMessage());
                throw new Exception("This policy document already exists.");
            }
            throw ex; // Rethrow if not related to constraint violation
        }
    }

    @Transactional
    public void updatePolicyDocument(HttpServletRequest request, PolicyDocumentDto policyDocumentDto, Long id) throws Exception {
        Optional<PolicyDocument> policyDocumentOpt = policyDocumentRepository.findById(id);

        if (policyDocumentOpt.isEmpty()) {
            throw new Exception("The policy document cannot be found");
        }

        PolicyDocument policyDocument = policyDocumentOpt.get();

        int userId = (int) jwtService.getTokenValue(jwtService.getJWT(request), "user-id");

        policyDocument.setFolderName(policyDocumentDto.getFolderName());
        policyDocument.setDocumentName(policyDocumentDto.getDocumentName());
        policyDocument.setBlobUrl(policyDocumentDto.getBlobUrl());
        policyDocument.setUpdatedBy(Long.valueOf(userId));

        try {
            policyDocumentRepository.save(policyDocument);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                logger.info("PolicyDocument error ==========> id: " + ex.getMessage());
                throw new Exception("This policy document already exists.");
            }
            throw ex; // Rethrow if not related to constraint violation
        }
    }

    @Transactional
    public void deletePolicyDocument(HttpServletRequest request, Long id) throws Exception {
        Optional<PolicyDocument> policyDocumentOpt = policyDocumentRepository.findById(id);

        if (policyDocumentOpt.isPresent()) {
            policyDocumentRepository.delete(policyDocumentOpt.get());
        } else {
            throw new Exception("The policy document cannot be found");
        }
    }

    public PolicyDocumentDto getPolicyDocument(HttpServletRequest request, Long id) throws Exception {
        Optional<PolicyDocument> policyDocumentOpt = policyDocumentRepository.findById(id);

        if (policyDocumentOpt.isPresent()) {
            PolicyDocumentDto policyDocumentDto = new PolicyDocumentDto();
            policyDocumentDto.setId(policyDocumentOpt.get().getId());
            policyDocumentDto.setPolicyId(policyDocumentOpt.get().getPolicy().getId());
            policyDocumentDto.setFolderName(policyDocumentOpt.get().getFolderName());
            policyDocumentDto.setDocumentName(policyDocumentOpt.get().getDocumentName());
            policyDocumentDto.setBlobUrl(policyDocumentOpt.get().getBlobUrl());
            policyDocumentDto.setUpdatedBy(policyDocumentOpt.get().getUpdatedBy());
            policyDocumentDto.setCreatedAt(policyDocumentOpt.get().getCreatedAt());

            return policyDocumentDto;
        } else {
            throw new Exception("The policy document does not exist");
        }
    }

    public List<PolicyDocumentDto> getPolicyDocuments(HttpServletRequest request) throws Exception {
        List<PolicyDocument> policyDocuments = policyDocumentRepository.findAll();

        return policyDocuments.stream().map(policyDocument -> {
            PolicyDocumentDto policyDocumentDto = new PolicyDocumentDto();
            policyDocumentDto.setId(policyDocument.getId());
            policyDocumentDto.setPolicyId(policyDocument.getPolicy().getId());
            policyDocumentDto.setFolderName(policyDocument.getFolderName());
            policyDocumentDto.setDocumentName(policyDocument.getDocumentName());
            policyDocumentDto.setBlobUrl(policyDocument.getBlobUrl());
            policyDocumentDto.setUpdatedBy(policyDocument.getUpdatedBy());
            policyDocumentDto.setCreatedAt(policyDocument.getCreatedAt());
            return policyDocumentDto;
        }).collect(Collectors.toList());
    }
}