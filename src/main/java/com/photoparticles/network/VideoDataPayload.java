package com.photoparticles.network;

import com.photoparticles.PhotoParticlesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** 服务端把 GIF 数据发给提出请求的客户端，不进方块实体的同步包。 */
public record VideoDataPayload(BlockPos pos, int version, byte[] data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<VideoDataPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(PhotoParticlesMod.MOD_ID, "video_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VideoDataPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.pos);
                buf.writeVarInt(payload.version);
                buf.writeByteArray(payload.data);
            },
            buf -> new VideoDataPayload(buf.readBlockPos(), buf.readVarInt(),
                    buf.readByteArray(VideoSourcePayload.MAX_DATA))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
