package app.g_agent.policy_service.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
//import AdministrativeAreaDto
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import app.g_agent.policy_service.dto.ContactAddressWrapper;
import app.g_agent.policy_service.dto.ContactDto;
import app.g_agent.policy_service.dto.UserDto;

@FeignClient(name = "contact-service")
public interface ContactsDataClient {

    @GetMapping("/contact-service/api/v1/contact/get-contacts-by-ids")
    ContactAddressWrapper getContactsByIds(@RequestParam String contactIds,
            @RequestHeader Map<String, String> headers);

    @PostMapping("/contact-service/api/v1/contact/search")
    List<ContactDto> getContactsByKeyword(@RequestParam String keyword,
            @RequestHeader Map<String, String> headers);
}
