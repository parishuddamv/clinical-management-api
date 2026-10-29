package com.clinicos.common.dto;

import com.clinicos.common.entity.AuditEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEventDto {
    private Long id;
    private String actor;
    private String action;
    private String resourceType;
    private String resourceId;
    private String detail;
    private Integer status;
    private String ipAddress;
    private LocalDateTime occurredAt;

    public static AuditEventDto from(AuditEvent e) {
        return AuditEventDto.builder().id(e.getId()).actor(e.getActor()).action(e.getAction()).resourceType(e.getResourceType())
                .resourceId(e.getResourceId()).detail(e.getDetail()).status(e.getStatus()).ipAddress(e.getIpAddress())
                .occurredAt(e.getOccurredAt()).build();
    }
}
