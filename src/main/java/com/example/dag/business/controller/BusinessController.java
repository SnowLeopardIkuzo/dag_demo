package com.example.dag.business.controller;

import com.example.dag.business.exception.BusinessException;
import com.example.dag.business.service.BusinessService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class BusinessController {

    @Resource
    private BusinessService businessService;

    @PostMapping("/sync")
    public Map<String, Object> sync(@RequestParam(defaultValue = "demoGraph") String graphName) {
        try {
            int size = businessService.sync(graphName);
            return Map.of("code", 0, "message", "success", "data", size);
        } catch (BusinessException e) {
            return Map.of("code", -1, "message", e.getMessage());
        } catch (Exception e) {
            return Map.of("code", -2, "message", e.getMessage());
        }
    }

    @PostMapping("/sync-with-timeout")
    public Map<String, Object> syncWithTimeout(@RequestParam(defaultValue = "demoGraph") String graphName,
                                               @RequestParam(defaultValue = "500") long timeout) {
        try {
            int size = businessService.syncWithTimeout(graphName, timeout);
            return Map.of("code", 0, "message", "success", "data", size);
        } catch (BusinessException e) {
            return Map.of("code", -1, "message", e.getMessage());
        } catch (Exception e) {
            return Map.of("code", -2, "message", e.getMessage());
        }
    }

    @PostMapping("/async")
    public Map<String, Object> async(@RequestParam(defaultValue = "demoGraph") String graphName,
                                     @RequestParam(defaultValue = "500") long timeout) {
        try {
            int size = businessService.async(graphName, timeout);
            return Map.of("code", 0, "message", "success", "data", size);
        } catch (BusinessException e) {
            return Map.of("code", -1, "message", e.getMessage());
        } catch (Exception e) {
            return Map.of("code", -2, "message", e.getMessage());
        }
    }

}
