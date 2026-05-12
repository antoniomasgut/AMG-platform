package com.amg.digitalitzacio.billing.api.dto;

import lombok.Data;

@Data
public class UpdateBudgetRequest {
    private String notes;
    private String clientNotes;
}
