package io.dwg.format.common;

import java.util.Optional;

/**
 * 알려진 DWG 섹션 이름 상수
 */
public enum SectionType {
    HEADER("AcDb:Header"),
    CLASSES("AcDb:Classes"),
    HANDLES("AcDb:Handles"),
    OBJECTS("AcDb:AcDbObjects"),
    SUMMARY_INFO("AcDb:SummaryInfo"),
    PREVIEW("AcDb:Preview"),
    APP_INFO("AcDb:AppInfo"),
    FILE_DEP_LIST("AcDb:FileDepList"),
    REV_HISTORY("AcDb:RevHistory"),
    SECURITY("AcDb:Security"),
    OBJ_FREE_SPACE("AcDb:ObjFreeSpace"),
    TEMPLATE("AcDb:Template"),
    AUX_HEADER("AcDb:AuxHeader");

    private final String sectionName;

    SectionType(String sectionName) {
        this.sectionName = sectionName;
    }

    public String sectionName() {
        return sectionName;
    }

    public static Optional<SectionType> fromName(String name) {
        for (SectionType type : values()) {
            if (type.sectionName.equals(name)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
