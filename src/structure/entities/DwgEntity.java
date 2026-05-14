package structure.entities;

import structure.CmColor;

public interface DwgEntity extends DwgObject {
    DwgHandleRef layerHandle();
    DwgHandleRef lineTypeHandle();
    double lineTypeScale();
    CmColor color();
    int invisibility();
    int entityMode();
    double lineWeight();
}
