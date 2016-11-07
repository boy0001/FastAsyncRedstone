package com.boydti.far;

public class BlockPos {
    public int x,y,z;

    public BlockPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public BlockPos(BlockPos node) {
        this.x = node.x;
        this.y = node.y;
        this.z = node.z;
    }

    public BlockPos() {}

    public final void set(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public final void set(BlockPos node) {
        this.x = node.x;
        this.y = node.y;
        this.z = node.z;
    }

    @Override
    public final int hashCode() {
        return (x ^ (z << 12)) ^ (y << 24);
    }

    public final int getX() {
        return x;
    }

    public final int getY() {
        return y;
    }

    public final int getZ() {
        return z;
    }

    @Override
    public String toString() {
        return x + "," + y + "," + z;
    }

    @Override
    public boolean equals(Object obj) {
        BlockPos other = (BlockPos) obj;
        return other.x == x && other.z == z && other.y == y;
    }
}
