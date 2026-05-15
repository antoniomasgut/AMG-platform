package com.amg.digitalitzacio.engine.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "template_sections")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TemplateSection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false, length = 30)
    private BlockType blockType;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "props_schema", nullable = false, columnDefinition = "TEXT")
    private String propsSchema;

    @Column(name = "default_props", columnDefinition = "TEXT")
    private String defaultProps;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;
}
