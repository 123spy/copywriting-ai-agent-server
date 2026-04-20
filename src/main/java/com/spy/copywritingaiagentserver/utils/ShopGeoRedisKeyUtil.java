package com.spy.server.utils;

public final class ShopGeoRedisKeyUtil {

    public static final String SHOP_GEO_KEY_PATTERN = "shop:geo:*";
    public static final String SHOP_GEO_KEY = "shop:geo:all";
    public static final String SHOP_GEO_LAST_SYNC_KEY = "shop:geo:sync:lastTime";

    private ShopGeoRedisKeyUtil() {
    }
}
