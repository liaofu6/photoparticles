package com.photoparticles.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.photoparticles.block.VideoBlockEntity;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.LightCoordsUtil;

/**
 * 视频方块：读取本地 GIF 的全部帧，按固定帧率循环播放。
 */
public class VideoBlockRenderer implements BlockEntityRenderer<VideoBlockEntity, VideoBlockRenderer.State> {
    private static final int MAX_SIDE = 224;
    private static final int MAX_FRAMES = 60;
    private static final int TICKS_PER_FRAME = 2;
    private static final Map<BlockPos, VideoTexture> CACHE = new HashMap<>();
    private static final java.util.Set<BlockPos> LOADING = new java.util.HashSet<>();

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(VideoBlockEntity blockEntity, State state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderState.extractBase(blockEntity, state, overlay);
        state.pos = blockEntity.getBlockPos();
        state.source = blockEntity.getSource();
        state.data = blockEntity.getData();
        state.facing = blockEntity.getFacing();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (state.source.isEmpty()) {
            return;
        }
        if (!PhotoBlockRenderer.imageSupportAvailable()) {
            return;
        }
        VideoTexture video = CACHE.get(state.pos);
        if (video == null || !video.source.equals(state.source) || video.data != state.data) {
            requestLoad(state.pos, state.source, state.data);
            return;
        }
        if (video.frames.isEmpty()) {
            return;
        }

        int frameIndex = 0;
        if (Minecraft.getInstance().level != null) {
            long ticks = Minecraft.getInstance().level.getGameTime();
            frameIndex = (int) ((ticks / TICKS_PER_FRAME) % video.frames.size());
        }
        if (frameIndex != video.lastFrame) {
            int[] pixels = video.frames.get(frameIndex);
            for (int y = 0; y < video.height; y++) {
                for (int x = 0; x < video.width; x++) {
                    video.image.setPixelABGR(x, y, PhotoBlockRenderer.toAbgr(pixels[y * video.width + x]));
                }
            }
            video.texture.upload();
            video.lastFrame = frameIndex;
        }

        Direction normal = state.facing;
        double rightX = normal.getStepZ();
        double rightZ = -normal.getStepX();
        float pixel = PhotoBlockRenderer.PIXEL_SIZE;
        double planeWidth = video.width * (double) pixel;
        double planeHeight = video.height * (double) pixel;
        double centerX = 0.5D + normal.getStepX() * 0.55D;
        double centerZ = 0.5D + normal.getStepZ() * 0.55D;
        double bottomY = 1.02D;
        double x0 = centerX - rightX * planeWidth / 2.0D;
        double z0 = centerZ - rightZ * planeWidth / 2.0D;
        double x1 = centerX + rightX * planeWidth / 2.0D;
        double z1 = centerZ + rightZ * planeWidth / 2.0D;
        double y1 = bottomY + planeHeight;
        float nx = normal.getStepX();
        float nz = normal.getStepZ();
        int light = LightCoordsUtil.FULL_BRIGHT;

        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(video.id), (pose, buffer) -> {
            buffer.addVertex(pose, (float) x0, (float) bottomY, (float) z0)
                    .setColor(0xFFFFFFFF).setUv(0.0F, 1.0F)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, 0.0F, nz);
            buffer.addVertex(pose, (float) x1, (float) bottomY, (float) z1)
                    .setColor(0xFFFFFFFF).setUv(1.0F, 1.0F)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, 0.0F, nz);
            buffer.addVertex(pose, (float) x1, (float) y1, (float) z1)
                    .setColor(0xFFFFFFFF).setUv(1.0F, 0.0F)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, 0.0F, nz);
            buffer.addVertex(pose, (float) x0, (float) y1, (float) z0)
                    .setColor(0xFFFFFFFF).setUv(0.0F, 0.0F)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, 0.0F, nz);
        });
    }

    /** 解码放到后台线程，渲染线程只登记一次请求，本帧先不画。 */
    private static void requestLoad(BlockPos pos, String source, byte[] data) {
        if (!LOADING.add(pos)) {
            return;
        }
        com.photoparticles.client.PhotoParticlesClient.runAsync(() -> {
            Frames frames = decode(source, data);
            Minecraft.getInstance().execute(() -> {
                LOADING.remove(pos);
                if (frames == null || frames.frames.isEmpty()) {
                    CACHE.remove(pos);
                    return;
                }
                Identifier id = Identifier.fromNamespaceAndPath("photoparticles",
                        "dyn/video_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ());
                NativeImage image = new NativeImage(frames.width, frames.height, true);
                DynamicTexture texture = new DynamicTexture(() -> "photoparticles video", image);
                VideoTexture old = CACHE.get(pos);
                Minecraft.getInstance().getTextureManager().register(id, texture);
                if (old != null) {
                    old.texture.close();
                }
                CACHE.put(pos, new VideoTexture(source, data, id, image, texture,
                        frames.frames, frames.width, frames.height));
            });
        });
    }

    private static Frames decode(String path, byte[] data) {
        List<int[]> frames = new ArrayList<>();
        int width = 0;
        int height = 0;
        try {
            ImageInputStream input = data.length > 0
                    ? ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(data))
                    : ImageIO.createImageInputStream(new File(path));
            if (input == null) {
                return null;
            }
            try (ImageInputStream stream = input) {
                Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
                if (!readers.hasNext()) {
                    return null;
                }
                ImageReader reader = readers.next();
                reader.setInput(stream, false, false);
                int count = Math.min(reader.getNumImages(true), MAX_FRAMES);
                long budget = 12_000_000L;
                for (int i = 0; i < count; i++) {
                    BufferedImage frame = reader.read(i);
                    if (frame == null) {
                        continue;
                    }
                    if (width == 0) {
                        double scale = Math.min(1.0D, (double) MAX_SIDE / Math.max(frame.getWidth(), frame.getHeight()));
                        width = Math.max(1, (int) Math.round(frame.getWidth() * scale));
                        height = Math.max(1, (int) Math.round(frame.getHeight() * scale));
                    }
                    if ((long) width * height * (frames.size() + 1L) > budget) {
                        break;
                    }
                    frames.add(resize(frame, width, height));
                }
                reader.dispose();
            }
        } catch (Throwable e) {
            return null;
        }
        if (frames.isEmpty()) {
            return null;
        }
        return new Frames(frames, width, height);
    }

    private record Frames(List<int[]> frames, int width, int height) {
    }

    private static int[] resize(BufferedImage source, int width, int height) {
        int[] pixels = new int[width * height];
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        int[] row = new int[sourceWidth];
        for (int y = 0; y < height; y++) {
            int y0 = (int) ((long) y * sourceHeight / height);
            int y1 = Math.max(y0 + 1, (int) ((long) (y + 1) * sourceHeight / height));
            for (int x = 0; x < width; x++) {
                int x0 = (int) ((long) x * sourceWidth / width);
                int x1 = Math.max(x0 + 1, (int) ((long) (x + 1) * sourceWidth / width));
                pixels[y * width + x] = average(source, x0, y0, x1, y1, row);
            }
        }
        return pixels;
    }

    private static int average(BufferedImage source, int x0, int y0, int x1, int y1, int[] row) {
        long r = 0;
        long g = 0;
        long b = 0;
        long a = 0;
        int count = 0;
        for (int y = y0; y < y1; y++) {
            source.getRGB(x0, y, x1 - x0, 1, row, 0, source.getWidth());
            for (int i = 0; i < x1 - x0; i++) {
                int value = row[i];
                a += (value >>> 24) & 0xFF;
                r += (value >>> 16) & 0xFF;
                g += (value >>> 8) & 0xFF;
                b += value & 0xFF;
                count++;
            }
        }
        if (count == 0) {
            return 0;
        }
        return (int) (a / count) << 24 | (int) (r / count) << 16 | (int) (g / count) << 8 | (int) (b / count);
    }

    private static final class VideoTexture {
        final String source;
        final byte[] data;
        final Identifier id;
        final NativeImage image;
        final DynamicTexture texture;
        final List<int[]> frames;
        final int width;
        final int height;
        int lastFrame = -1;

        VideoTexture(String source, byte[] data, Identifier id, NativeImage image, DynamicTexture texture,
                     List<int[]> frames, int width, int height) {
            this.source = source;
            this.data = data;
            this.id = id;
            this.image = image;
            this.texture = texture;
            this.frames = frames;
            this.width = width;
            this.height = height;
        }
    }

    public static class State extends BlockEntityRenderState {
        public BlockPos pos = BlockPos.ZERO;
        public String source = "";
        public byte[] data = new byte[0];
        public Direction facing = Direction.SOUTH;
    }
}
