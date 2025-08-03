package app.g_agent.policy_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.g_agent.policy_service.commons.Message;
import app.g_agent.policy_service.dto.PolicyDto;
import app.g_agent.policy_service.service.PolicyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/policy")
@Validated
public class PolicyController {

    private static final Logger logger = LoggerFactory.getLogger(PolicyController.class);

    @Autowired
    PolicyService policyService;

    @PostMapping("/create")
    public ResponseEntity<?> createPolicy(HttpServletRequest request, @Valid @RequestBody PolicyDto policyDto) {
        Message message = new Message();

        try {
            policyService.createPolicy(request, policyDto);
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
        message.setName("Success");
        message.setMessage("Policy created successfully");
        return ResponseEntity.ok(message);
    }

    @PutMapping("/update")
    public ResponseEntity<?> updatePolicy(HttpServletRequest request, @RequestBody PolicyDto policyDto,
            @RequestParam Long id) {
        Message message = new Message();

        try {
            policyService.updatePolicy(request, policyDto, id);
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
        message.setName("Success");
        message.setMessage("Policy updated successfully");
        return ResponseEntity.ok(message);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deletePolicy(HttpServletRequest request, @RequestParam Long id) {
        Message message = new Message();

        try {
            policyService.deletePolicy(request, id);
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
        message.setName("Success");
        message.setMessage("Policy deleted successfully");
        return ResponseEntity.ok(message);
    }

    @GetMapping("/get")
    public ResponseEntity<?> getPolicy(HttpServletRequest request, @RequestParam Long id) {
        Message message = new Message();

        try {
            return ResponseEntity.ok(policyService.getPolicyById(request, id));
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
    }


     @GetMapping("/get-policies")
    public ResponseEntity<?> getProposals(
            HttpServletRequest request,
            @RequestHeader MultiValueMap<String, String> headers,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Message message = new Message();

        try {
            return ResponseEntity.ok(policyService.getPolicys(request, headers, page, size));
        } catch (Exception ex) {
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
    }
}