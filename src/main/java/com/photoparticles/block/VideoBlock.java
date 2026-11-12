package com.photoparticles.block;

import com.mojang.serialization.MapCodec;
import com.photoparticles.PhotoParticlesMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.Registry;

public class VideoBlock extends BaseEntityBlock {
    public static final MapCodec<VideoBlock> CODEC = simpleCodec(VideoBlock::new);

    public VideoBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VideoBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        openScreen(level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        openScreen(level, pos);
        return InteractionResult.SUCCESS;
    }

    private static void openScreen(Level level, BlockPos pos) {
        if (level.isClientSide() && FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            com.photoparticles.client.PhotoParticlesClient.openVideoScreen(pos);
        }
    }

    public static void register() {
        Identifier id = Identifier.fromNamespaceAndPath(PhotoParticlesMod.MOD_ID, "video_block");
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);

        PhotoParticlesMod.VIDEO_BLOCK = Registry.register(
                BuiltInRegistries.BLOCK,
                blockKey,
                new VideoBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_MAGENTA)
                        .strength(1.5F)
                        .sound(SoundType.STONE)
                        .setId(blockKey)));

        PhotoParticlesMod.VIDEO_BLOCK_ITEM = Registry.register(
                BuiltInRegistries.ITEM,
                itemKey,
                new BlockItem(PhotoParticlesMod.VIDEO_BLOCK, new Item.Properties()
                        .useBlockDescriptionPrefix()
                        .setId(itemKey)));
    }
}
