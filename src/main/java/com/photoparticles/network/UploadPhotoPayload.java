package com.photoparticles.network;

import com.photoparticles.PhotoParticlesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UploadPhotoPayload(BlockPos pos, int width, int height, int[] pixels) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UploadPhotoPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(PhotoParticlesMod.MOD_ID, "upload_photo"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UploadPhotoPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.pos);
                buf.writeVarInt(payload.width);
                buf.writeVarInt(payload.height);
                buf.writeVarIntArray(payload.pixels);
            },
            buf -> new UploadPhotoPayload(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readVarIntArray())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
