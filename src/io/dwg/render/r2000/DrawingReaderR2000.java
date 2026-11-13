package io.dwg.render.r2000;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.dwg.render.model.BlockHeader;
import io.dwg.render.model.Drawing;
import io.dwg.render.model.DwgObject;
import io.dwg.render.model.Layer;
import io.dwg.render.model.entity.Entity;
import io.dwg.render.model.entity.Insert;
import io.dwg.render.model.entity.PolyfaceMesh;
import io.dwg.render.model.entity.PolyfaceVertex;
import io.dwg.render.model.entity.Text;

/** Reads an R2000 (AC1015) file into a {@link Drawing} : layers, blocks and the supported entities. */
public class DrawingReaderR2000 {

    public static Drawing read(File dwgFile) throws IOException, DwgParseException {
        ObjectScannerR2000 scanner = new ObjectScannerR2000();
        scanner.scan(dwgFile);

        Drawing d = new Drawing();
        List<Entity> entities = new ArrayList<>();
        Map<Long, Entity> byHandle = new HashMap<>();
        for (DwgObject o : scanner.objects) {
            if (o.entity) {
                d.entityCount++;
            }
            try {
                if (o.type == TableDecoderR2000.TYPE_BLOCK_HEADER) {
                    BlockHeader b = TableDecoderR2000.decodeBlockHeader(scanner.file, o, scanner.charset);
                    d.blocks.put(o.handle, b);
                } else if (o.type == TableDecoderR2000.TYPE_LAYER) {
                    Layer l = TableDecoderR2000.decodeLayer(scanner.file, o, scanner.charset);
                    d.layers.put(o.handle, l);
                } else {
                    Entity e = EntityDecoderR2000.decode(scanner.file, o, scanner.charset);
                    if (e != null) {
                        d.decodedCount++;
                        entities.add(e);
                        byHandle.put(o.handle, e);
                    }
                }
            } catch (DwgParseException ex) {
                d.failedCount++;
            }
        }

        // Sub-entities belong to the entity that precedes them : attributes to their block reference,
        // vertices to their polyline. When the owner handle names that entity, it is used instead.
        Entity parent = null;
        for (Entity e : entities) {
            boolean attrib = e instanceof Text && ((Text) e).attribute;
            if (attrib || e instanceof PolyfaceVertex) {
                Entity owner = e.entMode == Entity.MODE_OWNED ? byHandle.get(e.ownerHandle) : null;
                if (!(owner instanceof Insert) && !(owner instanceof PolyfaceMesh)) {
                    owner = parent;
                }
                if (attrib && owner instanceof Insert) {
                    ((Insert) owner).attribs.add((Text) e);
                } else if (!attrib && owner instanceof PolyfaceMesh) {
                    PolyfaceVertex v = (PolyfaceVertex) e;
                    if (v.point != null) {
                        ((PolyfaceMesh) owner).vertices.add(v.point);
                    } else {
                        ((PolyfaceMesh) owner).faces.add(v.indexes);
                    }
                }
                continue;
            }
            if (e instanceof Insert || e instanceof PolyfaceMesh) {
                parent = e;
            }
            if (e.entMode == Entity.MODE_MODEL_SPACE) {
                d.modelSpace.add(e);
            } else if (e.entMode == Entity.MODE_OWNED) {
                d.blockEntities.computeIfAbsent(e.ownerHandle, k -> new ArrayList<>()).add(e);
            }
        }

        // entities that name the model space block as their owner belong to the model space too
        for (BlockHeader b : d.blocks.values()) {
            if (b.isModelSpace() && d.blockEntities.containsKey(b.handle)) {
                d.modelSpace.addAll(d.blockEntities.remove(b.handle));
            }
        }
        return d;
    }
}
