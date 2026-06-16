package com.amg.digitalitzacio.booking.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking_tokens")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public class BookingToken {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column
    private UUID leadId;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(nullable = false, length = 150)
    private String leadName;

    @Column(length = 150)
    private String leadEmail;

    @Column
    private UUID sourceDocumentId;

    @Column(length = 30)
    private String recipientPhone;

    @Column(length = 255)
    private String recipientName;

    @Column(length = 100)
    private String bookingLabel;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean confirmed = false;

    private Instant meetingAt;

    @Column(length = 500)
    private String meetLink;

    @Column(length = 255)
    private String googleEventId;

    @CreatedDate
    private Instant createdAt;
}
