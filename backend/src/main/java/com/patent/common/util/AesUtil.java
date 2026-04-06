package com.patent.common.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.symmetric.AES;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * AES 对称加密工具（用于 API Key 加密存储）
 * <p>
 * 密钥来源：{@code patent.api-key-secret} 配置项（建议通过环境变量 {@code API_KEY_SECRET} 注入）。
 * <p>
 * 密钥长度处理规则（AES-256 要求 32 字节）：
 * <ul>
 *   <li>密钥超过 32 字节：截取前 32 字节</li>
 *   <li>密钥不足 32 字节：右侧以 0x00 字节填充至 32 字节</li>
 * </ul>
 * 默认密钥 {@code PatentSys@AES256Key!ChangeMe!!} 恰好 32 字节（ASCII），可直接使用。
 * <p>
 * ⚠️ 生产环境必须通过环境变量覆盖默认密钥，否则任何人都能解密数据库中的 API Key！
 * 示例：{@code export API_KEY_SECRET=your-random-32-byte-secret-here!!}
 */
@Component
public class AesUtil {

    private final AES aes;

    public AesUtil(@Value("${patent.api-key-secret:PatentSys@AES256Key!ChangeMe!!}") String secret) {
        byte[] rawKey = secret.getBytes(StandardCharsets.UTF_8);
        // AES-256 固定需要 32 字节密钥：超出截断，不足以 0x00 填充（Arrays.copyOf 会自动填 0）
        byte[] keyBytes = Arrays.copyOf(rawKey, 32);
        this.aes = new AES(keyBytes);
    }

    /**
     * 加密，返回 Base64 密文；入参为空则返回 null
     */
    public String encrypt(String plainText) {
        if (StrUtil.isBlank(plainText)) return null;
        return aes.encryptBase64(plainText);
    }

    /**
     * 解密，返回明文；入参为空则返回 null。
     * <p>
     * 兼容处理：若密文无法用当前密钥解密（如旧版明文数据或密钥已更换），
     * 则直接返回原始字符串（降级为明文），并记录异常由调用方感知。
     */
    public String decrypt(String cipherText) {
        if (StrUtil.isBlank(cipherText)) return null;
        try {
            return aes.decryptStr(cipherText);
        } catch (Exception e) {
            // 兼容旧数据（未加密的明文），降级直接返回原始字符串
            return cipherText;
        }
    }
}
