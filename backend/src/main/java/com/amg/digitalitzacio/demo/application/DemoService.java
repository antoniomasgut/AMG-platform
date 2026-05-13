package com.amg.digitalitzacio.demo.application;

import com.amg.digitalitzacio.demo.api.dto.DemoFlowResponse;
import com.amg.digitalitzacio.demo.api.dto.DemoListResponse;

public interface DemoService {
    DemoListResponse listDemos();
    DemoFlowResponse getDemo(String id);
}
