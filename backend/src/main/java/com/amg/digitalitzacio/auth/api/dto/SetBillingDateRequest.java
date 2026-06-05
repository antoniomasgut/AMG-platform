package com.amg.digitalitzacio.auth.api.dto;

import java.time.LocalDate;

public record SetBillingDateRequest(
        LocalDate billingStartDate
) {}
