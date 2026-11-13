# jDwgParser

A DWG parser written in pure Java. It reads DWG files without any external library and can render a drawing to a PNG image.

The file format follows the [Open Design Specification for .dwg files](https://www.opendesign.com/files/guestdownloads/OpenDesign_Specification_for_.dwg_files.pdf).

## What is supported

Only **R2000 (AC1015)** files can be rendered. Objects of other versions are not read yet.

| Group | Supported |
|---|---|
| Curves | LINE, CIRCLE, ARC, ELLIPSE, SPLINE, LWPOLYLINE, LEADER, POINT |
| Faces | 3DFACE, polyface mesh (edges), SOLID/TRACE (filled) |
| Hatch | HATCH (solid fill, pattern lines) |
| Text | TEXT, MTEXT, ATTRIB |
| References | INSERT (block is expanded), DIMENSION (dimension block is expanded) |
| Appearance | Entity, layer and block colors; frozen or switched-off layers are hidden |

Not drawn yet: 3DSOLID/REGION, old-style POLYLINE, MINSERT, ATTDEF, VIEWPORT, images, linetypes, lineweights, polyline widths.

## Checking the result visually

`run.RenderTest` draws the model space of a DWG file to a PNG. It is not an automated test: you run it and look at the images.

### 1. Prerequisites

- JDK 8 or later (checked with JDK 21)
- DWG files in R2000 format. The repository contains no samples, so put your own files in an `AC1015` folder.
  - The first 6 bytes of a file tell its version. A file that starts with `AC1015` is R2000.

```
jDwgParser/
├─ src/
└─ AC1015/          ← put the .dwg files here
```

### 2. Compile

Run from the project root.

PowerShell:

```powershell
javac -encoding UTF-8 -d bin (Get-ChildItem src -Recurse -Filter *.java).FullName
```

bash:

```bash
javac -encoding UTF-8 -d bin $(find src -name '*.java')
```

In Eclipse, importing the project builds it automatically.

### 3. Render

```
java -cp bin run.RenderTest [file or folder] [output folder]
```

Without arguments it reads every `.dwg` in the `AC1015` folder and writes a PNG of the same name to the `render` folder.

```powershell
java -cp bin run.RenderTest                               # all of AC1015 → render\
java -cp bin run.RenderTest AC1015\boeing_747.dwg         # one file → render\
java -cp bin run.RenderTest AC1015 out                    # all of AC1015 → out\
```

In Eclipse, run `src/run/RenderTest.java` with **Run As → Java Application**. Arguments go in **Run Configurations → Arguments → Program arguments**.

### 4. Reading the output

One summary line is printed per file.

```
boeing_747.dwg : entities=10140, decoded=10136, failed=0, layers=2, blocks=2 | drawn=10136, inserts expanded=0, hidden=0, missing blocks=0 -> boeing_747.png (2000x1936)
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

### 5. When the picture looks wrong

- **Almost empty image**: if `decoded` is much smaller than `entities`, the drawing consists mostly of unsupported types. Use `ObjectStats` below to see which types it contains.
- **Lines are hard to see**: the drawing uses a dark color (color 18, for example). This is common in drawings made for a white background.
- **The drawing is a tiny dot**: a far-away entity or a very large text has stretched the extents.
- **Letter shapes and widths differ from the original**: a system font is used instead of the fonts of the drawing.
- **A dense hatch appears as a translucent area**: when the pattern lines are closer than 3 pixels, a translucent fill is drawn instead of the lines.

## Other tools

These print numbers instead of pictures. Both read the `AC1015` folder when run without arguments.

```
java -cp bin run.ObjectStats [file or folder]
java -cp bin run.EntityStats [file or folder] [number of entities to print]
```

- `run.ObjectStats` counts every object in a file by type. `E` marks an entity, `O` a non-graphical object. `handle mismatches` and `failures` must be 0.
- `run.EntityStats` decodes the supported entities and prints how many succeeded and failed per type, and the extents of the model space. Give a number as the second argument to print the coordinates and properties of that many entities, which you can compare with a DXF of the same drawing.

> `run.ReadTest` is not for rendering. It is a sorting tool that **moves** the files of the `DWG` folder into one folder per version (`AC1015`, `AC1018`, ...). Do not run it if the files must stay where they are.

## Code layout

| Location | Content |
|---|---|
| `src/decode/BitReader.java` | Bit stream reader (compressed data types of DWG) |
| `src/decode/ObjectScannerR2000.java` | Section locators, classes, object map, common object header |
| `src/decode/EntityDecoderR2000.java` | Entity data and handles |
| `src/decode/TableDecoderR2000.java` | BLOCK_HEADER, LAYER |
| `src/decode/DrawingReaderR2000.java` | Entry point that reads one file into a `Drawing` |
| `src/structure/` | Drawing model (`Drawing`, `Layer`, `BlockHeader`, `entity/*`) |
| `src/render/` | PNG renderer |
| `src/run/` | Runnable classes |

To use it from code:

```java
Drawing drawing = DrawingReaderR2000.read(new File("AC1015/boeing_747.dwg"));
BufferedImage image = new Renderer().render(drawing);   // null if there is nothing to draw
ImageIO.write(image, "png", new File("boeing_747.png"));
```
