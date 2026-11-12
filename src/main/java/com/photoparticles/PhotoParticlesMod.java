package com.photoparticles;

import com.photoparticles.block.PhotoBlock;
import com.photoparticles.block.PhotoBlockEntities;
import com.photoparticles.block.VideoBlock;
import com.photoparticles.network.PhotoNetwork;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhotoParticlesMod implements ModInitializer {
    public static final String MOD_ID = "photoparticles";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Block PHOTO_BLOCK;
    public static Item PHOTO_BLOCK_ITEM;
    public static Block VIDEO_BLOCK;
    public static Item VIDEO_BLOCK_ITEM;

    @Override
    public void onInitialize() {
        PhotoBlock.register();
        VideoBlock.register();
        PhotoBlockEntities.register();
        PhotoNetwork.register();

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS)
                .register(output -> {
                    output.accept(PHOTO_BLOCK_ITEM);
                    output.accept(VIDEO_BLOCK_ITEM);
                });

        LOGGER.info("Photo Particles initialized");
    }
}
