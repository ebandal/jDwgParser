package io.dwg.api;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ReadLimits;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.core.version.DwgVersionDetector;
import io.dwg.format.common.DwgFileStructureHandler;
import io.dwg.format.common.DwgFileStructureHandlerFactory;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.entities.AbstractDwgAcisEntity;
import io.dwg.sections.classes.ClassesSectionParser;
import io.dwg.sections.datastorage.DataStorageSectionParser;
import io.dwg.sections.classes.DwgClassDefinition;
import io.dwg.sections.classes.DwgClassRegistry;
import io.dwg.sections.handles.HandleRegistry;
import io.dwg.sections.handles.HandlesSectionParser;
import io.dwg.sections.header.HeaderSectionParser;
import io.dwg.sections.header.HeaderVariables;
import io.dwg.sections.objects.ObjectsSectionParser;
import io.dwg.entities.DwgObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * DWG 파일 읽기 진입점. Fluent Builder 패턴.
 */
public class DwgReader {

    /** At most this many issues are kept in a document; the listener sees all of them. */
    static final int MAX_KEPT_ISSUES = 10_000;

    private Consumer<ReadIssue> issueListener;
    private long maxBytes = Runtime.getRuntime().maxMemory() / 2;
    private long timeoutMillis;

    private DwgReader() {}

    public static DwgReader defaultReader() {
        return new DwgReader();
    }

    /**
     * Calls {@code listener} for each problem met while reading (see {@link ReadIssue}). The
     * issues are also kept in the document, {@link DwgDocument#issues()}.
     */
    public DwgReader onIssue(Consumer<ReadIssue> listener) {
        this.issueListener = listener;
        return this;
    }

    /**
     * The most bytes one read may allocate for the sections of the file (decompressed data and
     * the like), in all. The default is half of the JVM's maximum heap. A file that would need
     * more fails with a {@link DwgReadException}, instead of exhausting the memory.
     */
    public DwgReader maxBytes(long bytes) {
        if (bytes <= 0) {
            throw new IllegalArgumentException("maxBytes must be positive: " + bytes);
        }
        this.maxBytes = bytes;
        return this;
    }

