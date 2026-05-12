package com.amg.digitalitzacio.vault.api.dto;

import com.amg.digitalitzacio.vault.domain.ServiceStatus;
import lombok.Data;

@Data
public class ChangeServiceStatusRequest {
    private ServiceStatus status;
}
