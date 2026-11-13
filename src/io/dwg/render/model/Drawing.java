package io.dwg.render.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.dwg.render.model.entity.Entity;

/** Decoded content of a drawing, as far as the decoders support it. */
public class Drawing {
    public List<Entity> modelSpace = new ArrayList<>();
    /** BLOCK_HEADER handle -> block */
    public Map<Long, BlockHeader> blocks = new HashMap<>();
    /** BLOCK_HEADER handle -> entities owned by the block, in file order */
    public Map<Long, List<Entity>> blockEntities = new HashMap<>();
    /** LAYER handle -> layer */
    public Map<Long, Layer> layers = new HashMap<>();

    public int entityCount;         // all entities in the file, supported or not
    public int decodedCount;        // entities of supported types decoded
    public int failedCount;         // entities or table records that failed to decode

    public List<Entity> entitiesOf(long blockHandle) {
        List<Entity> list = blockEntities.get(blockHandle);
        return list == null ? Collections.emptyList() : list;
    }
}
