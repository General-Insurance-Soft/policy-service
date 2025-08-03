package app.g_agent.policy_service.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ContactAddressWrapper {
    @JsonProperty("locality_mapper")
    public List<LocalityDto> localityMapper;
    public List<ContactDto> contacts;
}
