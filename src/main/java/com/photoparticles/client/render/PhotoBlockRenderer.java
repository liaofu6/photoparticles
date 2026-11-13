package com.photoparticles.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.photoparticles.PhotoParticlesMod;
import com.photoparticles.block.PhotoBlockEntity;
import java.util.HashMap;
import java.util.Map;
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
 * 用一张动态纹理四边形渲染照片，纹理与像素一一对应，边缘不会被粒子柔化。
 */
public class PhotoBlockRenderer implements BlockEntityRenderer<PhotoBlockEntity, PhotoBlockRenderer.State> {
    public static final float PIXEL_SIZE = 1.0F / 144.0F;

    private static Boolean imageSupport;

    /**
     * 部分运行环境（例如安卓启动器）缺少 java.desktop，ImageIO 不可用。
     * 这时只保留方块模型，不绘制照片平面，避免渲染线程抛错。
     */
    public static boolean imageSupportAvailable() {
        if (imageSupport == null) {
            try {
                Class.forName("javax.imageio.ImageIO");
                Class.forName("java.awt.image.BufferedImage");
                imageSupport = Boolean.TRUE;
            } catch (Throwable t) {
                imageSupport = Boolean.FALSE;
                PhotoParticlesMod.LOGGER.warn("Image decoding is unavailable in this runtime, photo blocks will render as plain blocks");
            }
        }
        return imageSupport;
    }

    private static final Map<BlockPos, TextureEntry> TEXTURES = new HashMap<>();

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PhotoBlockEntity blockEntity, State state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderState.extractBase(blockEntity, state, overlay);
        state.pos = blockEntity.getBlockPos();
        state.width = blockEntity.getWidth();
        state.height = blockEntity.getHeight();
        state.pixels = blockEntity.getPixels();
        state.facing = blockEntity.getFacing();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (state.width <= 0 || state.height <= 0 || state.pixels.length != state.width * state.height) {
            return;
        }
        if (!imageSupportAvailable()) {
            return;
        }
        Identifier texture = textureFor(state.pos, state.width, state.height, state.pixels);

        Direction normal = state.facing;
        double rightX = normal.getStepZ();
        double rightZ = -normal.getStepX();
        double planeWidth = state.width * (double) PIXEL_SIZE;
        double planeHeight = state.height * (double) PIXEL_SIZE;
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

        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(texture), (pose, buffer) -> {
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

    private static Identifier textureFor(BlockPos pos, int width, int height, int[] pixels) {
        TextureEntry entry = TEXTURES.get(pos);
        if (entry != null && entry.pixels == pixels) {
            return entry.id;
        }
        Identifier id = entry != null ? entry.id : Identifier.fromNamespaceAndPath("photoparticles",
                "dyn/photo_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ());
        NativeImage image = new NativeImage(width, height, true);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setPixelABGR(x, y, toAbgr(pixels[y * width + x]));
            }
        }
        DynamicTexture texture = new DynamicTexture(() -> "photoparticles photo", image);
        if (entry != null) {
            entry.texture.close();
        }
        Minecraft.getInstance().getTextureManager().register(id, texture);
        TEXTURES.put(pos, new TextureEntry(id, texture, pixels));
        return id;
    }

    public static int toAbgr(int argb) {
        return ((argb >>> 24) & 0xFF) << 24
                | (argb & 0xFF) << 16
                | ((argb >>> 8) & 0xFF) << 8
                | ((argb >>> 16) & 0xFF);
    }

    private record TextureEntry(Identifier id, DynamicTexture texture, int[] pixels) {
    }

    public static class State extends BlockEntityRenderState {
        public BlockPos pos = BlockPos.ZERO;
        public int width;
        public int height;
        public int[] pixels = new int[0];
        public Direction facing = Direction.SOUTH;
    }
}
