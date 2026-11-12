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

public class VideoBlockEntity extends BlockEntity {
    private static final int MAX_PATH_LENGTH = 512;

    private String source = "";
    private int facing = Direction.SOUTH.get2DDataValue();

    public VideoBlockEntity(BlockPos pos, BlockState state) {
        super(PhotoBlockEntities.VIDEO_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("Source", source);
        output.putInt("Facing", facing);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        source = input.getStringOr("Source", "");
        facing = input.getIntOr("Facing", Direction.SOUTH.get2DDataValue()) & 3;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public String getSource() {
        return source;
    }

    public Direction getFacing() {
        return Direction.from2DDataValue(facing & 3);
    }

    public void setSource(String path, Direction direction) {
        if (path == null) {
            path = "";
        }
        this.source = path.length() > MAX_PATH_LENGTH ? path.substring(0, MAX_PATH_LENGTH) : path;
        this.facing = direction.get2DDataValue();
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
