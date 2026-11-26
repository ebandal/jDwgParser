package io.dwg.tika;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Property;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AbstractParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReadException;
import io.dwg.api.DwgReader;

/**
 * An Apache Tika parser for AutoCAD DWG files (R13 to R2018), with jDwgParser.
 * <p>The text of the drawing (TEXT, ATTRIB, MTEXT, table cells, multileaders; see
 * {@link DwgDocument#texts()}) becomes one paragraph per text, so it can be searched and
 * indexed. The metadata gets the DWG version, the number of objects and the number of parts that
 * could not be read ({@code dwg:readIssues}; the rest of a damaged file is still parsed).
 * <p>Registered in {@code META-INF/services/org.apache.tika.parser.Parser}, so Tika's
 * {@code AutoDetectParser} uses it for {@code image/vnd.dwg}. The standard Tika parsers have a
 * DWG parser of their own that reads only the summary information; with both on the class path,
 * configure which one Tika uses (tika-config.xml), or call this parser directly.
 */
public class DwgParser extends AbstractParser {

    private static final long serialVersionUID = 1L;

    /** The media types of DWG files: Tika's own, and the ones other software uses. */
    private static final Set<MediaType> SUPPORTED_TYPES;
    static {
        Set<MediaType> types = new HashSet<>();
        types.add(MediaType.image("vnd.dwg"));
        types.add(MediaType.image("x-dwg"));
        types.add(MediaType.application("acad"));
        types.add(MediaType.application("x-acad"));
        types.add(MediaType.application("x-autocad"));
        types.add(MediaType.application("dwg"));
        SUPPORTED_TYPES = Collections.unmodifiableSet(types);
    }

    public static final Property VERSION = Property.externalText("dwg:version");
    public static final Property OBJECTS = Property.externalInteger("dwg:objects");
    public static final Property READ_ISSUES = Property.externalInteger("dwg:readIssues");

    /** The most bytes one parse may allocate for the sections of a file. */
    private long maxBytes = 512L << 20;

    @Override
    public Set<MediaType> getSupportedTypes(ParseContext context) {
        return SUPPORTED_TYPES;
    }

    /** The most bytes one parse may allocate for the sections of a file (default 512 MB). */
    public void setMaxBytes(long maxBytes) {
        this.maxBytes = maxBytes;
    }

    @Override
    public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context)
            throws IOException, SAXException, TikaException {
        DwgDocument doc;
        try {
            doc = DwgReader.defaultReader().maxBytes(maxBytes).open(stream);
        } catch (DwgReadException e) {
            throw new TikaException("cannot read the DWG file: " + e.getMessage(), e);
        }
        metadata.set(Metadata.CONTENT_TYPE, MediaType.image("vnd.dwg").toString());
        metadata.set(VERSION, doc.version().name());
        metadata.set(OBJECTS, doc.objectMap().size());
        metadata.set(READ_ISSUES, doc.issues().size());
        metadata.set(TikaCoreProperties.FORMAT, "AutoCAD DWG " + doc.version().name());

        XHTMLContentHandler xhtml = new XHTMLContentHandler(handler, metadata);
        xhtml.startDocument();
        for (String text : doc.texts()) {
            xhtml.element("p", text);
        }
        xhtml.endDocument();
    }
}
