package com.boydti.far;

import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.example.NMSMappedFaweQueue;
import com.boydti.fawe.object.IntegerTrio;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.SetQueue;
import com.boydti.fawe.util.TaskManager;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.v1_10_R1.*;
import net.minecraft.server.v1_10_R1.BlockPosition.MutableBlockPosition;

public class QueueManager {
    private HashMap<World, Map<Long, Character>> blockQueueWorlds = new HashMap<>();
    private Map<World, Map<Long, Map<Short, Object>>> lightQueueWorlds = new ConcurrentHashMap<>(8, 0.9f, 1);

    private final MutableBlockPosition mutable = new BlockPosition.MutableBlockPosition();
    private volatile boolean updateBlocks;

    public QueueManager() {
        TaskManager.IMP.repeat(new Runnable() {
            @Override
            public void run() {
                if (updateBlocks || blockQueueWorlds.isEmpty()) {
                    return;
                }
                updateBlocks = true;
                final Map<World, Map<Long, Map<Short, Object>>> updateLight = lightQueueWorlds;
                final HashMap<World, Map<Long, Character>> sendBlocks = blockQueueWorlds;
                blockQueueWorlds = new HashMap<>();
                lightQueueWorlds = new HashMap<>();
                TaskManager.IMP.async(new Runnable() {
                    @Override
                    public void run() {
                        for (Entry<World, Map<Long, Character>> entry : sendBlocks.entrySet()) {
                            World world = entry.getKey();
                            NMSMappedFaweQueue queue = (NMSMappedFaweQueue) SetQueue.IMP.getNewQueue(world.getWorld().getName(), true, false);
                            Map<Long, Map<Short, Object>> updateLightQueue = updateLight.get(world);
                            if (updateLightQueue != null) {
                                updateBlockLight(queue, updateLightQueue);
                            }
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
    
    public void updateBlockLight(NMSMappedFaweQueue queue, Map<Long, Map<Short, Object>> map) {
        int size = map.size();
        if (size == 0) {
            return;
        }
        Queue<IntegerTrio> lightPropagationQueue = new ArrayDeque<>();
        Queue<Object[]> lightRemovalQueue = new ArrayDeque<>();
        Map<IntegerTrio, Object> visited = new HashMap<>();
        Map<IntegerTrio, Object> removalVisited = new HashMap<>();

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
            for (short blockHash : blocks.keySet()) {
                int hi = (byte) (blockHash >>> 8);
                int lo = (byte) blockHash;
                int y = lo & 0xFF;
                int x = (hi & 0xF) + bx;
                int z = ((hi >> 4) & 0xF) + bz;
                int lcx = x & 0xF;
                int lcz = z & 0xF;
                mutable.c(lcx, y, lcz);
                int oldLevel = queue.getEmmittedLight(x, y, z);
                int newLevel = queue.getBrightness(x, y, z);
                if (oldLevel != newLevel) {
                    queue.setBlockLight(x, y, z, newLevel);
                    IntegerTrio node = new IntegerTrio(x, y, z);
                    if (newLevel < oldLevel) {
                        removalVisited.put(node, present);
                        lightRemovalQueue.add(new Object[] { node, oldLevel });
                    } else {
                        visited.put(node, present);
                        lightPropagationQueue.add(node);
                    }
                }
            }
        }
        
        while (!lightRemovalQueue.isEmpty()) {
            Object[] val = lightRemovalQueue.poll();
            IntegerTrio node = (IntegerTrio) val[0];
            int lightLevel = (int) val[1];
            
            this.computeRemoveBlockLight(queue, node.x - 1, node.y, node.z, lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
            this.computeRemoveBlockLight(queue, node.x + 1, node.y, node.z, lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
            this.computeRemoveBlockLight(queue, node.x, node.y - 1, node.z, lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
            this.computeRemoveBlockLight(queue, node.x, node.y + 1, node.z, lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
            this.computeRemoveBlockLight(queue, node.x, node.y, node.z - 1, lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
            this.computeRemoveBlockLight(queue, node.x, node.y, node.z + 1, lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
        }

        while (!lightPropagationQueue.isEmpty()) {
            IntegerTrio node = lightPropagationQueue.poll();
            int lightLevel = queue.getEmmittedLight(node.x, node.y, node.z);
            if (lightLevel > 1) {
                this.computeSpreadBlockLight(queue, node.x - 1, node.y, node.z, lightLevel, lightPropagationQueue, visited);
                this.computeSpreadBlockLight(queue, node.x + 1, node.y, node.z, lightLevel, lightPropagationQueue, visited);
                this.computeSpreadBlockLight(queue, node.x, node.y - 1, node.z, lightLevel, lightPropagationQueue, visited);
                this.computeSpreadBlockLight(queue, node.x, node.y + 1, node.z, lightLevel, lightPropagationQueue, visited);
                this.computeSpreadBlockLight(queue, node.x, node.y, node.z - 1, lightLevel, lightPropagationQueue, visited);
                this.computeSpreadBlockLight(queue, node.x, node.y, node.z + 1, lightLevel, lightPropagationQueue, visited);
            }
        }
    }
    
    private void computeRemoveBlockLight(NMSMappedFaweQueue world, int x, int y, int z, int currentLight, Queue<Object[]> queue, Queue<IntegerTrio> spreadQueue, Map<IntegerTrio, Object> visited,
    Map<IntegerTrio, Object> spreadVisited) {
        int current = world.getEmmittedLight(x, y, z);
        if (current != 0 && current < currentLight) {
            world.setBlockLight(x, y, z, 0);
            if (current > 1) {
                if (!visited.containsKey(mutable)) {
                    IntegerTrio index = new IntegerTrio(x, y, z);
                    visited.put(index, present);
                    queue.add(new Object[] { index, current });
                }
            }
        } else if (current >= currentLight) {
            if (!spreadVisited.containsKey(mutable)) {
                IntegerTrio index = new IntegerTrio(x, y, z);
                spreadVisited.put(index, present);
                spreadQueue.add(index);
            }
        }
    }
    
    private void computeSpreadBlockLight(NMSMappedFaweQueue world, int x, int y, int z, int currentLight, Queue<IntegerTrio> queue, Map<IntegerTrio, Object> visited) {
        
        currentLight = currentLight - Math.max(1, world.getOpacity(x, y, z));
        if (currentLight > 0) {
            int current = world.getEmmittedLight(x, y, z);
            if (current < currentLight) {
                world.setBlockLight(x, y, z, currentLight);
                if (!visited.containsKey(mutable)) {
                    visited.put(new IntegerTrio(x, y, z), present);
                    if (currentLight > 1) {
                        queue.add(new IntegerTrio(x, y, z));
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
