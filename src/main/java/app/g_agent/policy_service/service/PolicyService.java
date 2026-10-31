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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import app.g_agent.policy_service.dto.ContactAddressWrapper;
import app.g_agent.policy_service.dto.ContactDto;
import app.g_agent.policy_service.dto.PolicyDocumentDto;
import app.g_agent.policy_service.dto.PolicyDto;
import app.g_agent.policy_service.dto.PolicySearchResultDto;
import app.g_agent.policy_service.dto.UserDto;
import app.g_agent.policy_service.model.Policy;
import app.g_agent.policy_service.model.PolicyDocument;
import app.g_agent.policy_service.repository.PolicyDocumentRepository;
import app.g_agent.policy_service.repository.PolicyRepository;
import app.g_agent.policy_service.repository.PolicySpecification;
import app.g_agent.policy_service.system.DuplicateContactException;
import jakarta.servlet.http.HttpServletRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
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

    @Value("${backblaze.s3.endpoint-url}")
    private String ENDPOINT_URL;
    @Value("${backblaze.s3.bucket-name}")
    private String bucketName;

    @Autowired
    B2ClientFactory b2ClientFactory;

    public PolicyService(PolicyRepository policyRepository, PolicyDocumentRepository policyDocumentRepository,
            JwtService jwtService) {
        this.policyRepository = policyRepository;
        this.policyDocumentRepository = policyDocumentRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public void deletePolicy(HttpServletRequest request, Long id) throws Exception {
        Long orgId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "organization-id").toString());

        Optional<Policy> policyOpt = policyRepository.findByIdAndCompanyId(id, orgId);

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

            policyDto.setStartDate(policy.getStartDate());
            policyDto.setEndDate(policy.getEndDate());
            policyDto.setCompanyId(policy.getCompanyId());
            policyDto.setContactId(policy.getContactId());
            policyDto.setProposalId(policy.getProposalId());
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

    private Policy preparePolicy(HttpServletRequest request, PolicyDto policyDto) throws Exception {
        Policy policy = new Policy();

        Long userId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "user-id").toString());
        Long orgId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "organization-id").toString());
        logger.info("user ID: ==============================>" + userId);

        policy.setCertificateNumber(policyDto.getCertificateNumber());
        policy.setPolicyNumber(policyDto.getPolicyNumber());
        policy.setStartDate(policyDto.getStartDate());
        policy.setEndDate(policyDto.getEndDate());
        policy.setCompanyId(orgId);
        policy.setContactId(policyDto.getContactId());
        policy.setUpdatedBy(Long.valueOf(userId));
        policy.setProposalId(policyDto.getProposalId());

        return policy;
    }

    private Set<PolicyDocumentDto> preparePolicyDocuments(List<Map<String, String>> policyDocs, Policy policy,
            Long userId) {
        Set<PolicyDocumentDto> policyDocuments = new HashSet<>();

        for (Map<String, String> fileMeta : (List<Map<String, String>>) policyDocs) {
            PolicyDocumentDto doc = new PolicyDocumentDto();
            doc.setDocumentName(fileMeta.get("name"));
            doc.setBlobUrl((String) fileMeta.get("blob_url"));
            doc.setVersionId((String) fileMeta.get("version_id"));
            doc.setFileExtension(fileMeta.get("extension"));
            policyDocuments.add(doc);
        }
        logger.debug("Alter policy documents DTO ======>: " + policyDocuments.toString());
        return policyDocuments;
    }

    private Set<PolicyDocument> preparePolicyDocumentObject(PolicyDto policyDto, Policy policy,
            Long userId) {
        Set<PolicyDocument> policyDocuments = new HashSet<>();

        if (policyDto.getPolicyDocuments() != null) {
            policyDto.getPolicyDocuments().forEach(documentDto -> {

                // Skip empty documents
                if (documentDto.getBlobUrl() == null ||
                        documentDto.getDocumentName() == null ||
                        documentDto.getDocumentType() == null) {
                    logger.debug("Skipping empty document DTO");
                    return;
                }
                PolicyDocument document = new PolicyDocument();
                document.setFolderName(documentDto.getFolderName());
                document.setDocumentName(documentDto.getDocumentName());
                document.setBlobUrl(documentDto.getBlobUrl());
                document.setDocumentType(documentDto.getDocumentType());
                document.setVersionId(documentDto.getVersionId());
                document.setUpdatedBy(Long.valueOf(userId));
                document.setPolicy(policy); // Set the policy reference
                policyDocuments.add(document);
            });
            policy.setPolicyDocuments(policyDocuments);
        }

        return policyDocuments;
    }

    @Transactional
    public void createPolicy(HttpServletRequest request, PolicyDto policyDto,
            List<MultipartFile> files) throws Exception {

        Policy policy = this.preparePolicy(request, policyDto);

        Long userId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "user-id").toString());
        Long orgId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "organization-id").toString());

        List<Map<String, String>> policyDocs = this.uploadFilesToS3(files, orgId);
        Set<PolicyDocumentDto> policyDocumentDtos = this.preparePolicyDocuments(policyDocs, policy, userId);
        policyDto.setPolicyDocuments(policyDocumentDtos);

        if (policyDto.getPolicyDocuments() != null) {
            logger.info("policyDto.getPolicyDocuments() not null");
            Set<PolicyDocument> policyDocuments = this.preparePolicyDocumentObject(policyDto, policy, userId);
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
            throw new RuntimeException("An unexpected database error occurred while saving the policy.");
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
        // List<Policy> policys = policyRepository.findAll();
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
            policyDto.setProposalId(policy.getProposalId());
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

        // List<Policy> policys = policyRepository.findAll();
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
            // [2025-08-16, 2025-08-09, 2, 7, 2025-08-09 11:53:04.449001, 1, 7, 2025-08-09
            // 11:53:04.449001, 2, sdsdsd, sxcsds, 1, 1]
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

    public List<PolicySearchResultDto> searchPolicies(HttpServletRequest request,
            MultiValueMap<String, String> headers, String keyword) throws Exception {

        int orgId = (int) jwtService.getTokenValue(jwtService.getJWT(request), "organization-id");
        logger.info("The org id ==============> " + orgId);
        List<Policy> policies = policyRepository.findAll(PolicySpecification.matchesKeyword(keyword, orgId));

        if (policies.size() != 0) {
            return policies.stream().map(
                    policy -> {
                        PolicySearchResultDto policySearchResultDto = new PolicySearchResultDto();
                        policySearchResultDto.setId(policy.getId());
                        policySearchResultDto.setPolicyNumber(policy.getPolicyNumber());
                        policySearchResultDto.setContactName("");
                        policySearchResultDto.setContactIdNumber("");
                        policySearchResultDto.setCertificateNumber(policy.getCertificateNumber());
                        policySearchResultDto.setContactId(policy.getContactId());

                        return policySearchResultDto;
                    }).collect(Collectors.toList());
        } else {
            headers.add("Content-Type", "application/json");

            Map<String, String> flattenedHeaders = new HashMap<>();
            headers.forEach((key, values) -> {
                flattenedHeaders.put(key, String.join(",", values));
            });

            try {

                List<ContactDto> results = contactsDataClient.getContactsByKeyword(keyword, flattenedHeaders);

                List<Long> contactIds = results.stream()
                        .map(ContactDto::getId)
                        .collect(Collectors.toList());
                logger.info("Got contactIds of size ==============> " + contactIds.size());
                // find policy by contact ids
                List<PolicySearchResultDto> policyResults = policyRepository
                        .findByContactIdInAndCompanyId(contactIds, orgId)
                        .stream()
                        .map(policy -> {
                            PolicySearchResultDto policySearchResultDto = new PolicySearchResultDto();
                            policySearchResultDto.setId(policy.getId());
                            policySearchResultDto.setPolicyNumber(policy.getPolicyNumber());
                            policySearchResultDto.setContactName("");
                            policySearchResultDto.setContactIdNumber("");
                            policySearchResultDto.setCertificateNumber(policy.getCertificateNumber());
                            policySearchResultDto.setContactId(policy.getContactId());

                            return policySearchResultDto;
                        }).collect(Collectors.toList());

                return policyResults;

            } catch (Exception e) {
                logger.error("Error occurred while fetching users: {}",
                        e.getMessage());
                List<PolicySearchResultDto> results = new ArrayList<>();
                return results;
            }
        }

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

   private List<Map<String, String>> uploadFilesToS3(List<MultipartFile> filesList, Long orgId) {

        if (orgId == null) {
            logger.warn("Organization ID is null for uploading proposal and user files.");
            return new ArrayList<>();
        }

        Matcher matcher = Pattern.compile("https://s3\\.([a-z0-9-]+)\\.backblazeb2\\.com")
                .matcher(ENDPOINT_URL.trim());
        String region = matcher.find() ? matcher.group(1) : null;
        if (region == null) {
            logger.error("Can't find a region in the endpoint URL: " + ENDPOINT_URL);
            return new ArrayList<>();
        }

        List<Map<String, String>> filesMetaList = new ArrayList<>();
        try {
            S3Client b2 = b2ClientFactory.createClient();

            for (MultipartFile file : filesList) {

                String contentType = file.getContentType() != null
                        ? file.getContentType()
                        : "";

                // Accept files with no content type, but reject empty "blob" files
                if (("blob".equalsIgnoreCase(file.getOriginalFilename())
                        || file.getOriginalFilename().isEmpty())
                        && file.getSize() == 0) {
                    logger.info("Skipping empty blob file upload.");
                    continue;
                }

                if (!contentType.isEmpty()
                        && !contentType.matches("image/.*|application/pdf")) {
                    throw new IllegalArgumentException(
                            "Invalid file type: " + file.getOriginalFilename());
                }

                byte[] bytes = file.getBytes();

                String key;

                String input = file.getOriginalFilename();
                String result = input
                        .replaceAll("name:", "")
                        .replaceAll("-type:[^-]+", "");

                key = orgId + "/policy-service/" + result;

                PutObjectRequest objRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(contentType)
                        .build();

                PutObjectResponse putObjectResponse = b2.putObject(
                        objRequest,
                        software.amazon.awssdk.core.sync.RequestBody.fromBytes(bytes));

                logger.info("Response from S3 for file {}: {}", file.getOriginalFilename(),
                        putObjectResponse.toString());

                String fileUrl = String.format(
                        "%s/%s/%s",
                        ENDPOINT_URL,
                        bucketName,
                        key);

                logger.debug("process policy files ======>: ");
                Map<String, String> filesMeta = new HashMap<>();

                Pattern fileNamePattern = Pattern.compile("name:([^\\-]+)");
                Pattern fileTypePattern = Pattern.compile("-type:([^\\-]+)");

                Matcher fileName = fileNamePattern.matcher(input);
                Matcher typeVal = fileTypePattern.matcher(input);

                logger.debug("process regex and create metadata ======>: ");
                if (fileName.find() && typeVal.find()) {
                    String name = fileName.group(1);
                    String typeNumber = typeVal.group(1);
                    filesMeta.put("name", name);
                    filesMeta.put("type", typeNumber);
                    filesMeta.put("version_id", (String) putObjectResponse.versionId());
                }

                filesMeta.put("blob_url", fileUrl);

                logger.debug("Add objects to map for processing ======>: ");

                filesMetaList.add(filesMeta);

                logger.debug("objects added to map for processing ======>: ");

            }
        } catch (Exception e) {
            logger.error("Error uploading file to S3: " + e.getMessage());
            throw new RuntimeException(e);
        }
        return filesMetaList;
    }

}