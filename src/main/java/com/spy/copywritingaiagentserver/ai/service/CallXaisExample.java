package com.spy.copywritingaiagentserver.ai.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
public class CallXaisExample {
  public static Response generate(String prompt) throws IOException {
    // 注意绘制时间很长，超时要足够，只是5分钟
    OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(300, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .followRedirects(true)
        .build();
    JSONObject requestBody = new JSONObject();
    // 出4k图用gemini-3-pro-image-preview-max-4K
    requestBody.put("model", "Nano_Banana_Pro_2K_0");

    JSONArray array = new JSONArray();
    JSONObject obj = new JSONObject();
    obj.put("role", "user");
    obj.put("content", prompt);
    array.add(obj);
    requestBody.put("messages", array);
    Headers headers = new Headers.Builder().add("Authorization", "Bearer x-B6PWd9oNdYFViDQ3YUQMIFoWAjeqJN5GwolRBLdYihh2GCLW").build();
    Request request = new Request.Builder()
        .url("https://sg2.dchai.cn/v1/chat/completions")
        .post(RequestBody.create(
            MediaType.parse("application/json"),
            requestBody.toString().getBytes(StandardCharsets.UTF_8)))
        .headers(headers)
        .build();

    // 输出是markdown格式，注意url是按unicode编码的，需要将其中"\u0026"转回"&"
    return client.newCall(request).execute();
  }

  public static void main(String[] args) throws IOException {
    String prompt = "style=冷调科技感摄影风格，真实生活场景融合微距质感特写，无LOGO、无硬广元素，强调自然融入与可触达的旗舰体验, scene=清晨通勤地铁车厢内，手机置于磨砂帆布通勤包边缘，屏幕亮起显示微信扫码界面（绿码已出），背景虚化呈现轻微晃动的车窗光影；或俯拍视角： 手机静置在木质办公桌一角，旁有半杯拿铁与打开的笔记本，屏幕显示4K视频剪辑时间轴界面, composition=三分法构图，主体为手机中框CNC微磨砂纹理特写（占画面左1/3），右侧留白呈现真实使用场景细节（如指尖轻触屏幕、耳机线搭在手机边缘暗示反向充电）； 景深控制精准，焦点落在金属切边与屏幕动效交界处，背景柔和虚化但保有生活温度, imagePrompt=Ultra-detailed macro photography of a modern rectangular smartphone with subtly textured matte metal frame (realistic CNC-machined edge visible), placed naturally on a warm-toned wooden desk beside a ceramic coffee cup and an open notebook; screen displays clean, smooth UI animation示意 — either a generic health code interface with green checkmark or a timeline-based editing interface, rendered as stylized digital overlay (not device-specific OS); shallow depth of field, precise focus on frame texture and screen glow intersection, cool-toned color grading (teal, graphite, soft silver), no logos, no branding, no text, lifestyle realism, Xiaohongshu aesthetic, 8k";

    generate(prompt);
//    Response response = generate(prompt);
//    System.out.println(response);
  }
}
