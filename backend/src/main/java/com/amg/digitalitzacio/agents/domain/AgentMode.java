package com.amg.digitalitzacio.agents.domain;

public enum AgentMode {
    AUTO,    // Response sent immediately
    HYBRID,  // Response saved as pending, tenant notified for approval
    MANUAL   // No automatic response, tenant notified only
}
