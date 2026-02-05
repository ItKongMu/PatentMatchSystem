package com.patent;

import cn.hutool.crypto.digest.BCrypt;

public class BCryptTest {
    public static void main(String[] args) {
        String password = "admin123";
        String storedHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt3GWoa";
        
        // 验证已有的哈希
        System.out.println("验证已有哈希: " + BCrypt.checkpw(password, storedHash));
        
        // 生成新的哈希
        String newHash = BCrypt.hashpw(password);
        System.out.println("新生成的哈希: " + newHash);
        
        // 验证新哈希
        System.out.println("验证新哈希: " + BCrypt.checkpw(password, newHash));
    }
}
