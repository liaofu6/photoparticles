package com.photoparticles.client;

import com.photoparticles.block.PhotoBlockEntities;
import com.photoparticles.client.render.PhotoBlockRenderer;
import com.photoparticles.client.render.VideoBlockRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhotoParticlesClient implements ClientModInitializer {
    /** 图片解码放到后台线程，避免阻塞渲染线程造成卡顿。 */
    private static final ExecutorService DECODER = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "photoparticles-decoder");
        thread.setDaemon(true);
        return thread;
    });

    public static void runAsync(Runnable task) {
        DECODER.execute(task);
    }

    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(PhotoBlockEntities.PHOTO_BLOCK_ENTITY, context -> new PhotoBlockRenderer());
        BlockEntityRenderers.register(PhotoBlockEntities.VIDEO_BLOCK_ENTITY, context -> new VideoBlockRenderer());
    }

    public static void openUploadScreen(BlockPos pos) {
        if (!ensureSupported()) {
            return;
        }
        Minecraft.getInstance().setScreenAndShow(new PhotoUploadScreen(pos, false));
    }

    public static void openVideoScreen(BlockPos pos) {
        if (!ensureSupported()) {
            return;
        }
        Minecraft.getInstance().setScreenAndShow(new PhotoUploadScreen(pos, true));
    }

    private static boolean ensureSupported() {
        if (PhotoBlockRenderer.imageSupportAvailable()) {
            return true;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendOverlayMessage(
                    Component.literal("当前运行环境缺少图片解码能力，方块只能显示模型"));
        }
        return false;
    }
}
