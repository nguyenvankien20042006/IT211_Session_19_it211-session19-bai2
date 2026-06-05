package com.example.bai2;

import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        InsecureTokenService insecureTokenService = new InsecureTokenService();
        // Giả mạo hacker tấn công với Secret key không chinh xác
        SecretKey fakeSecretKey = Keys.hmacShaKeyFor("fakeSecretKeyfakeSecretKeyfakeSecretKeyfakeSecretKey".getBytes(StandardCharsets.UTF_8));
        // Tạo token với secret key không chính xác
        String token = insecureTokenService.generateToken("user123");
        // Kiểm tra token
        boolean isValid = insecureTokenService.validateToken(token);
        System.out.println("Token is valid: " + isValid);

        // Mô phỏng token hết hạn
        String token2 = insecureTokenService.generateToken("user123");
        Thread.sleep(1000000);
        // Gọi phương thức validate sẽ in ra lỗi vì token đã hết hạn
        insecureTokenService.validateToken(token2);
    }
}
