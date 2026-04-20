package com.spy.copywritingaiagentserver.agent.model;

public enum PlatformToPath {
    DOUYIN("抖音", "douyin"),
    WECHAT("微信图文", "weixin"),
    XIAOHONGSHU("小红书", "xiaohongshu");

    private String platform;
    private String path;

    PlatformToPath(String platform, String path) {
        this.platform = platform;
        this.path = path;
    }
}
