package com.boydti.far;

import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.example.NMSMappedFaweQueue;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.SetQueue;
import com.boydti.fawe.util.TaskManager;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.World;

public class QueueManager {
    private HashMap<World, Map<Long, Character>> blockQueueWorlds = new HashMap<>();
    private Map<World, Map<Long, Map<Short, Object>>> lightQueueWorlds = new ConcurrentHashMap<>(8, 0.9f, 1);

    private volatile boolean updateBlocks;
    public final BlockPos mutableBlockPos = new BlockPos();
    public final Object present = new Object();

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
                            NMSMappedFaweQueue queue = (NMSMappedFaweQueue) SetQueue.IMP.getNewQueue(world.getName(), true, false);
                            Map<Long, Map<Short, Object>> updateLightQueue = updateLight.get(world);
                            if (updateLightQueue != null) {
                                updateBlockLight(queue, updateLightQueue);
                            }
                            queue.startSet(true);
                            for (Entry<Long, Character> entry2 : entry.getValue().entrySet()) {
                                Long pair = entry2.getKey();
                                int cx = MathMan.unpairIntX(pair);
                                int cz = MathMan.unpairIntY(pair);
                                char bitMask = entry2.getValue();
                                CharFaweChunk chunk = (CharFaweChunk) queue.getFaweChunk(cx, cz);
                                chunk.setBitMask(bitMask);
                                try {
                                    queue.refreshChunk(chunk);
                                } catch (Throwable ignore) {}
                            }
                            queue.startSet(false);
                        }
                        updateBlocks = false;
                    }
                });
            }
        }, RedstoneSettings.QUEUE.INTERVAL);
    }
    
    public void updateBlockLight(NMSMappedFaweQueue queue, Map<Long, Map<Short, Object>> map) {
        int size = map.size();
        if (size == 0) {
            return;
        }
        Queue<BlockPos> lightPropagationQueue = new ArrayDeque<>();
        Queue<Object[]> lightRemovalQueue = new ArrayDeque<>();
        Map<BlockPos, Object> visited = new HashMap<>();
        Map<BlockPos, Object> removalVisited = new HashMap<>();

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
                int oldLevel = queue.getEmmittedLight(x, y, z);
                int newLevel = queue.getBrightness(x, y, z);
                if (oldLevel != newLevel) {
                    queue.setBlockLight(x, y, z, newLevel);
                    BlockPos node = new BlockPos(x, y, z);
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
            BlockPos node = (BlockPos) val[0];
            int lightLevel = (int) val[1];
            
            this.computeRemoveBlockLight(queue, node.x - 1, node.y, node.z, lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
            this.computeRemoveBlockLight(queue, node.x + 1, node.y, node.z, lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
            if (node.y > 0) {
                this.computeRemoveBlockLight(queue, node.x, node.y - 1, node.z, lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
            }
            if (node.y < 255) {
                this.computeRemoveBlockLight(queue, node.x, node.y + 1, node.z, lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
            }
            this.computeRemoveBlockLight(queue, node.x, node.y, node.z - 1, lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
            this.computeRemoveBlockLight(queue, node.x, node.y, node.z + 1, lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
        }

        while (!lightPropagationQueue.isEmpty()) {
            BlockPos node = lightPropagationQueue.poll();
            int lightLevel = queue.getEmmittedLight(node.x, node.y, node.z);
            if (lightLevel > 1) {
                this.computeSpreadBlockLight(queue, node.x - 1, node.y, node.z, lightLevel, lightPropagationQueue, visited);
                this.computeSpreadBlockLight(queue, node.x + 1, node.y, node.z, lightLevel, lightPropagationQueue, visited);
                if (node.y > 0) {
                    this.computeSpreadBlockLight(queue, node.x, node.y - 1, node.z, lightLevel, lightPropagationQueue, visited);
                }
                if (node.y < 255) {
                    this.computeSpreadBlockLight(queue, node.x, node.y + 1, node.z, lightLevel, lightPropagationQueue, visited);
                }
                this.computeSpreadBlockLight(queue, node.x, node.y, node.z - 1, lightLevel, lightPropagationQueue, visited);
                this.computeSpreadBlockLight(queue, node.x, node.y, node.z + 1, lightLevel, lightPropagationQueue, visited);
            }
        }
    }

    private void computeRemoveBlockLight(NMSMappedFaweQueue world, int x, int y, int z, int currentLight, Queue<Object[]> queue, Queue<BlockPos> spreadQueue, Map<BlockPos, Object> visited,
    Map<BlockPos, Object> spreadVisited) {
        int current = world.getEmmittedLight(x, y, z);
        if (current != 0 && current < currentLight) {
            world.setBlockLight(x, y, z, 0);
            if (current > 1) {
                mutableBlockPos.set(x, y, z);
                if (!visited.containsKey(mutableBlockPos)) {
                    BlockPos index = new BlockPos(x, y, z);
                    visited.put(index, present);
                    queue.add(new Object[] { index, current });
                }
            }
        } else if (current >= currentLight) {
            mutableBlockPos.set(x, y, z);
            if (!spreadVisited.containsKey(mutableBlockPos)) {
                BlockPos index = new BlockPos(x, y, z);
                spreadVisited.put(index, present);
                spreadQueue.add(index);
            }
        }
    }
    
    private void computeSpreadBlockLight(NMSMappedFaweQueue world, int x, int y, int z, int currentLight, Queue<BlockPos> queue, Map<BlockPos, Object> visited) {
        currentLight = currentLight - Math.max(1, world.getOpacity(x, y, z));
        if (currentLight > 0) {
            int current = world.getEmmittedLight(x, y, z);
            if (current < currentLight) {
                world.setBlockLight(x, y, z, currentLight);
                mutableBlockPos.set(x, y, z);
                if (!visited.containsKey(mutableBlockPos)) {
                    visited.put(new BlockPos(x, y, z), present);
                    if (currentLight > 1) {
                        queue.add(new BlockPos(x, y, z));
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

    public void queueUpdate(World bukkitWorld, int x, int y, int z, int distance) {
        Map<Long, Character> map = getMap(blockQueueWorlds, bukkitWorld);
        int cx = x >> 4;
        int cz = z >> 4;
        int cy = y >> 4;
        sendChunk(map, cx, cy, cz);
        if (distance != 0) {
            for (int cxx = (x - distance) >> 4; cxx <= (x + distance) >> 4; cxx++) {
                for (int czz = (z - distance) >> 4; czz <= (z + distance) >> 4; czz++) {
                    for (int cyy = Math.max(0, (y - distance) >> 4); cyy <= (y + distance) >> 4 && cyy < 16; cyy++) {
                        sendChunk(map, cxx, cyy, czz);
                    }
                }
            }
            addLightUpdate(bukkitWorld, x, y, z);
        }
    }
}
