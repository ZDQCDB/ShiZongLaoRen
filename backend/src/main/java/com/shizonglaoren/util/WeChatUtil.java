package com.shizonglaoren.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shizonglaoren.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 微信小程序工具类（调用微信官方接口）
 */
@Slf4j
@Component
public class WeChatUtil {

    @Value("${wechat.app-id}")
    private String appId;

    @Value("${wechat.app-secret}")
    private String appSecret;

    @Value("${wechat.code2session-url}")
    private String code2SessionUrl;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 通过code获取微信openid
     *
     * @param code wx.login()获取的临时凭证
     * @return openid
     */
    public String getOpenId(String code) {
        String url = String.format("%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                code2SessionUrl, appId, appSecret, code);

        Request request = new Request.Builder().url(url).get().build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw BusinessException.serverError("微信接口调用失败");
            }

            String body = response.body().string();
            log.debug("微信code2session响应: {}", body);

            JsonNode node = objectMapper.readTree(body);

            // 检查微信错误码
            if (node.has("errcode") && node.get("errcode").asInt() != 0) {
                String errmsg = node.has("errmsg") ? node.get("errmsg").asText() : "未知错误";
                log.error("微信登录失败: errcode={}, errmsg={}", node.get("errcode").asInt(), errmsg);
                throw BusinessException.badRequest("微信登录失败：" + errmsg);
            }

            if (!node.has("openid")) {
                throw BusinessException.serverError("获取openid失败");
            }

            return node.get("openid").asText();

        } catch (IOException e) {
            log.error("调用微信接口异常", e);
            throw BusinessException.serverError("微信接口调用异常");
        }
    }
}
