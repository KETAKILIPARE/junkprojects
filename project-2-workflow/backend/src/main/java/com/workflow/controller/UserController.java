package com.workflow.controller;

import com.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<String>> getAllUsernames() {
        return ResponseEntity.ok(
                userRepository.findAll().stream()
                        .map(u -> u.getUsername())
                        .toList()
        );
    }
}
