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
    private static final int MAX_SIDE = 160;
    private static final int MAX_FRAMES = 100;
    private static final int TICKS_PER_FRAME = 2;
    private static final Map<BlockPos, VideoTexture> CACHE = new HashMap<>();

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
        if (video == null || !video.source.equals(state.source)) {
            video = load(state.pos, state.source);
            if (video == null) {
                return;
            }
            CACHE.put(state.pos, video);
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

    private static VideoTexture load(BlockPos pos, String path) {
        List<int[]> frames = new ArrayList<>();
        int width = 0;
        int height = 0;
        try (ImageInputStream stream = ImageIO.createImageInputStream(new File(path))) {
            if (stream == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            reader.setInput(stream, false, false);
            int count = Math.min(reader.getNumImages(true), MAX_FRAMES);
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
                frames.add(resize(frame, width, height));
            }
            reader.dispose();
        } catch (Throwable e) {
            return null;
        }
        if (frames.isEmpty()) {
            return null;
        }
        Identifier id = Identifier.fromNamespaceAndPath("photoparticles",
                "dyn/video_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ());
        NativeImage image = new NativeImage(width, height, true);
        DynamicTexture texture = new DynamicTexture(() -> "photoparticles video", image);
        Minecraft.getInstance().getTextureManager().register(id, texture);
        return new VideoTexture(path, id, image, texture, frames, width, height);
    }

    private static int[] resize(BufferedImage source, int width, int height) {
        int[] pixels = new int[width * height];
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        for (int y = 0; y < height; y++) {
            int sourceY = Math.min(sourceHeight - 1, y * sourceHeight / height);
            for (int x = 0; x < width; x++) {
                int sourceX = Math.min(sourceWidth - 1, x * sourceWidth / width);
                pixels[y * width + x] = source.getRGB(sourceX, sourceY);
            }
        }
        return pixels;
    }

    private static final class VideoTexture {
        final String source;
        final Identifier id;
        final NativeImage image;
        final DynamicTexture texture;
        final List<int[]> frames;
        final int width;
        final int height;
        int lastFrame = -1;

        VideoTexture(String source, Identifier id, NativeImage image, DynamicTexture texture,
                     List<int[]> frames, int width, int height) {
            this.source = source;
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
        public Direction facing = Direction.SOUTH;
    }
}
