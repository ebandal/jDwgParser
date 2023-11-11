package structure;

public enum DwgVersion {
    R13     (1),
    R14     (2),
    R15     (3),
    R2000   (4),
    ;
    
    private int verNum;
    
    DwgVersion(int verNum) {
        this.verNum = verNum;
    }

}
