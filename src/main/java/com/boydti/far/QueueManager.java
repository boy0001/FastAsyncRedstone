package com.boydti.far;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.server.v1_10_R1.Block;
import net.minecraft.server.v1_10_R1.BlockPosition;
import net.minecraft.server.v1_10_R1.BlockPosition.MutableBlockPosition;
import net.minecraft.server.v1_10_R1.Chunk;
import net.minecraft.server.v1_10_R1.EnumSkyBlock;
import net.minecraft.server.v1_10_R1.IBlockData;
import net.minecraft.server.v1_10_R1.World;

import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.example.NMSMappedFaweQueue;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.SetQueue;
import com.boydti.fawe.util.TaskManager;

public class QueueManager {
    private HashMap<World, Map<Long, Character>> blockQueueWorlds = new HashMap<>();
    
    private volatile boolean updateBlocks;
    private final MutableBlockPosition mutable = new BlockPosition.MutableBlockPosition();
    private final Map<World, Map<Long, Map<Short, Object>>> lightQueueWorlds = new ConcurrentHashMap<>(8, 0.9f, 1);

    public QueueManager() {
        TaskManager.IMP.repeat(new Runnable() {
            @Override
            public void run() {
                if (!lightQueueWorlds.isEmpty()) {
                    for (Entry<World, Map<Long, Map<Short, Object>>> entry : lightQueueWorlds.entrySet()) {
                        updateBlockLight(entry.getKey(), entry.getValue());
                    }
                    lightQueueWorlds.clear();
                }
                if (updateBlocks || blockQueueWorlds.isEmpty()) {
                    return;
                }
                updateBlocks = true;
                final HashMap<World, Map<Long, Character>> tmp = blockQueueWorlds;
                blockQueueWorlds = new HashMap<>();
                TaskManager.IMP.async(new Runnable() {
                    @Override
                    public void run() {
                        for (Entry<World, Map<Long, Character>> entry : tmp.entrySet()) {
                            World world = entry.getKey();
                            NMSMappedFaweQueue queue = (NMSMappedFaweQueue) SetQueue.IMP.getNewQueue(world.getWorld().getName(), true, false);
                            for (Entry<Long, Character> entry2 : entry.getValue().entrySet()) {
                                Long pair = entry2.getKey();
                                int cx = MathMan.unpairIntX(pair);
                                int cz = MathMan.unpairIntY(pair);
                                char bitMask = entry2.getValue();
                                CharFaweChunk chunk = (CharFaweChunk) queue.getFaweChunk(cx, cz);
                                chunk.setBitMask(bitMask);
                                queue.refreshChunk(chunk);
                            }
                        }
                        updateBlocks = false;
                    }
                });
            }
        }, 19);
    }
    
