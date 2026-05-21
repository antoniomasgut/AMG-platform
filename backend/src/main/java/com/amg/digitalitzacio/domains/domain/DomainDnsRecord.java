package com.amg.digitalitzacio.domains.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

// Registre DNS associat a un domini gestionat
@Entity
@Table(name = "domain_dns_records")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DomainDnsRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Referència al domini pare
    @Column(nullable = false)
    private UUID domainId;

    // Tipus de registre DNS: A, CNAME, MX, TXT
    @Column(length = 10, nullable = false)
    private String type;

    // Nom del registre (p.ex. "@", "www", "mail")
    @Column(length = 253, nullable = false)
    private String name;

    // Valor del registre (IP, hostname, text)
    @Column(length = 512, nullable = false)
    private String value;

    // Temps de vida en segons (TTL)
    @Builder.Default
    private int ttl = 3600;

    // Prioritat (només per registres MX)
    private Integer priority;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;
}
