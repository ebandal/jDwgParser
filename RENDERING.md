# Rendering a drawing to PNG

`io.dwg.render.tool.RenderTest` draws the model space of a DWG file to a PNG, so that the result of parsing can be checked by eye. It is not an automated test: you run it and look at the images.

Only **R2000 (AC1015)** files are supported.

## How this relates to the rest of the library

The `io.dwg.render` packages are self-contained. They read the file with their own R2000 reader (`io.dwg.render.r2000`) into their own model (`io.dwg.render.model`) and do not use `DwgReader` / `DwgDocument`.

| Package | Content |
|---|---|
| `io.dwg.render` | PNG renderer (`Renderer`), curve and text helpers |
| `io.dwg.render.r2000` | Bit stream reader, object map scanner, entity and table decoders, `DrawingReaderR2000` |
| `io.dwg.render.model` | `Drawing`, `Layer`, `BlockHeader`, `entity/*` |
| `io.dwg.render.tool` | Runnable classes: `RenderTest`, `EntityStats`, `ObjectStats` |

## What is drawn

| Group | Supported |
|---|---|
| Curves | LINE, CIRCLE, ARC, ELLIPSE, SPLINE, LWPOLYLINE, LEADER, POINT |
| Faces | 3DFACE, polyface mesh (edges), SOLID/TRACE (filled) |
| Hatch | HATCH (solid fill, pattern lines) |
| Text | TEXT, MTEXT, ATTRIB |
| References | INSERT (block is expanded), DIMENSION (dimension block is expanded) |
| Appearance | Entity, layer and block colors; frozen or switched-off layers are hidden |

Not drawn yet: 3DSOLID/REGION, old-style POLYLINE, MINSERT, ATTDEF, VIEWPORT, images, linetypes, lineweights, polyline widths.

## Running it

Build first:

```bash
mvn compile
```

Then:

```
java -cp target/classes io.dwg.render.tool.RenderTest [file or folder] [output folder]
```

Without arguments it reads every `.dwg` in `samples/2000` and writes a PNG of the same name to `target/render`.

```bash
java -cp target/classes io.dwg.render.tool.RenderTest                             # samples/2000 → target/render
java -cp target/classes io.dwg.render.tool.RenderTest samples/example_2000.dwg    # one file → target/render
java -cp target/classes io.dwg.render.tool.RenderTest my-drawings out             # a folder → out
```

In an IDE, run `src/io/dwg/render/tool/RenderTest.java` as a Java application and pass the arguments as program arguments.

The first 6 bytes of a file tell its version. A file that starts with `AC1015` is R2000.

## Reading the output

One summary line is printed per file.

```
TS1.dwg : entities=133, decoded=88, failed=0, layers=3, blocks=11 | drawn=68, inserts expanded=7, hidden=0, missing blocks=0 -> TS1.png (949x2000)
```

| Field | Meaning |
|---|---|
| `entities` | All entities in the file, supported or not |
| `decoded` | Entities of a supported type that were read |
| `failed` | Entities or table records that failed to decode. **Anything other than 0 is a decoder bug** |
| `drawn` | Shapes actually drawn, after block expansion |
| `inserts expanded` | Block references that were expanded |
| `hidden` | Skipped because they are hidden (invisible entity, frozen or switched-off layer) |
| `missing blocks` | Block references skipped because the block is unknown or has nothing drawable |

`-> nothing to draw` means the file has no entity that can be drawn. `FAILED` means the file could not be read, which is what you get for a file that is not R2000.

The image is a top view. Its longer side is 2000 pixels and the background is black.

A decoded entity counts as read only if its data ends exactly where the object header says the handle stream starts, and its handles end at the end of the object. That check is what `failed` reports.

## When the picture looks wrong

- **Almost empty image**: if `decoded` is much smaller than `entities`, the drawing consists mostly of unsupported types. Use `ObjectStats` below to see which types it contains.
- **Lines are hard to see**: the drawing uses a dark color (color 18, for example). This is common in drawings made for a white background.
- **The drawing is a tiny dot**: a far-away entity or a very large text has stretched the extents.
- **Letter shapes and widths differ from the original**: a system font is used instead of the fonts of the drawing.
- **A dense hatch appears as a translucent area**: when the pattern lines are closer than 3 pixels, a translucent fill is drawn instead of the lines.

## Other tools

These print numbers instead of pictures. Both read `samples/2000` when run without arguments.

```
java -cp target/classes io.dwg.render.tool.ObjectStats [file or folder]
java -cp target/classes io.dwg.render.tool.EntityStats [file or folder] [number of entities to print]
```

- `ObjectStats` counts every object in a file by type. `E` marks an entity, `O` a non-graphical object. `handle mismatches` and `failures` must be 0.
- `EntityStats` decodes the supported entities and prints how many succeeded and failed per type, and the extents of the model space. Give a number as the second argument to print the coordinates and properties of that many entities, which you can compare with a DXF of the same drawing.

## Checking DwgReader against this reader

`ReaderCheck` reads R2000 files with both `DwgReader` and the reader of `io.dwg.render.r2000`, and compares the two object by object, under the same handle.

```
java -cp target/classes io.dwg.render.tool.ReaderCheck [file or folder]...
```

Without arguments it reads `samples/2000`. For each type it prints three numbers:

| Column | Meaning |
|---|---|
| `in files` | Objects of that type in the files |
| `right class` | Objects `DwgReader` returned under the same handle as the matching class |
| `values equal` | Objects whose compared values are equal |

Below the table it lists the first difference of each type. All three numbers of a row should be the same.

What is compared depends on the type: coordinates and sizes for LINE, CIRCLE, ARC, ELLIPSE, POINT, INSERT, SOLID, TRACE, 3DFACE, LWPOLYLINE (vertices) and SPLINE (control points); text, position and height for TEXT and MTEXT; name, color and frozen flag for LAYER; name and base point for BLOCK_HEADER. For ATTRIB, HATCH, DIMENSION, LEADER and polyface meshes no type-specific value is compared.

Every entity is also compared on its common values: entity mode, color, visibility, and the handles of its layer and (inside a block) its owner. INSERT and DIMENSION are compared on the handle of their block too.

The same comparison runs as a test on the R2000 samples of the repository:

```bash
mvn test
```

`test/io/dwg/ReaderAgreementTest.java` fails if any object is missing, is of the wrong class or differs.

## Using it from code

```java
Drawing drawing = DrawingReaderR2000.read(new File("samples/example_2000.dwg"));
BufferedImage image = new Renderer().render(drawing);   // null if there is nothing to draw
ImageIO.write(image, "png", new File("example_2000.png"));
```
