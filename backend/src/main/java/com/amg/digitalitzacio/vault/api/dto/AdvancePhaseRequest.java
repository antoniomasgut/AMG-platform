package com.amg.digitalitzacio.vault.api.dto;

import com.amg.digitalitzacio.vault.domain.ImplementationStatus;
import lombok.Data;

@Data
public class AdvancePhaseRequest {
    private ImplementationStatus status;
}
