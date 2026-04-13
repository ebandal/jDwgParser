package io.dwg.sections.header;

import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point2D;
import io.dwg.core.type.Point3D;

import java.util.HashMap;
import java.util.Map;

/**
 * 스펙 §9의 모든 헤더 변수를 담는 컨테이너.
 */
public class HeaderVariables {
    // 주요 필드
    private DwgHandleRef currentLayer;
    private String currentLineType = "BYLAYER";
    private double dimscale = 1.0;
    private double ltscale  = 1.0;
    private Point3D insBase = Point3D.ORIGIN;
    private Point3D extMin  = Point3D.ORIGIN;
    private Point3D extMax  = Point3D.ORIGIN;
    private Point2D limMin  = new Point2D(0.0, 0.0);
    private Point2D limMax  = new Point2D(12.0, 9.0);
    private int lunits  = 2;
    private int luprec  = 4;
    private boolean attmode;
    private boolean blipmode;
    private DwgHandleRef dimstyle;
    private String acadVer = "";
    private int angbase;
    private int angdir;

    // 나머지 변수들은 generic map에 보관
    private final Map<String, Object> vars = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T get(String varName) {
        return (T) vars.getOrDefault(varName, getBuiltin(varName));
    }

    private Object getBuiltin(String name) {
        return switch (name) {
            case "DIMSCALE"  -> dimscale;
            case "LTSCALE"   -> ltscale;
            case "INSBASE"   -> insBase;
            case "EXTMIN"    -> extMin;
            case "EXTMAX"    -> extMax;
            case "LIMMIN"    -> limMin;
            case "LIMMAX"    -> limMax;
            case "LUNITS"    -> lunits;
            case "LUPREC"    -> luprec;
            case "CLAYER"    -> currentLayer;
            case "CELTYPE"   -> currentLineType;
            case "DIMSTYLE"  -> dimstyle;
            default          -> null;
        };
    }

    public void set(String varName, Object value) {
        vars.put(varName, value);
        // 알려진 필드는 직접 갱신
        switch (varName) {
            case "DIMSCALE" -> dimscale = (Double) value;
            case "LTSCALE"  -> ltscale  = (Double) value;
            case "INSBASE"  -> insBase  = (Point3D) value;
            case "EXTMIN"   -> extMin   = (Point3D) value;
            case "EXTMAX"   -> extMax   = (Point3D) value;
            case "LIMMIN"   -> limMin   = (Point2D) value;
            case "LIMMAX"   -> limMax   = (Point2D) value;
            case "LUNITS"   -> lunits   = (Integer) value;
            case "LUPREC"   -> luprec   = (Integer) value;
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> all = new HashMap<>(vars);
        all.put("DIMSCALE", dimscale);
        all.put("LTSCALE", ltscale);
        all.put("INSBASE", insBase);
        all.put("EXTMIN", extMin);
        all.put("EXTMAX", extMax);
        all.put("LIMMIN", limMin);
        all.put("LIMMAX", limMax);
        all.put("LUNITS", lunits);
        all.put("LUPREC", luprec);
        return all;
    }

    // Getters / setters for primary fields
    public DwgHandleRef currentLayer() { return currentLayer; }
    public void setCurrentLayer(DwgHandleRef h) { this.currentLayer = h; }
    public String currentLineType() { return currentLineType; }
    public void setCurrentLineType(String s) { this.currentLineType = s; }
    public double dimscale() { return dimscale; }
    public void setDimscale(double v) { this.dimscale = v; }
    public double ltscale() { return ltscale; }
    public void setLtscale(double v) { this.ltscale = v; }
    public Point3D insBase() { return insBase; }
    public void setInsBase(Point3D p) { this.insBase = p; }
    public Point3D extMin() { return extMin; }
    public void setExtMin(Point3D p) { this.extMin = p; }
    public Point3D extMax() { return extMax; }
    public void setExtMax(Point3D p) { this.extMax = p; }
    public Point2D limMin() { return limMin; }
    public void setLimMin(Point2D p) { this.limMin = p; }
    public Point2D limMax() { return limMax; }
    public void setLimMax(Point2D p) { this.limMax = p; }
    public int lunits() { return lunits; }
    public void setLunits(int v) { this.lunits = v; }
    public int luprec() { return luprec; }
    public void setLuprec(int v) { this.luprec = v; }
    public boolean attmode() { return attmode; }
    public void setAttmode(boolean v) { this.attmode = v; }
    public boolean blipmode() { return blipmode; }
    public void setBlipmode(boolean v) { this.blipmode = v; }
    public DwgHandleRef dimstyle() { return dimstyle; }
    public void setDimstyle(DwgHandleRef h) { this.dimstyle = h; }
    public String acadVer() { return acadVer; }
    public void setAcadVer(String v) { this.acadVer = v; }
    public int angbase() { return angbase; }
    public void setAngbase(int v) { this.angbase = v; }
    public int angdir() { return angdir; }
    public void setAngdir(int v) { this.angdir = v; }
}
