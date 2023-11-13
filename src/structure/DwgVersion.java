package structure;

public enum DwgVersion {
    R13     (1),    // AC1012
    R14     (2),    // AC1014
    R15     (3),
    R2000   (3),    // AC1015
    R2004   (4),    // AC1018
    R2007   (5),    // AC1021
    R2010   (6),    // AC1024
    R2013   (7),    // AC1027
    R2018   (8),    // AC1032
    ;
    
    private int verNum;
    
    DwgVersion(int verNum) {
        this.verNum = verNum;
    }
    
    private DwgVersion(DwgVersion ver) {
        this.verNum = ver.verNum;
    }
    
    public boolean only(DwgVersion ver) {
        if (this.verNum == ver.verNum)
            return true;
        else
            return false;
    }
    
    public boolean between(DwgVersion ver1, DwgVersion ver2) {
        if (this.verNum >= ver1.verNum && this.verNum <= ver2.verNum)
            return true;
        else 
            return false;
    }

    public boolean from(DwgVersion ver) {
        if (this.verNum > ver.verNum)
            return true;
        else 
            return false;
    }
    
    public boolean until(DwgVersion ver) {
        if (this.verNum <= ver.verNum)
            return true;
        else 
            return false;
    }
}
