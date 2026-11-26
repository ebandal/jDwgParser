package io.dwg.api;

import java.io.IOException;

/**
 * A file that cannot be read: it is not a DWG file of a supported version, its file header or
 * section structure is damaged, or reading it went past a limit set on the {@link DwgReader}.
 * Damage inside sections and objects does not throw: it is reported as {@link ReadIssue}s.
 */
public class DwgReadException extends IOException {

    public DwgReadException(String message) {
        super(message);
    }

    public DwgReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
