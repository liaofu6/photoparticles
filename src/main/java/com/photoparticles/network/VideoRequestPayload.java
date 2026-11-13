package com.photoparticles.network;

import com.photoparticles.PhotoParticlesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** 客户端向服务端索要某个视频方块的 GIF 数据。 */
public record VideoRequestPayload(BlockPos pos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<VideoRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(PhotoParticlesMod.MOD_ID, "video_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VideoRequestPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBlockPos(payload.pos),
            buf -> new VideoRequestPayload(buf.readBlockPos())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
