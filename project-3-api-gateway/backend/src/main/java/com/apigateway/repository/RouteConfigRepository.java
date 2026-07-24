package com.apigateway.repository;

import com.apigateway.domain.RouteConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RouteConfigRepository extends JpaRepository<RouteConfig, UUID> {
    Optional<RouteConfig> findByPathPrefixStartingWith(String path);
}
