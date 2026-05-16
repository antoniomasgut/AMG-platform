package com.amg.digitalitzacio.finops.api.dto;

import java.time.LocalDate;

public record SepaMandateRequest(String iban, String bic, String accountHolderName, LocalDate signedAt) {}
