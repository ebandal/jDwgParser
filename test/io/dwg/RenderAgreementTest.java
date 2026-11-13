package io.dwg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.render.DocumentDrawing;
import io.dwg.render.Renderer;
import io.dwg.render.tool.ReaderCheck;
import io.dwg.render.tool.RenderCompare;

/**
 * A drawing rendered from DwgReader must be the same image, pixel for pixel, as the one rendered from the
 * reference reader of io.dwg.render.r2000. This covers what the comparison of values does not :
 * hatches, splines, attributes, polyface meshes, and how entities are grouped into blocks.
 */
class RenderAgreementTest {

    @Test
    void r2000SamplesRenderIdentically() {
        List<File> files = ReaderCheck.collect("samples/2000", "samples/example_2000.dwg", "samples/sample_2000.dwg");
        assertTrue(files.size() >= 24, "R2000 samples are missing : found " + files.size());

        int images = 0;
        for (File f : files) {
            RenderCompare.Result r = RenderCompare.compare(f);
            assertNull(r.error, f.getName());
            assertTrue(r.differentPixels == 0, f.getName() + " : the images differ (" + r.differentPixels
                    + " pixels, drawn " + r.drawn + " vs reference " + r.referenceDrawn + ")");
            if (r.image != null) {
                images++;
            }
        }
        // the comparison is only meaningful if most files actually produce an image
        assertTrue(images >= 15, "only " + images + " files produced an image");
    }

    /** example_2004.dwg is the drawing of example_2000.dwg saved as R2004 : both must render the same. */
    @Test
    void r2004ExampleRendersLikeR2000() throws Exception {
        BufferedImage r2000 = render("samples/example_2000.dwg");
        BufferedImage r2004 = render("samples/example_2004.dwg");

        assertEquals(r2000.getWidth(), r2004.getWidth(), "image width");
        assertEquals(r2000.getHeight(), r2004.getHeight(), "image height");
        long different = 0;
        for (int y = 0; y < r2000.getHeight(); y++) {
            for (int x = 0; x < r2000.getWidth(); x++) {
                if (r2000.getRGB(x, y) != r2004.getRGB(x, y)) {
                    different++;
                }
            }
        }
        assertEquals(0, different, "pixels that differ between the R2000 and the R2004 file");
    }

    private static BufferedImage render(String path) throws Exception {
        DwgDocument doc = DwgReader.defaultReader().open(new File(path).toPath());
        BufferedImage image = new Renderer().render(DocumentDrawing.from(doc));
        assertNotNull(image, path + " : nothing was drawn");
        return image;
    }
}
