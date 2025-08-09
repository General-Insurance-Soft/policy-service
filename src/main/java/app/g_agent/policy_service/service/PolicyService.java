package app.g_agent.policy_service.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

import app.g_agent.policy_service.dto.ContactAddressWrapper;
import app.g_agent.policy_service.dto.PolicyDocumentDto;
import app.g_agent.policy_service.dto.PolicyDto;
import app.g_agent.policy_service.dto.UserDto;
import app.g_agent.policy_service.model.Policy;
import app.g_agent.policy_service.model.PolicyDocument;
import app.g_agent.policy_service.repository.PolicyDocumentRepository;
import app.g_agent.policy_service.repository.PolicyRepository;
import app.g_agent.policy_service.system.DuplicateContactException;
import jakarta.servlet.http.HttpServletRequest;
import app.g_agent.policy_service.service.ContactsDataClient;
import app.g_agent.policy_service.service.UsersDataClient;

@Service
public class PolicyService {

    private static final Logger logger = LoggerFactory.getLogger(PolicyService.class);

    private PolicyRepository policyRepository;
    private PolicyDocumentRepository policyDocumentRepository;
    private JwtService jwtService;

    @Autowired
    private ContactsDataClient contactsDataClient;
    @Autowired
    private UsersDataClient usersDataClient;

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

