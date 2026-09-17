package com.ttu.virtual_threads.service;

import java.util.Base64;
import java.util.UUID;

import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service 
public class TokenGenerationService {
    public String generateToken(Long userId, Long concertId){
        try{
            String rawData = userId + ":" + concertId + ":" + UUID.randomUUID();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawData.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error algorithm not found", e);
        }
    }
}
