package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgVbaProject;
import io.dwg.sections.objects.ObjectReader;

/**
 * VBA_PROJECT 오브젝트 리더 (타입 0x4F)
 * VBA 프로젝트 정보 저장
 */
public class VbaProjectReader implements ObjectReader {
    @Override
    public int objectType() { return DwgObjectType.VBA_PROJECT.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgVbaProject project = (DwgVbaProject) target;
        
        // VBA project-specific data
        // Typically contains VBA source code and metadata
        // For now, store raw bytes without interpretation
        // Further spec research needed for detailed structure
    }
}