    public PolicyDto getPolicyByIdandOrg(HttpServletRequest request, Long id) throws Exception {

        Long orgId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "organization-id").toString());

        Optional<Policy> policyOpt = policyRepository.findByIdAndCompanyId(id, orgId);

        if (policyOpt.isPresent()) {
            Policy policy = policyOpt.get();
            PolicyDto policyDto = new PolicyDto();
            policyDto.setId(policy.getId());
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
            policyRepository.flush();
            policyDocumentRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            logger.error("Exception thrown:=============> " + ex.getClass().getName(), ex);
            if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {

                throw new DuplicateContactException("This policy already exists.");
            }
            throw new RuntimeException("An unexpected database error occurred while saving the policy."); // Rethrow if
                                                                                                          // not related
                                                                                                          // to
                                                                                                          // constraint
                                                                                                          // violation
        }

    }

    @Transactional
    public void updatePolicy(HttpServletRequest request, PolicyDto policyDto, Long id) throws Exception {

        Long orgId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "organization-id").toString());

        Optional<Policy> policyOpt = policyRepository.findByIdAndCompanyId(id, orgId);

        if (policyOpt.isEmpty()) {
            throw new Exception("The policy cannot be found");
        }

        Policy policy = policyOpt.get();

        Long userId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "user-id").toString());

        policy.setCertificateNumber(policyDto.getCertificateNumber());
        policy.setPolicyNumber(policyDto.getPolicyNumber());
        policy.setStartDate(policyDto.getStartDate());
        policy.setEndDate(policyDto.getEndDate());
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

    public List<PolicyDto> getPolicyByContact(HttpServletRequest request, Long id) throws Exception {
        // List<Proposal> proposals = proposalRepository.findAll();
        Long orgId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "organization-id").toString());

        List<Policy> policyPage = policyRepository.findByContactIdAndCompanyId(id, orgId);

        List<PolicyDto> policies = policyPage.stream().map(policy -> {
            PolicyDto policyDto = new PolicyDto();
            policyDto.setId(policy.getId());

            policyDto.setStartDate(policy.getStartDate());
            policyDto.setEndDate(policy.getEndDate());
            policyDto.setCompanyId(policy.getCompanyId());
            policyDto.setContactId(policy.getContactId());
            policyDto.setUpdatedBy(policy.getUpdatedBy());

            policyDto.setCertificateNumber(policy.getCertificateNumber());
            policyDto.setPolicyNumber(policy.getPolicyNumber());
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

        return policies;

    }

    @Transactional
    public Map<String, Object> getPolicys(HttpServletRequest request, MultiValueMap<String, String> headers, int page,
            int size) throws Exception {

        // List<Proposal> proposals = policyRepository.findAll();
        Long orgId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "organization-id").toString());
        Pageable pageable = PageRequest.of(page, size, Sort.by("created_at").descending());
        Page<Object[]> policyPage = policyRepository.findLatestPoliciesPerContact(pageable, orgId);

        Set<Long> contacts = new HashSet<Long>();
        Set<Long> updatedByUsers = new HashSet<Long>();

        List<PolicyDto> dtoList = new ArrayList<>();
        for (Object[] row : policyPage.getContent()) {
            PolicyDto policyDto = new PolicyDto();
            logger.info("Mapping policy row: " + Arrays.toString(row));
            policyDto.setId((Long) row[5]);
            policyDto.setProposalId((Long) row[6]);
            policyDto.setCertificateNumber((String) row[9]);
            policyDto.setStartDate(((java.sql.Date) row[1]).toLocalDate());
            policyDto.setEndDate(((java.sql.Date) row[0]).toLocalDate());

            policyDto.setCompanyId((Long) row[2]);
            policyDto.setContactId((Long) row[3]);
            policyDto.setPolicyNumber((String) row[10]);
            policyDto.setUpdatedBy((Long) row[8]);
            policyDto.setCreatedAt(((java.sql.Timestamp) row[4]).toLocalDateTime());
            policyDto.setUpdatedAt(((java.sql.Timestamp) row[7]).toLocalDateTime());

            contacts.add((Long) row[3]);
            updatedByUsers.add((Long) row[8]);
            //[2025-08-16, 2025-08-09, 2, 7, 2025-08-09 11:53:04.449001, 1, 7, 2025-08-09 11:53:04.449001, 2, sdsdsd, sxcsds, 1, 1]
            Long policyCount = ((Number) row[row.length - 1]).longValue(); // last element
            policyDto.setPolicyCount(policyCount);

            dtoList.add(policyDto);
        }

        // start fetch documents
        Set<Long> policyIds = dtoList.stream().map(PolicyDto::getId).collect(Collectors.toSet());
        List<Policy> policiesWithDocs = policyRepository.findAllWithDocumentsByIds(policyIds);

        Map<Long, Set<PolicyDocumentDto>> policyDocsMap = policiesWithDocs.stream()
                .collect(Collectors.toMap(
                        Policy::getId,
                        policy -> policy.getPolicyDocuments().stream().map(document -> {
                            PolicyDocumentDto docDto = new PolicyDocumentDto();
                            docDto.setId(document.getId());
                            docDto.setFolderName(document.getFolderName());
                            docDto.setDocumentName(document.getDocumentName());
                            docDto.setBlobUrl(document.getBlobUrl());
                            docDto.setUpdatedBy(document.getUpdatedBy());
                            docDto.setCreatedAt(document.getCreatedAt());
                            return docDto;
                        }).collect(Collectors.toSet())));

        dtoList.forEach(dto -> {
            dto.setPolicyDocuments(policyDocsMap.getOrDefault(dto.getId(), Collections.emptySet()));
        });
        // end fetch documents

        Page<PolicyDto> policies = new PageImpl<>(dtoList, policyPage.getPageable(),
                policyPage.getTotalElements());

        ContactAddressWrapper contactsData = getAdministrativeAreasData(contacts, headers);
        List<UserDto> updatedByUsersData = getUpdatedByData(updatedByUsers, headers);

        Map<String, Object> response = new HashMap<>();

        response.put("totalElements", policies.getTotalElements());
        response.put("totalPages", policies.getTotalPages());
        response.put("currentPage", policies.getNumber());

        response.put("policies", policies.getContent());
        response.put("contact", contactsData.contacts);
        response.put("updatedBy", updatedByUsersData);
        response.put("administrative_areas", contactsData.localityMapper);

        return response;

    }

    private ContactAddressWrapper getAdministrativeAreasData(Set<Long> ids,
            MultiValueMap<String, String> headers) {
        logger.info("Number of admin area IDs============>: {}", ids.size());

        headers.add("Content-Type", "application/json");

        Map<String, String> flattenedHeaders = new HashMap<>();
        headers.forEach((key, values) -> {
            flattenedHeaders.put(key, String.join(",", values));
        });

        try {
            String contactIds = this.getStringFromList(ids);

            logger.info("Request body as a string ============>: {}", ids);

            ContactAddressWrapper results = contactsDataClient.getContactsByIds(contactIds, flattenedHeaders);

            return results;

        } catch (Exception e) {
            logger.error("Error occurred while fetching administrative areas: {}",
                    e.getMessage());
            ContactAddressWrapper results = new ContactAddressWrapper();
            return results;
        }

    }

    private List<UserDto> getUpdatedByData(Set<Long> ids,
            MultiValueMap<String, String> headers) {
        logger.info("Get update by IDs, size is============>: {}", ids.size());

        headers.add("Content-Type", "application/json");

        Map<String, String> flattenedHeaders = new HashMap<>();
        headers.forEach((key, values) -> {
            flattenedHeaders.put(key, String.join(",", values));
        });

        try {
            String stringIds = this.getStringFromList(ids);

            logger.info("Request body as a string ============>: {}", stringIds);

            List<UserDto> results = usersDataClient.getUsersByIds(stringIds, flattenedHeaders);

            return results;

        } catch (Exception e) {
            logger.error("Error occurred while fetching users: {}",
                    e.getMessage());
            List<UserDto> results = new ArrayList<>();
            return results;
        }

    }

    private String getStringFromList(Set<Long> ids) {
        return ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

}