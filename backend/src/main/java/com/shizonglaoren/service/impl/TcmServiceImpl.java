package com.shizonglaoren.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shizonglaoren.dto.TcmConsultRequest;
import com.shizonglaoren.dto.TcmConsultResponse;
import com.shizonglaoren.entity.TcmRecord;
import com.shizonglaoren.exception.BusinessException;
import com.shizonglaoren.repository.TcmRecordRepository;
import com.shizonglaoren.service.TcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.BufferedSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TcmServiceImpl implements TcmService {

    private final TcmRecordRepository tcmRecordRepository;
    private final ObjectMapper objectMapper;

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.api-url}")
    private String apiUrl;

    @Value("${deepseek.model}")
    private String model;

    private static final String SYSTEM_PROMPT =
            "你是一位专业的中医健康顾问，专注于为老人和小孩提供中医养生科普建议。\n" +
            "用户会描述症状，请用以下固定格式回答（每项必须换行，不超过350字）：\n\n" +
            "【中医辨证】\n简要说明症状的中医原因（2-3句话）\n\n" +
            "【调理建议】\n列出2-3个食疗或中药茶饮方案\n\n" +
            "【穴位按摩】\n推荐1-2个穴位，说明位置和按摩方法\n\n" +
            "【注意事项】\n生活起居注意2-3条\n\n" +
            "【联系方式】\n如需了解更多中医调理方法，欢迎加入我们的微信群：123456789\n\n" +
            "重要原则：\n" +
            "- 建议仅供科普参考，不替代正规医疗\n" +
            "- 症状严重时必须注明「请及时就医」\n" +
            "- 不提供具体药物剂量，只提供方向性建议\n" +
            "- 语言简洁通俗，适合普通家属理解";

    @Override
    public SseEmitter streamConsult(Long userId, TcmConsultRequest request) {
        SseEmitter emitter = new SseEmitter(60_000L);

        String userTypeLabel = "elder".equals(request.getUserType()) ? "老人" : "小孩";
        String userMessage = "患者类型：" + userTypeLabel + "\n症状描述：" + request.getSymptom();
        log.info("流式问诊请求，userId={}, userType={}", userId, request.getUserType());

        CompletableFuture.runAsync(() -> {
            StringBuilder fullContent = new StringBuilder();
            try {
                OkHttpClient streamClient = new OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .build();

                ObjectNode body = objectMapper.createObjectNode();
                body.put("model", model);
                body.put("stream", true);
                body.put("max_tokens", 1024);
                ArrayNode messages = body.putArray("messages");
                messages.addObject().put("role", "system").put("content", SYSTEM_PROMPT);
                messages.addObject().put("role", "user").put("content", userMessage);

                Request okhttpRequest = new Request.Builder()
                        .url(apiUrl)
                        .post(RequestBody.create(objectMapper.writeValueAsString(body),
                                MediaType.parse("application/json; charset=utf-8")))
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Accept", "text/event-stream")
                        .build();

                try (Response response = streamClient.newCall(okhttpRequest).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        emitter.send(SseEmitter.event().name("error").data("AI服务暂时不可用，请稍后重试"));
                        emitter.complete();
                        return;
                    }

                    BufferedSource source = response.body().source();
                    while (!source.exhausted()) {
                        String line = source.readUtf8Line();
                        if (line == null) break;
                        if (!line.startsWith("data: ")) continue;

                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) {
                            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                            break;
                        }

                        try {
                            JsonNode node = objectMapper.readTree(data);
                            JsonNode delta = node.path("choices").get(0).path("delta");
                            String content = delta.path("content").asText("");
                            if (!content.isEmpty()) {
                                fullContent.append(content);
                                // 直接发送内容片段（非JSON，前端直接拼接）
                                emitter.send(SseEmitter.event().data(content));
                            }
                        } catch (Exception ignore) {
                            // 忽略不完整的 JSON 片段
                        }
                    }
                }

                // 流式完成后保存完整记录到数据库
                if (fullContent.length() > 0) {
                    TcmRecord record = TcmRecord.builder()
                            .userId(userId)
                            .userType(request.getUserType())
                            .symptom(request.getSymptom())
                            .advice(fullContent.toString())
                            .build();
                    tcmRecordRepository.save(record);
                }

                emitter.complete();

            } catch (IOException e) {
                log.error("流式问诊连接中断", e);
                try { emitter.complete(); } catch (Exception ignored) {}
            } catch (Exception e) {
                log.error("流式问诊异常", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("服务异常，请重试"));
                    emitter.complete();
                } catch (Exception ignored) {}
            }
        });

        return emitter;
    }

    @Override
    public TcmConsultResponse consult(Long userId, TcmConsultRequest request) {
        String userTypeLabel = "elder".equals(request.getUserType()) ? "老人" : "小孩";
        String userMessage = "患者类型：" + userTypeLabel + "\n症状描述：" + request.getSymptom();

        log.info("中医问诊请求，userId={}, userType={}, symptom={}", userId, request.getUserType(), request.getSymptom());

        String advice = callDeepSeek(userMessage);

        TcmRecord record = TcmRecord.builder()
                .userId(userId)
                .userType(request.getUserType())
                .symptom(request.getSymptom())
                .advice(advice)
                .build();
        record = tcmRecordRepository.save(record);

        return TcmConsultResponse.builder()
                .recordId(record.getId())
                .advice(advice)
                .build();
    }

    @Override
    public List<TcmRecord> getHistory(Long userId) {
        return tcmRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private String callDeepSeek(String userMessage) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();

        try {
            // 构造请求体
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("stream", false);
            body.put("max_tokens", 1024);

            ArrayNode messages = body.putArray("messages");

            ObjectNode systemMsg = messages.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", SYSTEM_PROMPT);

            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);

            String requestBody = objectMapper.writeValueAsString(body);

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json; charset=utf-8")))
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.error("DeepSeek API 调用失败，状态码={}", response.code());
                    throw BusinessException.serverError("AI服务暂时不可用，请稍后重试");
                }

                String responseBody = response.body().string();
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                return jsonNode
                        .path("choices")
                        .get(0)
                        .path("message")
                        .path("content")
                        .asText();
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("DeepSeek API 调用异常", e);
            throw BusinessException.serverError("AI服务连接失败，请检查网络后重试");
        }
    }
}
