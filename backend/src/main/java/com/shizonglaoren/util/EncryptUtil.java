package com.shizonglaoren.util;

import cn.hutool.crypto.symmetric.AES;
import cn.hutool.core.codec.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 数据加解密工具类（AES对称加密，用于敏感信息如身份证号）
 */
@Slf4j
@Component
public class EncryptUtil {

    /** AES密钥（16/24/32字节）从JWT secret中派生 */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /** 获取16字节AES密钥 */
    private byte[] getAesKey() {
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        byte[] key = new byte[16];
        for (int i = 0; i < 16; i++) {
            key[i] = (i < secretBytes.length) ? secretBytes[i] : 0;
        }
        return key;
    }

    /**
     * AES加密
     *
     * @param plainText 明文
     * @return Base64编码的密文（数据库存储用）
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) return plainText;
        try {
            AES aes = new AES(getAesKey());
            byte[] encrypted = aes.encrypt(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.encode(encrypted);
        } catch (Exception e) {
            log.error("加密失败", e);
            return plainText;
        }
    }

    /**
     * AES解密
     *
     * @param cipherText Base64编码的密文
     * @return 明文
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isBlank()) return cipherText;
        try {
            AES aes = new AES(getAesKey());
            byte[] decrypted = aes.decrypt(Base64.decode(cipherText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("解密失败（可能是未加密的明文数据）", e);
            return cipherText;
        }
    }

    /**
     * 脱敏处理身份证号（用于展示，如：330102****1234）
     *
     * @param idCard 身份证号明文
     * @return 脱敏字符串
     */
    public String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) return "****";
        return idCard.substring(0, 6) + "****" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 手机号脱敏（如：138****5678）
     *
     * @param phone 手机号
     * @return 脱敏字符串
     */
    public String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "****";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