    public void updateBlockLight(World world, Map<Long, Map<Short, Object>> map) {
        int size = map.size();
        if (size == 0) {
            return;
        }
        
        Queue<BlockPosition> lightPropagationQueue = new ConcurrentLinkedQueue<>();
        Queue<Object[]> lightRemovalQueue = new ConcurrentLinkedQueue<>();
        Map<BlockPosition, Object> visited = new ConcurrentHashMap<>(8, 0.9f, 1);
        Map<BlockPosition, Object> removalVisited = new ConcurrentHashMap<>(8, 0.9f, 1);
        
        Iterator<Map.Entry<Long, Map<Short, Object>>> iter = map.entrySet().iterator();
        while (iter.hasNext() && size-- > 0) {
            Map.Entry<Long, Map<Short, Object>> entry = iter.next();
            iter.remove();
            long index = entry.getKey();
            Map<Short, Object> blocks = entry.getValue();
            int chunkX = MathMan.unpairIntX(index);
            int chunkZ = MathMan.unpairIntY(index);
            int bx = chunkX << 4;
            int bz = chunkZ << 4;
            Chunk chunk = world.getChunkAt(chunkX, chunkZ);
            if (chunk != null) {
                for (short blockHash : blocks.keySet()) {
                    int hi = (byte) (blockHash >>> 8);
                    int lo = (byte) blockHash;
                    int y = lo & 0xFF;
                    int x = (hi & 0xF) + bx;
                    int z = ((hi >> 4) & 0xF) + bz;
                    int lcx = x & 0xF;
                    int lcz = z & 0xF;
                    mutable.c(lcx, y, lcz);
                    int oldLevel = chunk.getBrightness(EnumSkyBlock.BLOCK, mutable);
                    IBlockData block = chunk.getBlockData(mutable);
                    int newLevel = block.d();
                    if (oldLevel != newLevel) {
                        chunk.a(EnumSkyBlock.BLOCK, mutable, newLevel);
                        if (newLevel < oldLevel) {
                            removalVisited.put(new BlockPosition(x, y, z), present);
                            lightRemovalQueue.add(new Object[] { new BlockPosition(x, y, z), oldLevel });
                        } else {
                            visited.put(new BlockPosition(x, y, z), present);
                            lightPropagationQueue.add(new BlockPosition(x, y, z));
                        }
                    }
                }
            }
        }
        
        while (!lightRemovalQueue.isEmpty()) {
            Object[] val = lightRemovalQueue.poll();
            BlockPosition node = (BlockPosition) val[0];
            int lightLevel = (int) val[1];
            
            this.computeRemoveBlockLight(world, node.getX() - 1, node.getY(), node.getZ(), lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
            this.computeRemoveBlockLight(world, node.getX() + 1, node.getY(), node.getZ(), lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
            this.computeRemoveBlockLight(world, node.getX(), node.getY() - 1, node.getZ(), lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
            this.computeRemoveBlockLight(world, node.getX(), node.getY() + 1, node.getZ(), lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
            this.computeRemoveBlockLight(world, node.getX(), node.getY(), node.getZ() - 1, lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
            this.computeRemoveBlockLight(world, node.getX(), node.getY(), node.getZ() + 1, lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
        }
        
        while (!lightPropagationQueue.isEmpty()) {
            BlockPosition node = lightPropagationQueue.poll();
            int lightLevel = world.b(EnumSkyBlock.BLOCK, node);
            if (lightLevel > 1) {
                this.computeSpreadBlockLight(world, node.getX() - 1, node.getY(), node.getZ(), lightLevel, lightPropagationQueue, visited);
                this.computeSpreadBlockLight(world, node.getX() + 1, node.getY(), node.getZ(), lightLevel, lightPropagationQueue, visited);
                this.computeSpreadBlockLight(world, node.getX(), node.getY() - 1, node.getZ(), lightLevel, lightPropagationQueue, visited);
                this.computeSpreadBlockLight(world, node.getX(), node.getY() + 1, node.getZ(), lightLevel, lightPropagationQueue, visited);
                this.computeSpreadBlockLight(world, node.getX(), node.getY(), node.getZ() - 1, lightLevel, lightPropagationQueue, visited);
                this.computeSpreadBlockLight(world, node.getX(), node.getY(), node.getZ() + 1, lightLevel, lightPropagationQueue, visited);
            }
        }
    }
    
    private void computeRemoveBlockLight(World world, int x, int y, int z, int currentLight, Queue<Object[]> queue, Queue<BlockPosition> spreadQueue, Map<BlockPosition, Object> visited,
    Map<BlockPosition, Object> spreadVisited) {
        int current = world.b(EnumSkyBlock.BLOCK, mutable.c(x, y, z));
        if (current != 0 && current < currentLight) {
            world.a(EnumSkyBlock.BLOCK, mutable, 0);
            if (current > 1) {
                if (!visited.containsKey(mutable)) {
                    BlockPosition index = new BlockPosition(x, y, z);
                    visited.put(index, present);
                    queue.add(new Object[] { index, current });
                }
            }
        } else if (current >= currentLight) {
            if (!spreadVisited.containsKey(mutable)) {
                BlockPosition index = new BlockPosition(x, y, z);
                spreadVisited.put(index, present);
                spreadQueue.add(index);
            }
        }
    }
    
    private void computeSpreadBlockLight(World world, int x, int y, int z, int currentLight, Queue<BlockPosition> queue, Map<BlockPosition, Object> visited) {
        IBlockData block = world.getType(mutable.c(x, y, z));
        currentLight = currentLight - Math.max(1, block.c());
        if (currentLight > 0) {
            int current = world.b(EnumSkyBlock.BLOCK, mutable);
            if (current < currentLight) {
                world.a(EnumSkyBlock.BLOCK, mutable, currentLight);
                
                if (!visited.containsKey(mutable)) {
                    visited.put(new BlockPosition(x, y, z), present);
                    if (currentLight > 1) {
                        queue.add(new BlockPosition(x, y, z));
                    }
                }
            }
        }
    }

    public <T extends Map> T getMap(Map<World, T> worldMap, World world) {
        T map = worldMap.get(world);
        if (map == null) {
            map = (T) new HashMap();
            worldMap.put(world, map);
        }
        return map;
    }
    
    private final Object present = new Object();

    private final short localBlockHash(int x, int y, int z) {
        byte hi = (byte) ((x & 15) + ((z & 15) << 4));
        byte lo = (byte) y;
        return (short) (((hi & 0xFF) << 8) | (lo & 0xFF));
    }

    public void addLightUpdate(World world, int x, int y, int z) {
        long index = MathMan.pairInt(x >> 4, z >> 4);
        Map<Long, Map<Short, Object>> lightQueue = getMap(lightQueueWorlds, world);
        Map<Short, Object> currentMap = lightQueue.get(index);
        if (currentMap == null) {
            currentMap = new ConcurrentHashMap<>(8, 0.9f, 1);
            lightQueue.put(index, currentMap);
        }
        currentMap.put(localBlockHash(x, y, z), present);
    }
    
    public void sendChunk(Map<Long, Character> map, int cx, int cy, int cz) {
        long pair = MathMan.pairInt(cx, cz);
        Character value = map.get(pair);
        if (value == null) {
            value = (char) ((1 << cy));
        } else {
            value = (char) (value | (1 << cy));
        }
        map.put(pair, value);
    }

    public void update(World world, BlockPosition pos, IBlockData state, Block block, boolean light, int physics) {
        Map<Long, Character> map = getMap(blockQueueWorlds, world);
        int x = pos.getX();
        int z = pos.getZ();
        int y = pos.getY();
        int cx = x >> 4;
        int cz = z >> 4;
        int cy = y >> 4;
        sendChunk(map, cx, cy, cz);
        Chunk chunk = world.getChunkAtWorldCoords(pos);
        IBlockData previous = chunk.a(pos, state);
        if (light) {
            int distance = Math.max(state.d(), previous.d());
            for (int cxx = (x - distance) >> 4; cxx <= (x + distance) >> 4; cxx++) {
                for (int czz = (z - distance) >> 4; czz <= (z + distance) >> 4; czz++) {
                    for (int cyy = Math.max(0, (y - distance) >> 4); cyy <= (y + distance) >> 4 && cyy < 16; cyy++) {
                        sendChunk(map, cxx, cyy, czz);
                    }
                }
            }
            addLightUpdate(world, x, pos.getY(), z);
        }
        if (previous != null && physics != 0) {
            //            world.notifyAndUpdatePhysics(pos, chunk, previous, state, physics);
            world.e(pos.north(), block);
            world.e(pos.east(), block);
            world.e(pos.south(), block);
            world.e(pos.west(), block);
            if (pos.getY() > 0) {
                world.e(pos.down(), block);
            }
            if (pos.getY() < 255) {
                world.e(pos.up(), block);
            }
        }
    }
}
