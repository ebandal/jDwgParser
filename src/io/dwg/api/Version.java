package io.dwg.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The version of this library, as the build wrote it into the jar ({@code io/dwg/version.properties}).
 * The version follows VERSIONING.md: {@code MAJOR.MINOR.PATCH}, with {@code -SNAPSHOT} for a
 * build between releases.
 */
public final class Version {
    private static final String NUMBER;
    private static final String BUILT;

    static {
        Properties p = new Properties();
        try (InputStream in = Version.class.getResourceAsStream("/io/dwg/version.properties")) {
            if (in != null) {
                p.load(in);
            }
        } catch (IOException e) {
            // left unknown
        }
        NUMBER = known(p.getProperty("version"));
        BUILT = known(p.getProperty("built"));
    }

    private Version() {
    }

    /** The version, like {@code 0.1.0}; {@code unknown} when not built by Maven. */
    public static String number() {
        return NUMBER;
    }

    /** When the jar was built, like {@code 2026-11-26 08:30 UTC}; {@code unknown} when not built by Maven. */
    public static String built() {
        return BUILT;
    }

    /** Whether this is a build between releases. */
    public static boolean isSnapshot() {
        return NUMBER.endsWith("-SNAPSHOT");
    }

    /**
     * One line for bug reports: the version, the build time, the Java and the system, like
     * {@code jDwgParser 0.1.0 (built 2026-11-26 08:30 UTC; Java 21.0.2; Windows 11 10.0 amd64)}.
     */
    public static String describe() {
        return "jDwgParser " + NUMBER + " (built " + BUILT + "; Java " + System.getProperty("java.version")
                + "; " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " "
                + System.getProperty("os.arch") + ")";
    }

    /** A value Maven did not fill in (the file read from the sources) is unknown. */
    private static String known(String v) {
        return v == null || v.isEmpty() || v.contains("${") ? "unknown" : v;
    }
}
