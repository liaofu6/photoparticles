package com.photoparticles.client;

import com.photoparticles.network.VideoRequestPayload;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;

/**
 * 客户端保存已收到的视频数据，按方块位置和版本号校验。
 * 视频不再塞进方块同步包，而是按需向服务端索要。
 */
public final class VideoDataCache {
    public record Entry(int version, byte[] data) {
    }

    private static final Map<BlockPos, Entry> DATA = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Long> LAST_REQUEST = new ConcurrentHashMap<>();
    private static final long REQUEST_INTERVAL_MS = 1000L;

    private VideoDataCache() {
    }

    public static Entry get(BlockPos pos) {
        return DATA.get(pos);
    }

    public static void put(BlockPos pos, int version, byte[] data) {
        DATA.put(pos, new Entry(version, data));
    }

    public static void request(BlockPos pos) {
        long now = System.currentTimeMillis();
        Long last = LAST_REQUEST.get(pos);
        if (last != null && now - last < REQUEST_INTERVAL_MS) {
            return;
        }
        LAST_REQUEST.put(pos, now);
        ClientPlayNetworking.send(new VideoRequestPayload(pos));
    }
}
