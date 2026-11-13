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
    private static final int[] EMPTY_INTS = new int[0];
    private static final byte[] EMPTY_DATA = new byte[0];

    private String source = "";
    private byte[] data = EMPTY_DATA;
    private int dataVersion;
    private int facing = Direction.SOUTH.get2DDataValue();

    public VideoBlockEntity(BlockPos pos, BlockState state) {
        super(PhotoBlockEntities.VIDEO_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("Source", source);
        output.putInt("Facing", facing);
        output.putInt("DataVersion", dataVersion);
        output.putInt("DataLength", data.length);
        output.putIntArray("Data", pack(data));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        source = input.getStringOr("Source", "");
        facing = input.getIntOr("Facing", Direction.SOUTH.get2DDataValue()) & 3;
        dataVersion = input.getIntOr("DataVersion", 0);
        data = unpack(input.getIntArray("Data").orElse(EMPTY_INTS), input.getIntOr("DataLength", 0));
    }

    /**
     * 同步包只带元信息，GIF 本体由客户端按需单独请求，
     * 避免几 MB 的数据跟着方块更新包反复发送。
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        tag.remove("Data");
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public String getSource() {
        return source;
    }

    public byte[] getData() {
        return data;
    }

    public int getDataVersion() {
        return dataVersion;
    }

    public boolean hasData() {
        return data.length > 0;
    }

    public Direction getFacing() {
        return Direction.from2DDataValue(facing & 3);
    }

    public void setSource(String path, Direction direction, byte[] gifData) {
        if (path == null) {
            path = "";
        }
        this.source = path.length() > MAX_PATH_LENGTH ? path.substring(0, MAX_PATH_LENGTH) : path;
        this.data = gifData == null ? EMPTY_DATA : gifData.clone();
        this.dataVersion = level != null && level.getRandom() != null
                ? level.getRandom().nextInt()
                : (int) System.nanoTime();
        this.facing = direction.get2DDataValue();
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private static int[] pack(byte[] bytes) {
        int[] ints = new int[(bytes.length + 3) / 4];
        for (int i = 0; i < bytes.length; i++) {
            ints[i >> 2] |= (bytes[i] & 0xFF) << ((i & 3) * 8);
        }
        return ints;
    }

    private static byte[] unpack(int[] ints, int length) {
        if (length <= 0 || length > ints.length * 4) {
            return EMPTY_DATA;
        }
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) (ints[i >> 2] >>> ((i & 3) * 8));
        }
        return bytes;
    }
}
