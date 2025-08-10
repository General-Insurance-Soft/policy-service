package app.g_agent.policy_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PolicySearchResultDto {
    private Long id;

    @JsonProperty("policy_number")
    private String policyNumber;

    @JsonProperty("contact_name")
    private String contactName;

    @JsonProperty("contact_id_number")
    private String contactIdNumber;

    @JsonProperty("contact_id")
    private Long contactId;

    @JsonProperty("certificate_number")
    private String certificateNumber;

    @JsonProperty("proposal_id")
    private Long proposalId;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactIdNumber() {
        return contactIdNumber;
    }

    public void setContactIdNumber(String contactIdNumber) {
        this.contactIdNumber = contactIdNumber;
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

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

}
