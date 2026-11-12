package com.photoparticles.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class PhotoBlockEntity extends BlockEntity {
    public static final int MAX_SIDE = 256;
    private static final int[] EMPTY = new int[0];

    private int width;
    private int height;
    private int[] pixels = EMPTY;
    private int facing = Direction.SOUTH.get2DDataValue();
    private long lastParticleTick = Long.MIN_VALUE;

    public PhotoBlockEntity(BlockPos pos, BlockState state) {
        super(PhotoBlockEntities.PHOTO_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Width", width);
        output.putInt("Height", height);
        output.putIntArray("Pixels", pixels);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        width = input.getIntOr("Width", 0);
        height = input.getIntOr("Height", 0);
        pixels = input.getIntArray("Pixels").orElse(EMPTY);
        facing = input.getIntOr("Facing", Direction.SOUTH.get2DDataValue()) & 3;
        if (width <= 0 || height <= 0 || width > MAX_SIDE || height > MAX_SIDE
                || pixels.length != width * height) {
            width = 0;
            height = 0;
            pixels = EMPTY;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public boolean hasImage() {
        return pixels.length > 0;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int[] getPixels() {
        return pixels;
    }

    public Direction getFacing() {
        return Direction.from2DDataValue(facing);
    }

    public long getLastParticleTick() {
        return lastParticleTick;
    }

    public void setLastParticleTick(long tick) {
        this.lastParticleTick = tick;
    }

    public void setImage(int newWidth, int newHeight, int[] newPixels, Direction newFacing) {
        if (newWidth <= 0 || newHeight <= 0 || newWidth > MAX_SIDE || newHeight > MAX_SIDE
                || newPixels.length != newWidth * newHeight) {
            this.width = 0;
            this.height = 0;
            this.pixels = EMPTY;
        } else {
            this.width = newWidth;
            this.height = newHeight;
            this.pixels = newPixels.clone();
            this.facing = newFacing.get2DDataValue();
        }
        this.lastParticleTick = Long.MIN_VALUE;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
