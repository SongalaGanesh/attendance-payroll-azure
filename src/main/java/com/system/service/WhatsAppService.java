package com.system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.entity.Attendance;
import com.system.entity.ConversationState;
import com.system.entity.Employee;
import com.system.entity.WhatsAppMessage;
import com.system.repository.AttendanceRepository;
import com.system.repository.ConversationStateRepository;
import com.system.repository.EmployeeRepository;
import com.system.repository.WhatsAppMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class WhatsAppService {
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppService.class);

    @Value("${whatsapp.api.url}")
    private String apiUrl;

    @Value("${whatsapp.api.phone-number-id}")
    private String phoneNumberId;

    @Value("${whatsapp.api.access-token}")
    private String accessToken;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ConversationStateRepository conversationStateRepository;

    @Autowired
    private WhatsAppMessageRepository whatsAppMessageRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private LeaveService leaveService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Send message via Meta WhatsApp Business Cloud API.
     */
    public boolean sendWhatsAppMessage(String to, String text) {
        logger.info("Sending WhatsApp message to: {}, content: {}", to, text);
        
        // Log outgoing message to Database
        try {
            Optional<Employee> emp = employeeRepository.findByWhatsappNumber(to);
            WhatsAppMessage logMsg = WhatsAppMessage.builder()
                    .employee(emp.orElse(null))
                    .direction("OUTGOING")
                    .messageType("text")
                    .messageBody(text)
                    .waMessageId("OUT_" + UUID.randomUUID())
                    .build();
            whatsAppMessageRepository.save(logMsg);
        } catch (Exception e) {
            logger.error("Failed to log outgoing whatsapp message: {}", e.getMessage());
        }

        if ("YOUR_ACCESS_TOKEN".equals(accessToken) || accessToken.isEmpty()) {
            logger.warn("WhatsApp integration token placeholder detected. Skipping actual HTTP request.");
            return true;
        }

        try {
            String url = String.format("%s/%s/messages", apiUrl, phoneNumberId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, Object> body = new HashMap<>();
            body.put("messaging_product", "whatsapp");
            body.put("recipient_type", "individual");
            body.put("to", to);
            body.put("type", "text");

            Map<String, String> textObj = new HashMap<>();
            textObj.put("body", text);
            body.put("text", textObj);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("WhatsApp message sent successfully. Response: {}", response.getBody());
                return true;
            } else {
                logger.error("Failed to send WhatsApp message. Status: {}, Response: {}", response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error making HTTP call to WhatsApp Cloud API: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Process incoming message webhook.
     */
    @Transactional
    public void processIncomingMessage(String whatsappNumber, String msgId, String type, String bodyText, BigDecimal lat, BigDecimal lng) {
        logger.info("Processing webhook message: from={}, type={}, body={}, lat={}, lng={}", whatsappNumber, type, bodyText, lat, lng);
        
        Optional<Employee> employeeOpt = employeeRepository.findByWhatsappNumber(whatsappNumber);
        if (employeeOpt.isEmpty()) {
            logger.warn("Received message from unregistered number: {}", whatsappNumber);
            // Optional: send a message back informing them to register
            sendWhatsAppMessage(whatsappNumber, "Welcome to Employee System. Your WhatsApp number is not registered. Please contact HR.");
            return;
        }

        Employee employee = employeeOpt.get();

        // Check if message is already processed to prevent duplicate processing from webhook retries
        if (whatsAppMessageRepository.existsByWaMessageId(msgId)) {
            logger.warn("Message with ID: {} already processed. Skipping.", msgId);
            return;
        }

        // Log incoming message to Database
        WhatsAppMessage logMsg = WhatsAppMessage.builder()
                .employee(employee)
                .direction("INCOMING")
                .messageType(type)
                .messageBody("location".equalsIgnoreCase(type) ? "Location: " + lat + "," + lng : bodyText)
                .waMessageId(msgId)
                .build();
        whatsAppMessageRepository.save(logMsg);

        Optional<ConversationState> stateOpt = conversationStateRepository.findByEmployeeId(employee.getId());

        if (stateOpt.isEmpty() || "IDLE".equals(stateOpt.get().getCurrentState())) {
            // Idle State: Parse Commands
            if (!"text".equalsIgnoreCase(type)) {
                sendWhatsAppMessage(whatsappNumber, "Please send a command text to begin. Available commands: ATTENDANCE, CHECKOUT, LEAVE, STATUS, HELP.");
                return;
            }
            handleInitialCommand(employee, bodyText.trim().toUpperCase());
        } else {
            // Active Conversation Flow State
            handleStateFlow(employee, stateOpt.get(), type, bodyText, lat, lng);
        }
    }

    private void handleInitialCommand(Employee employee, String command) {
        String whatsapp = employee.getWhatsappNumber();
        switch (command) {
            case "ATTENDANCE":
                conversationStateRepository.save(ConversationState.builder()
                        .employee(employee)
                        .currentState("WAITING_CHECKIN_LOCATION")
                        .build());
                sendWhatsAppMessage(whatsapp, "Please share your location to mark ATTENDANCE.");
                break;

            case "CHECKOUT":
                conversationStateRepository.save(ConversationState.builder()
                        .employee(employee)
                        .currentState("WAITING_CHECKOUT_LOCATION")
                        .build());
                sendWhatsAppMessage(whatsapp, "Please share your location to CHECKOUT.");
                break;

            case "LEAVE":
                conversationStateRepository.save(ConversationState.builder()
                        .employee(employee)
                        .currentState("WAITING_LEAVE_TYPE")
                        .contextData("{}")
                        .build());
                sendWhatsAppMessage(whatsapp, "Enter Leave Type:\n(CASUAL / SICK / EARNED)");
                break;

            case "STATUS":
                LocalDate today = LocalDate.now();
                Optional<Attendance> att = attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), today);
                String attStr = att.map(attendance -> "Today's Attendance: " + attendance.getAttendanceStatus() +
                        " (In: " + attendance.getCheckInTime() +
                        ", Out: " + (attendance.getCheckOutTime() != null ? attendance.getCheckOutTime() : "N/A") + ")")
                        .orElse("Today's Attendance: Not marked yet.");
                sendWhatsAppMessage(whatsapp, String.format("Hello %s.\n%s\nDesignation: %s\nDepartment: %s",
                        employee.getFirstName(), attStr, employee.getDesignation().getTitle(), employee.getDepartment().getName()));
                break;

            case "HELP":
            default:
                sendWhatsAppMessage(whatsapp, "Available commands:\n" +
                        "1. ATTENDANCE : Start check-in process\n" +
                        "2. CHECKOUT : Start check-out process\n" +
                        "3. LEAVE : Apply for leave request\n" +
                        "4. STATUS : Check today's status\n" +
                        "5. HELP : Show this commands list");
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private void handleStateFlow(Employee employee, ConversationState state, String type, String bodyText, BigDecimal lat, BigDecimal lng) {
        String whatsapp = employee.getWhatsappNumber();
        String currentState = state.getCurrentState();

        try {
            switch (currentState) {
                case "WAITING_CHECKIN_LOCATION":
                    if (!"location".equalsIgnoreCase(type)) {
                        sendWhatsAppMessage(whatsapp, "Invalid input. Please share your actual location pin on WhatsApp.");
                        return;
                    }
                    try {
                        Attendance att = attendanceService.checkIn(employee, lat, lng);
                        sendWhatsAppMessage(whatsapp, "Check-in successful. Status: " + att.getAttendanceStatus() + 
                                " (Distance: " + att.getDistanceMeters() + "m)");
                    } catch (Exception e) {
                        sendWhatsAppMessage(whatsapp, "Check-in failed: " + e.getMessage());
                    }
                    conversationStateRepository.delete(state);
                    break;

                case "WAITING_CHECKOUT_LOCATION":
                    if (!"location".equalsIgnoreCase(type)) {
                        sendWhatsAppMessage(whatsapp, "Invalid input. Please share your actual location pin on WhatsApp.");
                        return;
                    }
                    try {
                        Attendance att = attendanceService.checkOut(employee, lat, lng);
                        sendWhatsAppMessage(whatsapp, "Checkout successful. Status: " + att.getAttendanceStatus() + 
                                " (Working Hours: " + att.getTotalWorkingHours() + " hours)");
                    } catch (Exception e) {
                        sendWhatsAppMessage(whatsapp, "Checkout failed: " + e.getMessage());
                    }
                    conversationStateRepository.delete(state);
                    break;

                case "WAITING_LEAVE_TYPE":
                    String ltype = bodyText.trim().toUpperCase();
                    if (!ltype.equals("CASUAL") && !ltype.equals("SICK") && !ltype.equals("EARNED")) {
                        sendWhatsAppMessage(whatsapp, "Invalid Leave Type. Please enter CASUAL, SICK, or EARNED:");
                        return;
                    }
                    Map<String, String> context = objectMapper.readValue(state.getContextData(), Map.class);
                    context.put("leaveType", ltype);
                    state.setContextData(objectMapper.writeValueAsString(context));
                    state.setCurrentState("WAITING_FROM_DATE");
                    conversationStateRepository.save(state);
                    sendWhatsAppMessage(whatsapp, "Enter From Date (YYYY-MM-DD):");
                    break;

                case "WAITING_FROM_DATE":
                    String fromStr = bodyText.trim();
                    try {
                        LocalDate fromDate = LocalDate.parse(fromStr);
                        if (fromDate.isBefore(LocalDate.now())) {
                            sendWhatsAppMessage(whatsapp, "From date cannot be in the past. Enter From Date (YYYY-MM-DD):");
                            return;
                        }
                        Map<String, String> ctx = objectMapper.readValue(state.getContextData(), Map.class);
                        ctx.put("fromDate", fromStr);
                        state.setContextData(objectMapper.writeValueAsString(ctx));
                        state.setCurrentState("WAITING_TO_DATE");
                        conversationStateRepository.save(state);
                        sendWhatsAppMessage(whatsapp, "Enter To Date (YYYY-MM-DD):");
                    } catch (DateTimeParseException e) {
                        sendWhatsAppMessage(whatsapp, "Invalid date format. Enter From Date using YYYY-MM-DD format:");
                    }
                    break;

                case "WAITING_TO_DATE":
                    String toStr = bodyText.trim();
                    try {
                        LocalDate toDate = LocalDate.parse(toStr);
                        Map<String, String> ctx = objectMapper.readValue(state.getContextData(), Map.class);
                        LocalDate fromDate = LocalDate.parse(ctx.get("fromDate"));
                        if (toDate.isBefore(fromDate)) {
                            sendWhatsAppMessage(whatsapp, "To date cannot be before From date. Enter To Date (YYYY-MM-DD):");
                            return;
                        }
                        ctx.put("toDate", toStr);
                        state.setContextData(objectMapper.writeValueAsString(ctx));
                        state.setCurrentState("WAITING_REASON");
                        conversationStateRepository.save(state);
                        sendWhatsAppMessage(whatsapp, "Enter Leave Reason:");
                    } catch (DateTimeParseException e) {
                        sendWhatsAppMessage(whatsapp, "Invalid date format. Enter To Date using YYYY-MM-DD format:");
                    }
                    break;

                case "WAITING_REASON":
                    String reason = bodyText.trim();
                    if (reason.isEmpty()) {
                        sendWhatsAppMessage(whatsapp, "Reason cannot be empty. Please enter Leave Reason:");
                        return;
                    }
                    Map<String, String> finalCtx = objectMapper.readValue(state.getContextData(), Map.class);
                    String finalType = finalCtx.get("leaveType");
                    LocalDate finalFrom = LocalDate.parse(finalCtx.get("fromDate"));
                    LocalDate finalTo = LocalDate.parse(finalCtx.get("toDate"));

                    try {
                        leaveService.applyLeave(employee, finalType, finalFrom, finalTo, reason);
                        sendWhatsAppMessage(whatsapp, String.format("Leave application submitted successfully!\n" +
                                "Type: %s\nPeriod: %s to %s\nStatus: PENDING Approval", finalType, finalFrom, finalTo));
                    } catch (Exception e) {
                        sendWhatsAppMessage(whatsapp, "Failed to apply leave: " + e.getMessage());
                    }
                    conversationStateRepository.delete(state);
                    break;

                default:
                    conversationStateRepository.delete(state);
                    sendWhatsAppMessage(whatsapp, "Resetting conversation. Please send HELP to list commands.");
                    break;
            }
        } catch (Exception e) {
            logger.error("Error processing state flow: {}", e.getMessage(), e);
            conversationStateRepository.delete(state);
            sendWhatsAppMessage(whatsapp, "An error occurred. Resetting conversation. Please try again.");
        }
    }
}
