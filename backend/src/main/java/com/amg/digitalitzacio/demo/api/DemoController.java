package com.amg.digitalitzacio.demo.api;

import com.amg.digitalitzacio.demo.api.dto.DemoFlowResponse;
import com.amg.digitalitzacio.demo.api.dto.DemoListResponse;
import com.amg.digitalitzacio.demo.application.DemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/demo")
@RequiredArgsConstructor
public class DemoController {

    private final DemoService demoService;

    @GetMapping
    public DemoListResponse listDemos() {
        return demoService.listDemos();
    }

    @GetMapping("/{id}")
    public DemoFlowResponse getDemo(@PathVariable String id) {
        return demoService.getDemo(id);
    }
}
