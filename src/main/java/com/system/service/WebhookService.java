package com.system.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class WebhookService {
    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);

    @Value("${whatsapp.webhook.verify-token}")
    private String verifyToken;

    @Autowired
    private WhatsAppService whatsAppService;

    /**
     * Verify Meta Webhook subscription.
     */
    public boolean verifySubscription(String mode, String token) {
        return "subscribe".equals(mode) && verifyToken.equals(token);
    }

    /**
     * Process JSON webhook body from Meta.
     */
    @SuppressWarnings("unchecked")
    public void processWebhookPayload(Map<String, Object> payload) {
        try {
            if (payload == null || !payload.containsKey("entry")) {
                return;
            }

            List<Map<String, Object>> entries = (List<Map<String, Object>>) payload.get("entry");
            for (Map<String, Object> entry : entries) {
                if (!entry.containsKey("changes")) {
                    continue;
                }

                List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get("changes");
                for (Map<String, Object> change : changes) {
                    Map<String, Object> value = (Map<String, Object>) change.get("value");
                    if (value == null || !value.containsKey("messages")) {
                        continue;
                    }

                    List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");
                    for (Map<String, Object> msg : messages) {
                        String from = (String) msg.get("from"); // WhatsApp ID (phone number)
                        String msgId = (String) msg.get("id");
                        String type = (String) msg.get("type");

                        // Format numbers: Meta numbers usually don't have "+" prefix
                        // Ensure we clean the whatsapp number mapping
                        if (from != null && !from.startsWith("+")) {
                            // We can store numbers without "+" in db or match cleanly
                            // Let's standardise formatting
                        }

                        String textBody = null;
                        BigDecimal lat = null;
                        BigDecimal lng = null;

                        if ("text".equalsIgnoreCase(type) && msg.containsKey("text")) {
                            Map<String, Object> textObj = (Map<String, Object>) msg.get("text");
                            textBody = (String) textObj.get("body");
                        } else if ("location".equalsIgnoreCase(type) && msg.containsKey("location")) {
                            Map<String, Object> locObj = (Map<String, Object>) msg.get("location");
                            Object latitude = locObj.get("latitude");
                            Object longitude = locObj.get("longitude");

                            if (latitude instanceof Number) {
                                lat = BigDecimal.valueOf(((Number) latitude).doubleValue());
                            } else if (latitude instanceof String) {
                                lat = new BigDecimal((String) latitude);
                            }

                            if (longitude instanceof Number) {
                                lng = BigDecimal.valueOf(((Number) longitude).doubleValue());
                            } else if (longitude instanceof String) {
                                lng = new BigDecimal((String) longitude);
                            }
                        }

                        if (from != null && msgId != null && type != null) {
                            whatsAppService.processIncomingMessage(from, msgId, type, textBody, lat, lng);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to parse Meta webhook payload: {}", e.getMessage(), e);
        }
    }
}
