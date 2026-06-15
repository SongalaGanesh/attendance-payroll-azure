package com.system.controller;

import com.system.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/whatsapp/webhook")
public class WhatsAppWebhookController {
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    @Autowired
    private WebhookService webhookService;

    /**
     * Meta Webhook verification endpoint (GET).
     */
    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        
        logger.info("WhatsApp webhook verification request: mode={}, token={}, challenge={}", mode, token, challenge);
        
        if (webhookService.verifySubscription(mode, token)) {
            logger.info("WhatsApp webhook verified successfully.");
            return ResponseEntity.ok(challenge);
        } else {
            logger.warn("WhatsApp webhook verification failed. Unauthorized token.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification token mismatch.");
        }
    }

    /**
     * Meta Webhook payload receiver endpoint (POST).
     */
    @PostMapping
    public ResponseEntity<Void> receiveWebhook(@RequestBody Map<String, Object> payload) {
        logger.info("Received WhatsApp webhook payload: {}", payload);
        webhookService.processWebhookPayload(payload);
        return ResponseEntity.ok().build();
    }
}
