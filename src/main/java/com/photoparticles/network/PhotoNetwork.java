package com.photoparticles.network;

import com.photoparticles.block.PhotoBlockEntity;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PhotoNetwork {
    private static final int MAX_PAYLOAD_BYTES = 1 << 21;
    private static final int MAX_VIDEO_BYTES = 12 << 20;
    private static final double MAX_DISTANCE_SQR = 64.0D;
    /** 视频方块可能离玩家很远，索要数据时放宽到 128 格。 */
    private static final double MAX_REQUEST_DISTANCE_SQR = 128.0D * 128.0D;

    private PhotoNetwork() {
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay()
                .registerLarge(UploadPhotoPayload.TYPE, UploadPhotoPayload.CODEC, MAX_PAYLOAD_BYTES);
        PayloadTypeRegistry.serverboundPlay()
                .registerLarge(VideoSourcePayload.TYPE, VideoSourcePayload.CODEC, MAX_VIDEO_BYTES);
        PayloadTypeRegistry.serverboundPlay()
                .register(VideoRequestPayload.TYPE, VideoRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay()
                .registerLarge(VideoDataPayload.TYPE, VideoDataPayload.CODEC, MAX_VIDEO_BYTES);

        ServerPlayNetworking.registerGlobalReceiver(UploadPhotoPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handleUpload(player, payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(VideoSourcePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handleVideoSource(player, payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(VideoRequestPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handleVideoRequest(player, payload));
        });
    }

    private static void handleVideoRequest(ServerPlayer player, VideoRequestPayload payload) {
        if (player.distanceToSqr(
                payload.pos().getX() + 0.5D,
                payload.pos().getY() + 0.5D,
                payload.pos().getZ() + 0.5D) > MAX_REQUEST_DISTANCE_SQR) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
        if (blockEntity instanceof com.photoparticles.block.VideoBlockEntity video && video.hasData()) {
            ServerPlayNetworking.send(player,
                    new VideoDataPayload(payload.pos(), video.getDataVersion(), video.getData()));
        }
    }

    private static void handleVideoSource(ServerPlayer player, VideoSourcePayload payload) {
        if (player.distanceToSqr(
                payload.pos().getX() + 0.5D,
                payload.pos().getY() + 0.5D,
                payload.pos().getZ() + 0.5D) > MAX_DISTANCE_SQR) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
        if (blockEntity instanceof com.photoparticles.block.VideoBlockEntity video) {
            byte[] data = payload.data();
            if (data == null || data.length > VideoSourcePayload.MAX_DATA) {
                return;
            }
            Direction facing = Direction.fromYRot(player.getYRot()).getOpposite();
            video.setSource(payload.path(), facing, data);
        }
    }

    private static void handleUpload(ServerPlayer player, UploadPhotoPayload payload) {
        if (player.distanceToSqr(
                payload.pos().getX() + 0.5D,
                payload.pos().getY() + 0.5D,
                payload.pos().getZ() + 0.5D) > MAX_DISTANCE_SQR) {
            return;
        }
        int width = payload.width();
        int height = payload.height();
        int[] pixels = payload.pixels();
        if (width <= 0 || height <= 0 || width > PhotoBlockEntity.MAX_SIDE || height > PhotoBlockEntity.MAX_SIDE
                || pixels == null || pixels.length != width * height) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
        if (blockEntity instanceof PhotoBlockEntity photo) {
            // 照片朝向玩家：平面法线取玩家视线反方向
            Direction facing = Direction.fromYRot(player.getYRot()).getOpposite();
            photo.setImage(width, height, pixels, facing);
        }
    }
}
