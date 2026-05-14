package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

public class DwgMaterial extends AbstractDwgEntity {
    private String materialName;
    private String description;
    private double ambientColorRed;
    private double ambientColorGreen;
    private double ambientColorBlue;
    private double diffuseColorRed;
    private double diffuseColorGreen;
    private double diffuseColorBlue;
    private double specularColorRed;
    private double specularColorGreen;
    private double specularColorBlue;
    private double shininess;
    private double opacity;
    private int materialType;

    @Override
    public DwgObjectType objectType() {
        return DwgObjectType.ACAD_MATERIAL;
    }

    public String materialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }

    public String description() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double ambientColorRed() { return ambientColorRed; }
    public void setAmbientColorRed(double ambientColorRed) { this.ambientColorRed = ambientColorRed; }

    public double ambientColorGreen() { return ambientColorGreen; }
    public void setAmbientColorGreen(double ambientColorGreen) { this.ambientColorGreen = ambientColorGreen; }

    public double ambientColorBlue() { return ambientColorBlue; }
    public void setAmbientColorBlue(double ambientColorBlue) { this.ambientColorBlue = ambientColorBlue; }

    public double diffuseColorRed() { return diffuseColorRed; }
    public void setDiffuseColorRed(double diffuseColorRed) { this.diffuseColorRed = diffuseColorRed; }

    public double diffuseColorGreen() { return diffuseColorGreen; }
    public void setDiffuseColorGreen(double diffuseColorGreen) { this.diffuseColorGreen = diffuseColorGreen; }

    public double diffuseColorBlue() { return diffuseColorBlue; }
    public void setDiffuseColorBlue(double diffuseColorBlue) { this.diffuseColorBlue = diffuseColorBlue; }

    public double specularColorRed() { return specularColorRed; }
    public void setSpecularColorRed(double specularColorRed) { this.specularColorRed = specularColorRed; }

    public double specularColorGreen() { return specularColorGreen; }
    public void setSpecularColorGreen(double specularColorGreen) { this.specularColorGreen = specularColorGreen; }

    public double specularColorBlue() { return specularColorBlue; }
    public void setSpecularColorBlue(double specularColorBlue) { this.specularColorBlue = specularColorBlue; }

    public double shininess() { return shininess; }
    public void setShininess(double shininess) { this.shininess = shininess; }

    public double opacity() { return opacity; }
    public void setOpacity(double opacity) { this.opacity = opacity; }

    public int materialType() { return materialType; }
    public void setMaterialType(int materialType) { this.materialType = materialType; }
}
