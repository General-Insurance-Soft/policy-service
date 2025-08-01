package app.g_agent.policy_service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PolicyDto {

    private Long id;

    @NotBlank(message = "policy_number is required")
    @JsonProperty("policy_number")
    String policyNumber;

    @NotBlank(message = "certificate_number is required")
    @JsonProperty("certificate_number")
    String certificateNumber;

    @NotNull(message = "proposal_id is required")
    @JsonProperty("proposal_id")
    Long proposalId;

    @NotNull(message = "insurance_company_id is required")
    @JsonProperty("insurance_company_id")
    private Long insuranceCompanyId;

    @NotNull(message = "policy_type_id is required")
    @JsonProperty("policy_type_id")
    private Long policyTypeId;

    @JsonProperty("start_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonProperty("end_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("updated_by")
    private Long updatedBy;

    @JsonProperty("company_id")
    private Long companyId;

    @NotNull(message = "Contact ID is required")
    @JsonProperty("contact_id")
    private Long contactId;

    @JsonProperty("policy_documents")
    private Set<PolicyDocumentDto> policyDocuments;

    @AssertTrue(message = "start date must be before end date")
	public boolean isValidStartAndEndDate() {
		if (startDate == null || endDate == null) {
			return true; // Let @NotNull handle null checks if needed
		}
		return startDate.isBefore(endDate);
	}

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getInsuranceCompanyId() {
        return insuranceCompanyId;
    }

    public void setInsuranceCompanyId(Long insuranceCompanyId) {
        this.insuranceCompanyId = insuranceCompanyId;
    }

    public Long getPolicyTypeId() {
        return policyTypeId;
    }

    public void setPolicyTypeId(Long policyTypeId) {
        this.policyTypeId = policyTypeId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

    public Set<PolicyDocumentDto> getPolicyDocuments() {
        return policyDocuments;
    }

    public void setPolicyDocuments(Set<PolicyDocumentDto> policyDocuments) {
        this.policyDocuments = policyDocuments;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public void setCertificateNumber(String certificateNumber) {
        this.certificateNumber = certificateNumber;
    }

    public Long getProposalId() {
        return proposalId;
    }

    public void setProposalId(Long proposalId) {
        this.proposalId = proposalId;
    }
}