    /** The longest one read may take; 0 (the default) means no limit. */
    public DwgReader timeout(java.time.Duration timeout) {
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("negative timeout: " + timeout);
        }
        this.timeoutMillis = timeout.toMillis();
        return this;
    }

    /** 파일 경로에서 DWG 파일 읽기 */
    public DwgDocument open(Path filePath) throws IOException {
        byte[] bytes = Files.readAllBytes(filePath);
        return open(bytes);
    }

    /** 스트림에서 DWG 파일 읽기 (전체 메모리 로드) */
    public DwgDocument open(InputStream stream) throws IOException {
        byte[] bytes = stream.readAllBytes();
        return open(bytes);
    }

    /**
     * Reads a DWG file. Damage inside its sections and objects is reported as {@link ReadIssue}s
     * and the rest is read; a file that cannot be read at all throws.
     *
     * @throws DwgReadException if the data is not a DWG file of a supported version, its file
     *     header or section structure cannot be read, or the read goes past a limit
     *     ({@link #maxBytes}, {@link #timeout})
     */
    public DwgDocument open(byte[] data) throws DwgReadException {
        ReadLimits.begin(maxBytes, timeoutMillis);
        try {
            return read(data);
        } catch (DwgReadException e) {
            throw e;
        } catch (ReadLimits.LimitExceededException e) {
            throw new DwgReadException(e.getMessage(), e);
        } catch (Exception | StackOverflowError e) {
            throw new DwgReadException("cannot read the file: " + e, e);
        } finally {
            ReadLimits.end();
        }
    }

    private DwgDocument read(byte[] data) throws Exception {
        // ① 버전 감지
        DwgVersion version;
        try {
            version = DwgVersionDetector.detect(data);
        } catch (RuntimeException e) {
            throw new DwgReadException("not a DWG file of a supported version (R13 to R2018): " + e.getMessage(), e);
        }
        DwgDocument doc = new DwgDocument(version);
        List<ReadIssue> kept = new ArrayList<>();
        Consumer<ReadIssue> issues = issue -> {
            if (kept.size() < MAX_KEPT_ISSUES) {
                kept.add(issue);
            }
            if (issueListener != null) {
                issueListener.accept(issue);
            }
        };
        doc.setIssues(kept);

        // ② 포맷 핸들러 선택
        DwgFileStructureHandler handler = DwgFileStructureHandlerFactory.forVersion(version);
        BitInput input = new ByteBufferBitInput(data);

        // ③ 헤더 파싱
        FileHeaderFields headerFields;
        try {
            headerFields = handler.readHeader(input);
        } catch (ReadLimits.LimitExceededException e) {
            throw e;
        } catch (Exception e) {
            throw new DwgReadException("cannot read the file header: " + e, e);
        }

        // ④ 섹션 추출
        input = new ByteBufferBitInput(data); // 처음부터 다시
        Map<String, SectionInputStream> sections;
        try {
            sections = handler.readSections(input, headerFields);
        } catch (ReadLimits.LimitExceededException e) {
            throw e;
        } catch (Exception e) {
            throw new DwgReadException("cannot read the sections of the file: " + e, e);
        }
        ReadLimits.checkTime();

        // ⑤ Header 섹션 파싱
        SectionInputStream headerStream = sections.get("AcDb:Header");
        if (headerStream != null) {
            try {
                HeaderSectionParser headerParser = new HeaderSectionParser();
                headerParser.setIssueListener(issues);
                HeaderVariables vars = headerParser.parse(headerStream, version);
                doc.setHeaderVariables(vars);
            } catch (ReadLimits.LimitExceededException e) {
                throw e;
            } catch (Exception e) {
                issues.accept(ReadIssue.section("cannot read AcDb:Header: " + e));
            }
        }

        // ⑥ Classes 섹션 파싱
        DwgClassRegistry classRegistry = new DwgClassRegistry();
        SectionInputStream classStream = sections.get("AcDb:Classes");
        if (classStream != null) {
            try {
                ClassesSectionParser classesParser = new ClassesSectionParser();
                classesParser.setIssueListener(issues);
                classesParser.setMaintenanceVersion(headerFields.maintenanceVersion());
                classesParser.setTextCharset(
                    io.dwg.core.util.DwgStringDecoder.charsetForDwgCodePage(headerFields.codePage()));
                List<DwgClassDefinition> classes = classesParser.parse(classStream, version);
                doc.setCustomClasses(classes);
                classes.forEach(classRegistry::register);
            } catch (ReadLimits.LimitExceededException e) {
                throw e;
            } catch (Exception e) {
                issues.accept(ReadIssue.section("cannot read AcDb:Classes; objects of custom classes are UNKNOWN: " + e));
            }
        }
        doc.setClassRegistry(classRegistry);

        // ⑦ Handles 섹션 파싱
        HandleRegistry handleRegistry = new HandleRegistry();
        SectionInputStream handlesStream = sections.get("AcDb:Handles");
        if (handlesStream != null) {
            try {
                HandlesSectionParser handlesParser = new HandlesSectionParser();
                handlesParser.setIssueListener(issues);
                handleRegistry = handlesParser.parse(handlesStream, version);
            } catch (ReadLimits.LimitExceededException e) {
                throw e;
            } catch (Exception e) {
                issues.accept(ReadIssue.section("cannot read AcDb:Handles; no objects can be found: " + e));
            }
        }
        doc.setHandleRegistry(handleRegistry);

        // ⑧ Objects 섹션 파싱
        // Try both naming conventions (R2007+ uses AcDb:AcDbObjects, R2004 uses AcDb:Objects)
        SectionInputStream objectsStream = sections.get("AcDb:AcDbObjects");
        if (objectsStream == null) {
            objectsStream = sections.get("AcDb:Objects");
        }
        // R13/R14/R2000: Objects are not a separate named section.
        // They are located via offsets in the Handles section.
        // Use the whole file as the pseudo-section so HandleRegistry offsets resolve correctly.
        boolean isPreR2004 = version == DwgVersion.R13 || version == DwgVersion.R14
                          || version == DwgVersion.R2000;
        if (objectsStream == null && isPreR2004 && !handleRegistry.allHandles().isEmpty()) {
            objectsStream = new SectionInputStream(data, "AcDb:AcDbObjects");
        }
        if (objectsStream != null) {
            ObjectsSectionParser objParser = new ObjectsSectionParser();
            objParser.setIssueListener(issues);
            objParser.setHandleRegistry(handleRegistry);
            objParser.setClassRegistry(classRegistry);
            objParser.setTextCharset(
                io.dwg.core.util.DwgStringDecoder.charsetForDwgCodePage(headerFields.codePage()));
            try {
                Map<Long, DwgObject> objectMap = objParser.parse(objectsStream, version);
                doc.setObjectMap(objectMap);
            } catch (ReadLimits.LimitExceededException e) {
                throw e;
            } catch (Exception e) {
                issues.accept(ReadIssue.section("cannot read the objects: " + e));
            }
        }

        // R2013+: the data storage section holds the ACIS models of REGION and 3DSOLID
        SectionInputStream dataStorage = sections.get("AcDb:AcDsPrototype_1b");
        if (dataStorage != null) {
            try {
                Map<Long, byte[]> records = DataStorageSectionParser.parse(dataStorage.rawBytes());
                doc.setDataStorage(records);
                attachAcisModels(doc, records);
            } catch (ReadLimits.LimitExceededException e) {
                throw e;
            } catch (Exception e) {
                issues.accept(ReadIssue.section("cannot read AcDb:AcDsPrototype_1b; R2013+ ACIS models are missing: " + e));
            }
        }

        return doc;
    }

    /** Gives the R2013+ ACIS entities whose ACIS empty bit is set their model from the data storage. */
    private static void attachAcisModels(DwgDocument doc, Map<Long, byte[]> records) {
        for (Map.Entry<Long, byte[]> record : records.entrySet()) {
            DwgObject o = doc.objectMap().get(record.getKey());
            if (!(o instanceof AbstractDwgAcisEntity) || !((AbstractDwgAcisEntity) o).isEmpty()) {
                continue;
            }
            AbstractDwgAcisEntity e = (AbstractDwgAcisEntity) o;
            byte[] data = record.getValue();
            e.setAcisVersion(2);
            if (startsWith(data, "ACIS BinaryFile") || startsWith(data, "ASM BinaryFile")) {
                e.setSabData(data);
            } else {
                e.setSatText(new String(data, java.nio.charset.StandardCharsets.ISO_8859_1));
            }
        }
    }

    private static boolean startsWith(byte[] data, String prefix) {
        byte[] p = prefix.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        return data.length >= p.length && java.util.Arrays.equals(data, 0, p.length, p, 0, p.length);
    }

    /** 버전만 빠르게 감지 */
    public DwgVersion detectVersion(Path filePath) throws Exception {
        byte[] header = new byte[6];
        try (var stream = Files.newInputStream(filePath)) {
            stream.read(header);
        }
        return DwgVersionDetector.detect(header);
    }
}
