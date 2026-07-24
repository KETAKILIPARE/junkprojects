package com.apigateway.controller;

import com.apigateway.domain.RouteConfig;
import com.apigateway.dto.RouteRequest;
import com.apigateway.repository.RouteConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteConfigRepository routeConfigRepository;

    @PostMapping
    public ResponseEntity<RouteConfig> create(@RequestBody RouteRequest request) {
        RouteConfig route = new RouteConfig(
                request.pathPrefix(),
                request.targetUrl(),
                request.rateLimit(),
                request.requiresAuth()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(routeConfigRepository.save(route));
    }

    @GetMapping
    public ResponseEntity<List<RouteConfig>> getAll() {
        return ResponseEntity.ok(routeConfigRepository.findAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        routeConfigRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
