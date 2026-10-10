package com.smartcampus.placement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
public class HomeController {

  @GetMapping("/")
  public ResponseEntity<Map<String, Object>> root() {
    return ResponseEntity.ok(Map.of(
        "status", "UP",
        "service", "Smart Campus Placement System API",
        "message", "Backend is live and running successfully!",
        "endpoints", List.of(
            "/api/dashboard",
            "/api/students",
            "/api/companies",
            "/api/jobs",
            "/api/applications"
        )
    ));
  }
}
