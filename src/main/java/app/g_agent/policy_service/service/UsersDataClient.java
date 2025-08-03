package app.g_agent.policy_service.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
//import AdministrativeAreaDto
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import app.g_agent.policy_service.dto.UserDto;

@FeignClient(name = "user-service")
public interface UsersDataClient {

    @GetMapping("/user-service/api/v1/user/get-contacts-by-ids")
    List<UserDto> getUsersByIds(@RequestParam String ids,
            @RequestHeader Map<String, String> headers);
}
