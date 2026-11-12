package com.photoparticles.block;

import com.photoparticles.PhotoParticlesMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

public final class PhotoBlockEntities {
    private static final ResourceKey<BlockEntityType<?>> PHOTO_KEY = ResourceKey.create(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(PhotoParticlesMod.MOD_ID, "photo_block"));
    private static final ResourceKey<BlockEntityType<?>> VIDEO_KEY = ResourceKey.create(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(PhotoParticlesMod.MOD_ID, "video_block"));

    public static BlockEntityType<PhotoBlockEntity> PHOTO_BLOCK_ENTITY;
    public static BlockEntityType<VideoBlockEntity> VIDEO_BLOCK_ENTITY;

    private PhotoBlockEntities() {
    }

    public static void register() {
        PHOTO_BLOCK_ENTITY = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                PHOTO_KEY,
                new BlockEntityType<>(PhotoBlockEntity::new, Set.of(PhotoParticlesMod.PHOTO_BLOCK)));
        VIDEO_BLOCK_ENTITY = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                VIDEO_KEY,
                new BlockEntityType<>(VideoBlockEntity::new, Set.of(PhotoParticlesMod.VIDEO_BLOCK)));
    }
}
