package com.amg.digitalitzacio.finops.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SepaXmlGenerator {
    byte[] generate(String creditorIban, String creditorName, String creditorId,
                    List<SepaPaymentEntry> entries);

    record SepaPaymentEntry(String mandateId, LocalDate mandateSignatureDate,
                            String debtorIban, String debtorName,
                            BigDecimal amount, String remittanceInfo) {}
}
