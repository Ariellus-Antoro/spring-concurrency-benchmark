package com.ttu.reactive.service;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service 
public class TokenGenerationService {
    public Mono<String> generateToken(Long userId, Long concertId){
        return Mono.fromCallable(()->{
            String rawData = userId + ":" + concertId + ":" + UUID.randomUUID();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawData.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        });
    } 
}
