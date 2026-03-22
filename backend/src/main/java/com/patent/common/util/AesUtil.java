package com.patent.common.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.symmetric.AES;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * AES 对称加密工具（用于 API Key 加密存储）
 * 密钥从配置文件读取，默认 32 字节占位符（生产环境必须覆盖）
 */
@Component
public class AesUtil {

    private final AES aes;

    public AesUtil(@Value("${patent.api-key-secret:PatentSys@AES256Key!ChangeMe!!}") String secret) {
        // 取前 32 字节作为 AES-256 密钥
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = new byte[32];
        System.arraycopy(key, 0, keyBytes, 0, Math.min(key.length, 32));
        this.aes = new AES(keyBytes);
    }

    /** 加密，返回 Base64 密文；入参为空则返回 null */
    public String encrypt(String plainText) {
        if (StrUtil.isBlank(plainText)) return null;
        return aes.encryptBase64(plainText);
    }

    /** 解密，返回明文；入参为空则返回 null */
    public String decrypt(String cipherText) {
        if (StrUtil.isBlank(cipherText)) return null;
        try {
            return aes.decryptStr(cipherText);
        } catch (Exception e) {
            // 兼容旧数据（未加密的明文直接返回）
            return cipherText;
        }
    }
}
