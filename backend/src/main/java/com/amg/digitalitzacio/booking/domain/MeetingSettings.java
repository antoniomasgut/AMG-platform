package com.amg.digitalitzacio.booking.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "meeting_settings")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public class MeetingSettings {

    @Id
    private UUID tenantId;

    @Column(nullable = false, length = 50)
    private String workingDays = "MON,TUE,WED,THU,FRI";

    @Column(nullable = false)
    private LocalTime startTime = LocalTime.of(9, 0);

    @Column(nullable = false)
    private LocalTime endTime = LocalTime.of(18, 0);

    @Column(nullable = false)
    private int slotDurationMinutes = 45;

    @Column(nullable = false)
    private int bufferMinutes = 15;

    @Column(nullable = false)
    private int minNoticeHours = 24;

    @Column(nullable = false)
    private int maxAdvanceDays = 30;

    @Column(length = 500)
    private String calendarId;

    @LastModifiedDate
    private Instant updatedAt;
}
