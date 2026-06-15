package com.system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(nullable = false, length = 10)
    private String direction; // INCOMING or OUTGOING

    @Column(name = "message_type", nullable = false, length = 20)
    private String messageType; // text or location

    @Column(name = "message_body", columnDefinition = "TEXT")
    private String messageBody;

    @Column(name = "wa_message_id", nullable = false, unique = true, length = 255)
    private String waMessageId;

    @Column(name = "message_timestamp", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime messageTimestamp = LocalDateTime.now();
}
