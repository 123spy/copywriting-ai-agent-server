package com.spy.copywritingaiagentserver.ai.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.spy.copywritingaiagentserver.ai.model.VisualPromptResult;
import com.spy.copywritingaiagentserver.common.ErrorCode;
import com.spy.copywritingaiagentserver.exception.BusinessException;
import com.spy.copywritingaiagentserver.file.CosClientConfig;
import com.spy.copywritingaiagentserver.file.CosManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageGenerationService {

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s)>\"]+");
    private static final Duration DOWNLOAD_WAIT_TIMEOUT = Duration.ofMinutes(12);
    private static final Duration DOWNLOAD_RETRY_INTERVAL = Duration.ofSeconds(15);

    private final CosManager cosManager;
    private final CosClientConfig cosClientConfig;

    private final OkHttpClient downloadClient = new OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

    public String generate(VisualPromptResult visualPromptResult) {
        log.info("ImageGenerationService start: style={}, hasPrompt={}",
                visualPromptResult == null ? "" : visualPromptResult.getStyle(),
                visualPromptResult != null && StringUtils.isNotBlank(visualPromptResult.getImagePrompt()));

        if (visualPromptResult == null || StringUtils.isBlank(visualPromptResult.getImagePrompt())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "image prompt is blank");
        }
        if (StringUtils.isAnyBlank(
                cosClientConfig.getAccessKey(),
                cosClientConfig.getSecretKey(),
                cosClientConfig.getRegion(),
                cosClientConfig.getBucket())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "COS config is incomplete");
        }

        String remoteImageUrl = requestGeneratedImageUrl(visualPromptResult.getImagePrompt());
        File downloadedImage = downloadRemoteImage(remoteImageUrl);
        String cosKey = buildCosKey(remoteImageUrl, downloadedImage.getName());
        try {
            cosManager.putObject(cosKey, downloadedImage);
            String uploadedUrl = buildUploadedUrl(cosKey);
            log.info("ImageGenerationService done: imageUrl={}", uploadedUrl);
            return uploadedUrl;
        } finally {
            if (!downloadedImage.delete()) {
                log.warn("temp image delete failed: {}", downloadedImage.getAbsolutePath());
            }
        }
    }

    private String requestGeneratedImageUrl(String imagePrompt) {
        try (Response response = CallXaisExample.generate(imagePrompt)) {
            if (response == null || !response.isSuccessful()) {
                String errorBody = null;
                try {
                    errorBody = response != null && response.body() != null ? response.body().string() : null;
                } catch (IOException ignored) {
                    log.warn("read image generation api error body failed");
                }
                log.error("image generation api failed: code={}, body={}",
                        response == null ? null : response.code(),
                        StringUtils.abbreviate(errorBody, 1000));
                throw new BusinessException(ErrorCode.API_REQUEST_ERROR, "image generation api failed");
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new BusinessException(ErrorCode.API_REQUEST_ERROR, "image generation api returned empty body");
            }
            String rawBody = responseBody.string();
            String imageUrl = extractImageUrl(rawBody);
            if (StringUtils.isBlank(imageUrl)) {
                log.error("image generation api did not return image url: {}", rawBody);
                throw new BusinessException(ErrorCode.API_REQUEST_ERROR, "image generation api did not return url");
            }
            return imageUrl;
        } catch (IOException e) {
            log.error("call real image generation api failed", e);
            throw new BusinessException(ErrorCode.API_REQUEST_ERROR, "real image generation failed");
        }
    }

    private String extractImageUrl(String rawBody) {
        if (StringUtils.isBlank(rawBody)) {
            return null;
        }
        String normalizedBody = rawBody.replace("\\u0026", "&");
        try {
            JSONObject root = JSONUtil.parseObj(normalizedBody);
            JSONArray choices = root.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject firstChoice = choices.getJSONObject(0);
                JSONObject message = firstChoice == null ? null : firstChoice.getJSONObject("message");
                String content = message == null ? null : message.getStr("content");
                String extracted = matchFirstUrl(content);
                if (StringUtils.isNotBlank(extracted)) {
                    return extracted;
                }
            }
        } catch (Exception ignored) {
            log.debug("image generation response is not standard json, fallback to regex url parse");
        }
        return matchFirstUrl(normalizedBody);
    }

    private String matchFirstUrl(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        Matcher matcher = URL_PATTERN.matcher(text.replace("\\u0026", "&"));
        if (!matcher.find()) {
            return null;
        }
        String url = matcher.group();
        url = url.replace("&amp;", "&");
        return url;
    }

    private File downloadRemoteImage(String remoteImageUrl) {
        Instant deadline = Instant.now().plus(DOWNLOAD_WAIT_TIMEOUT);
        int attempt = 0;
        while (Instant.now().isBefore(deadline)) {
            attempt++;
            Request request = new Request.Builder()
                    .url(remoteImageUrl)
                    .get()
                    .build();
            try (Response response = downloadClient.newCall(request).execute()) {
                ResponseBody responseBody = response.body();
                String contentType = responseBody == null || responseBody.contentType() == null
                        ? ""
                        : responseBody.contentType().toString();
                if (response.isSuccessful() && responseBody != null && isImageContentType(contentType)) {
                    String extension = resolveExtension(remoteImageUrl, contentType);
                    File tempFile = File.createTempFile("generated-image-", extension);
                    try (InputStream inputStream = responseBody.byteStream();
                         FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                        inputStream.transferTo(outputStream);
                    }
                    log.info("generated image download ready after {} attempt(s), contentType={}", attempt, contentType);
                    return tempFile;
                }
                log.info("generated image not ready yet, attempt={}, code={}, contentType={}",
                        attempt, response.code(), contentType);
            } catch (IOException e) {
                log.warn("generated image download attempt failed, attempt={}, url={}", attempt, remoteImageUrl, e);
            }

            sleepBeforeRetry();
        }
        log.error("download generated image timed out after waiting {} seconds, url={}",
                DOWNLOAD_WAIT_TIMEOUT.getSeconds(), remoteImageUrl);
        throw new BusinessException(ErrorCode.API_REQUEST_ERROR, "download generated image failed");
    }

    private boolean isImageContentType(String contentType) {
        return StringUtils.isNotBlank(contentType) && contentType.toLowerCase().startsWith("image/");
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(DOWNLOAD_RETRY_INTERVAL.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.API_REQUEST_ERROR, "download generated image interrupted");
        }
    }

    private String resolveExtension(String imageUrl, String contentType) {
        String lowerUrl = imageUrl == null ? "" : imageUrl.toLowerCase();
        if (lowerUrl.contains(".png")) {
            return ".png";
        }
        if (lowerUrl.contains(".webp")) {
            return ".webp";
        }
        if (lowerUrl.contains(".jpeg") || lowerUrl.contains(".jpg")) {
            return ".jpg";
        }
        if (contentType != null) {
            String lowerType = contentType.toLowerCase();
            if (lowerType.contains("png")) {
                return ".png";
            }
            if (lowerType.contains("webp")) {
                return ".webp";
            }
            if (lowerType.contains("jpeg") || lowerType.contains("jpg")) {
                return ".jpg";
            }
        }
        return ".png";
    }

    private String buildCosKey(String remoteImageUrl, String fallbackName) {
        String extension = resolveExtension(remoteImageUrl, null);
        if (StringUtils.isNotBlank(fallbackName) && fallbackName.contains(".")) {
            extension = fallbackName.substring(fallbackName.lastIndexOf('.'));
        }
        LocalDate today = LocalDate.now();
        return String.format("/copywriting/%d/%02d/%02d/%s%s",
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                UUID.randomUUID().toString().replace("-", ""),
                extension);
    }

    private String buildUploadedUrl(String cosKey) {
        String host = resolveCosHost();
        return host + cosKey;
    }

    private String resolveCosHost() {
        if (StringUtils.isNotBlank(cosClientConfig.getHost())) {
            return StringUtils.removeEnd(cosClientConfig.getHost(), "/");
        }
        return String.format("https://%s.cos.%s.myqcloud.com",
                cosClientConfig.getBucket(),
                cosClientConfig.getRegion());
    }
}
