package app.g_agent.policy_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.g_agent.policy_service.commons.Message;
import app.g_agent.policy_service.dto.PolicyDocumentDto;
import app.g_agent.policy_service.service.PolicyDocumentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/policy-document")
@Validated
public class PolicyDocumentController {

    private static final Logger logger = LoggerFactory.getLogger(PolicyDocumentController.class);

    @Autowired
    PolicyDocumentService policyDocumentService;

    @PostMapping("/create")
    public ResponseEntity<?> createPolicyDocument(HttpServletRequest request,
            @Valid @RequestBody PolicyDocumentDto policyDocumentDto) {
        Message message = new Message();

        try {
            policyDocumentService.createPolicyDocument(request, policyDocumentDto);
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
        message.setName("Success");
        message.setMessage("Policy document created successfully");
        return ResponseEntity.ok(message);
    }

    @PutMapping("/update")
    public ResponseEntity<?> updatePolicyDocument(HttpServletRequest request,
            @RequestBody PolicyDocumentDto policyDocumentDto,
            @RequestParam Long id) {
        Message message = new Message();

        try {
            policyDocumentService.updatePolicyDocument(request, policyDocumentDto, id);
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
        message.setName("Success");
        message.setMessage("Policy document updated successfully");
        return ResponseEntity.ok(message);
    }

    @DeleteMapping("/delete-file")
    public ResponseEntity<?> deletePolicyDocument(HttpServletRequest request, @RequestParam Long id) {
        Message message = new Message();

        try {
            policyDocumentService.deletePolicyDocument(request, id);
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
        message.setName("Success");
        message.setMessage("Policy file deleted successfully");
        return ResponseEntity.ok(message);
    }

    @GetMapping("/get")
    public ResponseEntity<?> getPolicyDocument(HttpServletRequest request, @RequestParam Long id) {
        Message message = new Message();

        try {
            return ResponseEntity.ok(policyDocumentService.getPolicyDocument(request, id));
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
    }

    @GetMapping("/get-policy-documents")
    public ResponseEntity<?> getPolicyDocuments(HttpServletRequest request) {
        Message message = new Message();

        try {
            return ResponseEntity.ok(policyDocumentService.getPolicyDocuments(request));
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
    }
}