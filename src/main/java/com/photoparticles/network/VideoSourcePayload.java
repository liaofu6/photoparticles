package com.photoparticles.network;

import com.photoparticles.PhotoParticlesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record VideoSourcePayload(BlockPos pos, String path, byte[] data) implements CustomPacketPayload {
    /** GIF 体积上限，超过这个值会明显拖慢方块同步。 */
    public static final int MAX_DATA = 1024 * 1024;

    public static final CustomPacketPayload.Type<VideoSourcePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(PhotoParticlesMod.MOD_ID, "video_source"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VideoSourcePayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.pos);
                buf.writeUtf(payload.path, 512);
                buf.writeByteArray(payload.data);
            },
            buf -> new VideoSourcePayload(buf.readBlockPos(), buf.readUtf(512), buf.readByteArray(MAX_DATA))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
