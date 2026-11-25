package io.dwg.entities.concrete;

import io.dwg.core.type.CmColor;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.entities.AbstractDwgObject;
import io.dwg.entities.DwgNonEntityObject;
import io.dwg.entities.DwgObjectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MLINESTYLE object (type 73, spec §20.4.73): the lines of a multiline style.
 */
public class DwgMLineStyle extends AbstractDwgObject implements DwgNonEntityObject {

    /**
     * One line of the style. Up to R2013 the linetype is given by index ({@code linetypeIndex});
     * R2018+ files give its handle ({@code linetypeHandle}) instead.
     */
    public record Line(double offset, CmColor color, int linetypeIndex, DwgHandleRef linetypeHandle) {
        /** This line with the linetype handle filled in. */
        public Line withLinetypeHandle(DwgHandleRef handle) {
            return new Line(offset, color, linetypeIndex, handle);
        }
    }

    private String name = "";
    private String description = "";
    private int flags;
    private CmColor fillColor;
    private double startAngle;
    private double endAngle;
    private final List<Line> lines = new ArrayList<>();

    @Override
    public DwgObjectType objectType() { return DwgObjectType.MLINESTYLE; }

    @Override
    public boolean isEntity() { return false; }

    public String name() { return name; }
    public String description() { return description; }
    /** The flags as DXF group 70 holds them (the file stores them with some bits swapped). */
    public int flags() { return flags; }
    public CmColor fillColor() { return fillColor; }
    public double startAngle() { return startAngle; }
    public double endAngle() { return endAngle; }
    public List<Line> lines() { return Collections.unmodifiableList(lines); }

    public void setName(String name) { this.name = name == null ? "" : name; }
    public void setDescription(String description) { this.description = description == null ? "" : description; }
    public void setFlags(int flags) { this.flags = flags; }
    public void setFillColor(CmColor color) { this.fillColor = color; }
    public void setStartAngle(double angle) { this.startAngle = angle; }
    public void setEndAngle(double angle) { this.endAngle = angle; }
    public void addLine(Line line) { lines.add(line); }
    public void setLine(int index, Line line) { lines.set(index, line); }
}
