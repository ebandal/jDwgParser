package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgMaterial;
import io.dwg.sections.objects.ObjectReader;

public class MaterialObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.ACAD_MATERIAL.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgMaterial material = (DwgMaterial) target;

        // Material name
        material.setMaterialName(r.readText());

        // Description
        material.setDescription(r.readText());

        // Ambient color (RGB)
        material.setAmbientColorRed(r.readBitDouble());
        material.setAmbientColorGreen(r.readBitDouble());
        material.setAmbientColorBlue(r.readBitDouble());

        // Diffuse color (RGB)
        material.setDiffuseColorRed(r.readBitDouble());
        material.setDiffuseColorGreen(r.readBitDouble());
        material.setDiffuseColorBlue(r.readBitDouble());

        // Specular color (RGB)
        material.setSpecularColorRed(r.readBitDouble());
        material.setSpecularColorGreen(r.readBitDouble());
        material.setSpecularColorBlue(r.readBitDouble());

        // Shininess
        material.setShininess(r.readBitDouble());

        // Opacity
        material.setOpacity(r.readBitDouble());

        // Material type
        material.setMaterialType(r.readBitShort());
    }
}
