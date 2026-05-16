package com.amg.digitalitzacio.finops.application;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Component
public class SepaXmlGeneratorImpl implements SepaXmlGenerator {

    @Override
    public byte[] generate(String creditorIban, String creditorName, String creditorId,
                           List<SepaPaymentEntry> entries) {
        var msgId = "AMG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        var now = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        var totalAmount = entries.stream().map(SepaPaymentEntry::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pain.008.001.02\">\n");
        sb.append("  <CstmrDrctDbtInitn>\n");
        sb.append("    <GrpHdr>\n");
        sb.append("      <MsgId>").append(msgId).append("</MsgId>\n");
        sb.append("      <CreDtTm>").append(now).append("T00:00:00</CreDtTm>\n");
        sb.append("      <NbOfTxs>").append(entries.size()).append("</NbOfTxs>\n");
        sb.append("      <CtrlSum>").append(totalAmount.toPlainString()).append("</CtrlSum>\n");
        sb.append("      <InitgPty><Nm>").append(escape(creditorName)).append("</Nm></InitgPty>\n");
        sb.append("    </GrpHdr>\n");
        sb.append("    <PmtInf>\n");
        sb.append("      <PmtInfId>").append(msgId).append("-PI</PmtInfId>\n");
        sb.append("      <PmtMtd>DD</PmtMtd>\n");
        sb.append("      <NbOfTxs>").append(entries.size()).append("</NbOfTxs>\n");
        sb.append("      <CtrlSum>").append(totalAmount.toPlainString()).append("</CtrlSum>\n");
        sb.append("      <PmtTpInf><SvcLvl><Cd>SEPA</Cd></SvcLvl><LclInstrm><Cd>CORE</Cd></LclInstrm><SeqTp>RCUR</SeqTp></PmtTpInf>\n");
        sb.append("      <ReqdColltnDt>").append(LocalDate.now().plusDays(3).format(DateTimeFormatter.ISO_LOCAL_DATE)).append("</ReqdColltnDt>\n");
        sb.append("      <Cdtr><Nm>").append(escape(creditorName)).append("</Nm></Cdtr>\n");
        sb.append("      <CdtrAcct><Id><IBAN>").append(creditorIban).append("</IBAN></Id></CdtrAcct>\n");
        sb.append("      <CdtrSchmeId><Id><PrvtId><Othr><Id>").append(creditorId).append("</Id><SchmeNm><Prtry>SEPA</Prtry></SchmeNm></Othr></PrvtId></Id></CdtrSchmeId>\n");

        for (var entry : entries) {
            sb.append("      <DrctDbtTxInf>\n");
            sb.append("        <PmtId><EndToEndId>").append(entry.mandateId()).append("</EndToEndId></PmtId>\n");
            sb.append("        <InstdAmt Ccy=\"EUR\">").append(entry.amount().toPlainString()).append("</InstdAmt>\n");
            sb.append("        <DrctDbtTx><MndtRltdInf><MndtId>").append(entry.mandateId())
                    .append("</MndtId><DtOfSgntr>").append(entry.mandateSignatureDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .append("</DtOfSgntr></MndtRltdInf></DrctDbtTx>\n");
            sb.append("        <Dbtr><Nm>").append(escape(entry.debtorName())).append("</Nm></Dbtr>\n");
            sb.append("        <DbtrAcct><Id><IBAN>").append(entry.debtorIban()).append("</IBAN></Id></DbtrAcct>\n");
            sb.append("        <RmtInf><Ustrd>").append(escape(entry.remittanceInfo())).append("</Ustrd></RmtInf>\n");
            sb.append("      </DrctDbtTxInf>\n");
        }

        sb.append("    </PmtInf>\n");
        sb.append("  </CstmrDrctDbtInitn>\n");
        sb.append("</Document>");
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
