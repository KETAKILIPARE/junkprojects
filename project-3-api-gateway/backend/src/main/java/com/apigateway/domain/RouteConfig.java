package com.apigateway.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "route_configs")
@Getter
@Setter
@NoArgsConstructor
public class RouteConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String pathPrefix;

    @Column(nullable = false)
    private String targetUrl;

    @Column(nullable = false)
    private int rateLimit;

    @Column(nullable = false)
    private boolean requiresAuth;

    public RouteConfig(String pathPrefix, String targetUrl, int rateLimit, boolean requiresAuth) {
        this.pathPrefix = pathPrefix;
        this.targetUrl = targetUrl;
        this.rateLimit = rateLimit;
        this.requiresAuth = requiresAuth;
    }
}
