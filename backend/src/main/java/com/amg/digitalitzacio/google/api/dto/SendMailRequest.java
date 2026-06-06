package com.amg.digitalitzacio.google.api.dto;

public record SendMailRequest(
    String to,
    String subject,
    String body
) {}
