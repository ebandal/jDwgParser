/**
 * Reading DWG files: start here.
 * <p>{@link io.dwg.api.DwgReader} opens a file (R13 to R2018) and returns a
 * {@link io.dwg.api.DwgDocument}: its objects by handle, typed lists
 * ({@link io.dwg.api.DwgDocument#objects(Class)}), the header variables and the problems met
 * while reading ({@link io.dwg.api.ReadIssue}). A file that cannot be read throws
 * {@link io.dwg.api.DwgReadException}.
 * <pre>{@code
 * DwgDocument doc = DwgReader.defaultReader().open(Paths.get("drawing.dwg"));
 * for (DwgLine line : doc.objects(DwgLine.class)) {
 *     System.out.println(line.start() + " -> " + line.end());
 * }
 * }</pre>
 * <h2>The API</h2>
 * The packages {@code io.dwg.api}, {@code io.dwg.entities}, {@code io.dwg.entities.concrete},
 * {@code io.dwg.core.type}, {@code io.dwg.core.version}, and the types the document returns from
 * {@code io.dwg.sections.header}, {@code io.dwg.sections.classes} and
 * {@code io.dwg.sections.handles}, and {@code io.dwg.render} (drawing a document to an image).
 * The other packages ({@code io.dwg.core.io}, {@code io.dwg.core.util}, {@code io.dwg.format},
 * {@code io.dwg.sections.objects}, the section parsers, {@code io.dwg.render.r2000}) are the
 * reader's internals: they are public for the reader's own use and may change in any release.
 */
package io.dwg.api;
