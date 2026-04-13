# dwg-entities — 클래스 명세

> 도메인 객체 모델. 스펙 §20 AcDbObjects의 모든 엔티티·비엔티티 객체 표현.  
> 이 모듈은 파싱 로직을 갖지 않는다. 순수 데이터 모델 역할만 한다.

---

## 패키지: `io.dwg.entities`

---

### `DwgObject` *(interface)*

모든 DWG 객체(엔티티 + 비엔티티)의 최상위 계약.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `long handle()` | 이 객체의 핸들 값 |
| `DwgObjectType objectType()` | 객체 타입 열거 반환 |
| `int rawTypeCode()` | 파일에서 읽은 원시 타입 코드 (커스텀 타입 지원) |
| `DwgHandleRef ownerHandle()` | 소유자 핸들 참조 |
| `List<DwgHandleRef> reactorHandles()` | reactor 핸들 목록 |
| `Optional<DwgHandleRef> xDicHandle()` | 확장 딕셔너리 핸들 |
| `List<XDataRecord> xData()` | 확장 엔티티 데이터 목록 |
| `boolean isEntity()` | 도면에 그려지는 엔티티 여부 |

---

### `DwgEntity` *(interface)* — `DwgObject` 상속

도면에 실제로 그려지는 엔티티의 추가 계약.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `DwgHandleRef layerHandle()` | 소속 레이어 핸들 |
| `DwgHandleRef lineTypeHandle()` | 선종류 핸들. null이면 ByLayer |
| `double lineTypeScale()` | 선종류 스케일 |
| `CmColor color()` | 색상 |
| `int invisibility()` | 0=visible, 1=invisible |
| `int entityMode()` | 비트필드: inPaperSpace 등 |
| `double lineWeight()` | 선 가중치 |

---

### `DwgNonEntityObject` *(interface)* — `DwgObject` 상속

테이블, 딕셔너리 등 비엔티티 객체의 추가 계약. 현재는 마커 인터페이스.

---

### `AbstractDwgObject` *(abstract)*

`DwgObject`의 공통 필드 구현. 모든 구체 클래스의 부모.

**필드** (§20 공통 객체 헤더)
- `long handle`
- `DwgHandleRef ownerHandle`
- `List<DwgHandleRef> reactorHandles`
- `DwgHandleRef xDicHandle`
- `List<XDataRecord> xData`
- `int rawTypeCode`

모든 필드에 대한 getter/setter 구현.

---

### `AbstractDwgEntity` *(abstract)* — `AbstractDwgObject` 상속

`DwgEntity`의 공통 필드 구현.

**필드** (§20 공통 엔티티 헤더 추가)
- `int entityMode`
- `DwgHandleRef layerHandle`
- `DwgHandleRef lineTypeHandle`
- `double lineTypeScale`
- `CmColor color`
- `int invisibility`
- `double lineWeight`
- `int plotStyleFlags`

---

### `DwgObjectType` *(enum)*

스펙 §20에서 정의된 표준 객체 타입 코드.

| 상수 | 타입 코드 | 설명 |
|---|---|---|
| `UNUSED` | 0x00 | 미사용 |
| `TEXT` | 0x01 | 텍스트 |
| `ATTDEF` | 0x02 | 속성 정의 |
| `ATTRIB` | 0x03 | 속성 |
| `SEQEND` | 0x04 | 시퀀스 끝 |
| `INSERT` | 0x07 | 블록 삽입 |
| `MINSERT` | 0x08 | 배열 삽입 |
| `VERTEX_2D` | 0x0A | 2D 버텍스 |
| `VERTEX_3D` | 0x0B | 3D 버텍스 |
| `VERTEX_MESH` | 0x0C | 메쉬 버텍스 |
| `VERTEX_PFACE` | 0x0D | PFace 버텍스 |
| `VERTEX_PFACE_FACE` | 0x0E | PFace 면 |
| `POLYLINE_2D` | 0x0F | 2D 폴리라인 |
| `POLYLINE_3D` | 0x10 | 3D 폴리라인 |
| `ARC` | 0x11 | 호 |
| `CIRCLE` | 0x12 | 원 |
| `LINE` | 0x13 | 선 |
| `DIMENSION_ORDINATE` | 0x14 | 세로좌표 치수 |
| `DIMENSION_LINEAR` | 0x15 | 선형 치수 |
| `DIMENSION_ALIGNED` | 0x16 | 정렬 치수 |
| `DIMENSION_ANG3PT` | 0x17 | 3점 각도 치수 |
| `DIMENSION_ANG2LN` | 0x18 | 2선 각도 치수 |
| `DIMENSION_RADIUS` | 0x19 | 반지름 치수 |
| `DIMENSION_DIAMETER` | 0x1A | 직경 치수 |
| `POINT` | 0x1B | 점 |
| `FACE3D` | 0x1C | 3D 면 |
| `POLYLINE_PFACE` | 0x1D | PFace 폴리라인 |
| `POLYLINE_MESH` | 0x1E | 메쉬 폴리라인 |
| `SOLID` | 0x1F | 솔리드 |
| `TRACE` | 0x20 | 트레이스 |
| `SHAPE` | 0x21 | 도형 |
| `VIEWPORT` | 0x22 | 뷰포트 |
| `ELLIPSE` | 0x23 | 타원 |
| `SPLINE` | 0x24 | 스플라인 |
| `REGION` | 0x25 | 영역 |
| `SOLID3D` | 0x26 | 3D 솔리드 |
| `BODY` | 0x27 | 바디 |
| `RAY` | 0x28 | 광선 |
| `XLINE` | 0x29 | 무한 직선 |
| `DICTIONARY` | 0x2A | 딕셔너리 |
| `MTEXT` | 0x2C | 다중행 텍스트 |
| `LEADER` | 0x2D | 지시선 |
| `TOLERANCE` | 0x2E | 공차 |
| `MLINE` | 0x2F | 다중선 |
| `BLOCK_CONTROL` | 0x30 | 블록 제어 |
| `BLOCK_HEADER` | 0x31 | 블록 헤더 |
| `LAYER_CONTROL` | 0x32 | 레이어 제어 |
| `LAYER` | 0x33 | 레이어 |
| `STYLE_CONTROL` | 0x34 | 스타일 제어 |
| `STYLE` | 0x35 | 텍스트 스타일 |
| `LTYPE_CONTROL` | 0x38 | 선종류 제어 |
| `LTYPE` | 0x39 | 선종류 |
| `VIEW_CONTROL` | 0x3C | 뷰 제어 |
| `VIEW` | 0x3D | 뷰 |
| `UCS_CONTROL` | 0x3E | UCS 제어 |
| `UCS` | 0x3F | UCS |
| `VPORT_CONTROL` | 0x40 | 뷰포트 제어 |
| `VPORT` | 0x41 | 뷰포트 설정 |
| `APPID_CONTROL` | 0x42 | AppID 제어 |
| `APPID` | 0x43 | AppID |
| `DIMSTYLE_CONTROL` | 0x44 | 치수스타일 제어 |
| `DIMSTYLE` | 0x45 | 치수스타일 |
| `BLOCK_RECORD` | 0x48 | 블록 레코드 |
| `LWPOLYLINE` | 0x4D | 경량 폴리라인 |
| `HATCH` | 0x4E | 해치 |
| `XRECORD` | 0x4F | 확장 레코드 |
| `PLACEHOLDER` | 0x50 | 플레이스홀더 |
| `LAYOUT` | 0x62 | 레이아웃 |
| `PROXY_ENTITY` | 0x1F4 | 프록시 엔티티 |
| `PROXY_OBJECT` | 0x1F5 | 프록시 객체 |
| `UNKNOWN` | -1 | 미인식 타입 |

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `int typeCode()` | 타입 코드 정수값 반환 |
| `static DwgObjectType fromCode(int code)` | 코드로 enum 조회. 미인식 → UNKNOWN |
| `boolean isEntity()` | 도면 엔티티 여부 (타입 코드 범위 기반) |
| `boolean isTableEntry()` | 테이블 항목 여부 |

---

### `CommonEntityData`

`AbstractDwgEntity`에 저장되는 공통 헤더 데이터 묶음. 파싱 결과를 임시 보관.

**필드**
- `int entityMode` (BB)
- `int numReactors` (BL)
- `boolean noLinks`
- `int colorIndex`
- `double linetypeScale`
- `int linetypeFlags` (2bit)
- `int plotstyleFlags` (2bit)
- `int invisibility` (BS)
- `int lineWeight` (RC)

---

## 패키지: `io.dwg.entities.geometry`

---

### `DwgLine` — `AbstractDwgEntity` 상속

스펙 §20 LINE 객체.

**필드**
- `Point3D start` — 시작점
- `Point3D end` — 끝점
- `double thickness`
- `double[] extrusion` — 법선 벡터 (기본 0,0,1)
- `boolean parametric` — R2000+ 압축 여부 플래그

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `Point3D start()` | 시작점 반환 |
| `Point3D end()` | 끝점 반환 |
| `double length()` | 선 길이 계산 (3D 거리) |
| `double thickness()` | 두께 반환 |
| `double[] extrusion()` | 법선 벡터 반환 |

---

### `DwgCircle` — `AbstractDwgEntity` 상속

스펙 §20 CIRCLE.

**필드**
- `Point3D center`
- `double radius`
- `double thickness`
- `double[] extrusion`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `Point3D center()` | 중심점 |
| `double radius()` | 반지름 |
| `double area()` | π × r² 계산 |
| `double circumference()` | 2π × r 계산 |

---

### `DwgArc` — `AbstractDwgEntity` 상속

스펙 §20 ARC.

**필드**
- `Point3D center`
- `double radius`
- `double startAngle` — 라디안
- `double endAngle` — 라디안
- `double thickness`
- `double[] extrusion`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `Point3D center()` | 중심점 |
| `double startAngle()` | 시작 각도 (라디안) |
| `double endAngle()` | 끝 각도 (라디안) |
| `double arcLength()` | 호 길이 계산 |
| `Point3D startPoint()` | 시작점 좌표 계산 |
| `Point3D endPoint()` | 끝점 좌표 계산 |

---

### `DwgEllipse` — `AbstractDwgEntity` 상속

스펙 §20 ELLIPSE.

**필드**
- `Point3D center`
- `Point3D majorAxisVec` — 장축 끝점 벡터
- `double[] extrusion`
- `double axisRatio` — 단축/장축 비율
- `double startParam` — 0 ~ 2π
- `double endParam`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `double majorRadius()` | 장축 반지름 (majorAxisVec 길이) |
| `double minorRadius()` | 단축 반지름 (majorRadius × axisRatio) |

---

### `DwgSpline` — `AbstractDwgEntity` 상속

스펙 §20 SPLINE.

**필드**
- `int scenario` — 1=knot/control, 2=fit
- `int degree`
- `boolean rational`, `boolean closed`, `boolean periodic`
- `double knotTolerance`, `double controlTolerance`, `double fitTolerance`
- `double[] knots`
- `Point3D[] controlPoints`
- `double[] weights` — 유리 스플라인 가중치
- `Point3D[] fitPoints`
- `Point3D startTangent`, `Point3D endTangent`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `int degree()` | 스플라인 차수 |
| `List<Point3D> controlPoints()` | 제어점 목록 |
| `List<Point3D> fitPoints()` | 피팅점 목록 |
| `boolean isClosed()` | 닫힘 여부 |

---

### `DwgPoint` — `AbstractDwgEntity` 상속

**필드**
- `Point3D position`
- `double thickness`
- `double[] extrusion`
- `double xAxisAngle`

---

### `DwgPolyline2D` — `AbstractDwgEntity` 상속

**필드**
- `int flags` — 비트필드 (closed, curveFit 등)
- `int curveType`
- `double startWidth`, `double endWidth`
- `double thickness`
- `double elevation`
- `double[] extrusion`
- `DwgHandleRef firstVertex`, `DwgHandleRef lastVertex`
- `DwgHandleRef seqEnd`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `boolean isClosed()` | flags & 0x01 여부 |
| `boolean isCurveFit()` | flags & 0x02 여부 |

---

### `DwgPolyline3D` — `AbstractDwgEntity` 상속

**필드**
- `int flags`
- `DwgHandleRef firstVertex`, `DwgHandleRef lastVertex`, `DwgHandleRef seqEnd`

---

### `DwgVertex2D` — `AbstractDwgEntity` 상속

**필드**
- `int flags`
- `Point3D position`
- `double startWidth`, `double endWidth`
- `double bulge`
- `double tangentDir`

---

### `DwgVertex3D` — `AbstractDwgEntity` 상속

**필드**
- `int flags`
- `Point3D position`

---

### `DwgLwPolyline` — `AbstractDwgEntity` 상속

스펙 §20 LWPOLYLINE. 경량화된 폴리라인.

**필드**
- `int flags`
- `double constantWidth`
- `double elevation`
- `double thickness`
- `double[] extrusion`
- `List<Point2D> vertices`
- `List<Double> bulges`
- `List<double[]> widths` — 각 버텍스 시작/끝 폭

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `int vertexCount()` | 버텍스 수 |
| `boolean isClosed()` | flags & 0x01 |
| `List<Point2D> vertices()` | 버텍스 좌표 목록 |

---

### `DwgSolid` — `AbstractDwgEntity` 상속

**필드**
- `double thickness`
- `double elevation`
- `Point2D corner1`, `corner2`, `corner3`, `corner4`
- `double[] extrusion`

---

### `DwgFace3D` — `AbstractDwgEntity` 상속

**필드**
- `Point3D corner1`, `corner2`, `corner3`, `corner4`
- `int invisEdgeFlags`

---

### `DwgRay`, `DwgXline` — `AbstractDwgEntity` 상속

**필드 (공통)**
- `Point3D point` — 기준점
- `Point3D vector` — 방향 벡터

---

### `DwgRegion`, `DwgBody`, `Dwg3dSolid` — `AbstractDwgEntity` 상속

**필드**
- `int version`
- `List<String> acisData` — ACIS 텍스트 데이터 (TV 배열)

---

## 패키지: `io.dwg.entities.annotation`

---

### `DwgText` — `AbstractDwgEntity` 상속

스펙 §20 TEXT.

**필드**
- `double insertionHeight` (BD, R2000+에서만)
- `Point2D insertionPoint`
- `double height`
- `String text`
- `double rotation`
- `double widthFactor`
- `double oblique`
- `int generation` — 미러 플래그
- `int horizAlign`, `int vertAlign`
- `Point2D alignmentPoint`
- `double[] extrusion`
- `double thickness`
- `DwgHandleRef styleHandle`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `String textString()` | 텍스트 내용 반환 |
| `Point2D insertionPoint()` | 삽입점 |
| `double height()` | 글자 높이 |
| `double rotation()` | 회전 각도 (라디안) |

---

### `DwgMText` — `AbstractDwgEntity` 상속

스펙 §20 MTEXT.

**필드**
- `Point3D insertionPoint`
- `double[] extrusion`
- `Point3D xAxisDir`
- `double rectangleWidth`
- `double textHeight`
- `int attachment` — 정렬 위치 (1~9)
- `int drawingDir`
- `String text`
- `DwgHandleRef styleHandle`
- `double lineSpacingFactor`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `String text()` | MTEXT 포맷 포함 원본 문자열 |
| `String plainText()` | MTEXT 포맷 코드 제거 후 순수 텍스트 |
| `int attachmentPoint()` | 정렬 위치 (1=TL ~ 9=BR) |

---

### `DwgAttDef`, `DwgAttrib` — `AbstractDwgEntity` 상속

DwgText와 거의 동일 필드에 추가로:
**필드**
- `String tag`
- `int fieldLength`
- `int flags`
- `boolean invisible`, `boolean constant`, `boolean verify`, `boolean preset`

---

### `DwgDimension` *(abstract)* — `AbstractDwgEntity` 상속

모든 치수 엔티티의 공통 기반.

**공통 필드**
- `String text` — 치수 텍스트
- `Point3D insertionPoint` — 치수선 삽입점
- `double[] extrusion`
- `Point3D textMidpoint`
- `double elevation`
- `byte flags` — 치수 타입 비트
- `double textRotation`
- `double horizontalDirection`
- `double[] insScale`
- `double insRotation`
- `DwgHandleRef styleHandle`, `DwgHandleRef blockHandle`

---

### `DwgDimAligned`, `DwgDimLinear`, `DwgDimRadial`, `DwgDimDiameter`, `DwgDimAngular2L`, `DwgDimAngular3P`, `DwgDimOrdinate`

각 치수 타입별 추가 필드 (§20 해당 섹션 참조). 공통 패턴:

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `DwgObjectType objectType()` | 해당 치수 타입 반환 |
| 타입별 좌표 getter | 정의점, 방향, 각도 등 반환 |

---

### `DwgLeader` — `AbstractDwgEntity` 상속

**필드**
- `boolean arrowHeadOn`
- `int pathType` — 0=직선, 1=스플라인
- `int annotType`
- `List<Point3D> points`
- `double[] extrusion`
- `Point3D xDir`
- `Point3D offset`
- `DwgHandleRef annotation`

---

### `DwgTolerance` — `AbstractDwgEntity` 상속

**필드**
- `String text`
- `Point3D insertionPoint`
- `Point3D xDir`
- `double[] extrusion`
- `DwgHandleRef styleHandle`

---

## 패키지: `io.dwg.entities.block`

---

### `DwgBlock` — `AbstractDwgEntity` 상속

**필드**
- `String name` — 블록 이름

---

### `DwgEndblk` — `AbstractDwgEntity` 상속

블록 끝 마커. 추가 필드 없음.

---

### `DwgInsert` — `AbstractDwgEntity` 상속

스펙 §20 INSERT.

**필드**
- `Point3D insertionPoint`
- `boolean hasAttribs`
- `double[] scale` — x, y, z
- `double rotation`
- `double[] extrusion`
- `DwgHandleRef blockHeader`
- `DwgHandleRef firstAttrib`, `DwgHandleRef lastAttrib`
- `DwgHandleRef seqEnd`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `Point3D insertionPoint()` | 삽입점 |
| `double[] scale()` | XYZ 스케일 |
| `double rotation()` | 회전각 (라디안) |
| `DwgHandleRef blockHandle()` | 참조하는 블록 핸들 |
| `boolean hasAttributes()` | 속성 포함 여부 |

---

### `DwgMInsert` — `AbstractDwgEntity` 상속

DwgInsert 모든 필드 + 배열 정보:
- `int columns`, `int rows`
- `double columnSpacing`, `double rowSpacing`

---

### `DwgBlockHeader` — `AbstractDwgObject` 상속, `DwgNonEntityObject` 구현

스펙 §20 BLOCK_HEADER.

**필드**
- `String name`
- `int flags`
- `Point3D basePoint`
- `String xRefPath`
- `boolean isXref`, `boolean isOverlaid`
- `boolean hasAttribs`
- `DwgHandleRef firstEntity`, `DwgHandleRef lastEntity`
- `DwgHandleRef layer`
- `List<DwgHandleRef> insertHandles`
- `DwgHandleRef blockRecord`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `String name()` | 블록 이름 |
| `boolean isModelSpace()` | name.equals("*MODEL_SPACE") |
| `boolean isPaperSpace()` | name.equals("*PAPER_SPACE") |
| `boolean isXRef()` | 외부 참조 여부 |

---

### `DwgBlockRecord` — `AbstractDwgObject` 상속, `DwgNonEntityObject` 구현

**필드**
- `String name`
- `DwgHandleRef blockHeader`
- `List<DwgHandleRef> layouts`

---

## 패키지: `io.dwg.entities.table`

---

### `DwgLayerTable` — `AbstractDwgObject` 상속

스펙 §20 LAYER_CONTROL. 레이어 테이블 컨트롤 객체.

**필드**
- `int numEntries`
- `List<DwgHandleRef> entries`

---

### `DwgLayerEntry` — `AbstractDwgObject` 상속

스펙 §20 LAYER.

**필드**
- `String name`
- `int flags`
- `boolean isFrozen`, `boolean isOff`, `boolean isFrozenInNew`, `boolean isLocked`
- `CmColor color`
- `DwgHandleRef lineTypeHandle`
- `DwgHandleRef plotStyleHandle`
- `int lineWeight`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `String name()` | 레이어 이름 |
| `boolean isVisible()` | !isOff |
| `boolean isFrozen()` | 동결 여부 |
| `CmColor color()` | 색상 |

---

### `DwgLTypeEntry` — `AbstractDwgObject` 상속

스펙 §20 LTYPE.

**필드**
- `String name`
- `String description`
- `double patternLength`
- `int alignment`
- `List<LTypeElement> elements`

---

### `DwgStyleEntry` — `AbstractDwgObject` 상속

스펙 §20 STYLE (텍스트 스타일).

**필드**
- `String name`
- `double fixedHeight`
- `double widthFactor`
- `double obliqueAngle`
- `int generationFlags`
- `double lastHeight`
- `String primaryFontFile`
- `String bigFontFile`

---

### `DwgViewEntry`, `DwgUcsTable`, `DwgVPortTable`, `DwgAppIdTable`, `DwgDimStyleTable`

각 테이블 타입별 §20 정의에 따른 필드 구현. 패턴은 `DwgLayerEntry`와 동일.

---

## 패키지: `io.dwg.entities.misc`

---

### `DwgHatch` — `AbstractDwgEntity` 상속

스펙 §20 HATCH.

**필드**
- `double elevation`
- `double[] extrusion`
- `String patternName`
- `boolean isSolid`
- `boolean isAssociative`
- `int style` — 0=normal, 1=outer, 2=ignore
- `int patternType`
- `double patternAngle`
- `double patternScale`
- `boolean doubleHatch`
- `List<HatchBoundary> boundaries`
- `List<HatchLine> lines` — 패턴 라인 (solid 아닐 때)

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `boolean isSolidFill()` | patternName.equals("SOLID") 또는 isSolid |
| `List<HatchBoundary> boundaries()` | 경계 목록 |

---

### `DwgViewport` — `AbstractDwgEntity` 상속

스펙 §20 VIEWPORT.

**필드**
- `Point3D center`
- `double width`, `double height`
- `Point3D viewCenter`
- `Point2D snapBase`, `Point2D snapSpacing`
- `double viewHeight`, `double lensLength`
- `double frontClip`, `double backClip`
- `double snapAngle`, `double twistAngle`
- `int viewportId`

---

### `DwgDictionary` — `AbstractDwgObject` 상속, `DwgNonEntityObject` 구현

스펙 §20 DICTIONARY.

**필드**
- `int numEntries`
- `List<String> entryNames`
- `List<DwgHandleRef> entryHandles`
- `boolean isHard` — 소유권 타입

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `Optional<DwgHandleRef> get(String name)` | 이름으로 핸들 조회 |
| `Map<String, DwgHandleRef> entries()` | 전체 엔트리 맵 |
| `boolean contains(String name)` | 이름 존재 여부 |

---

### `DwgGroup` — `AbstractDwgObject` 상속

**필드**
- `String description`
- `boolean unnamed`, `boolean selectable`
- `List<DwgHandleRef> entities`

---

### `DwgLayout` — `AbstractDwgObject` 상속

스펙 §20 LAYOUT.

**필드**
- `DwgPlotSettings plotSettings`
- `String layoutName`
- `int tabOrder`
- `Point2D limMin`, `Point2D limMax`
- `Point3D insBase`
- `Point3D extMin`, `Point3D extMax`
- `DwgHandleRef blockRecordHandle`
- `DwgHandleRef viewportHandle`

---

### `DwgPlotSettings`

PLOTSETTINGS 공통 데이터 (LAYOUT에 포함).

**필드**
- `String pageName`
- `String printerName`
- `String paperSize`
- `String plotViewName`
- `double leftMargin`, `double bottomMargin`, `double rightMargin`, `double topMargin`
- `double paperWidth`, `double paperHeight`
- `int plotFlags`

---

### `DwgXRecord` — `AbstractDwgObject` 상속

스펙 §20 XRECORD. 임의 데이터 저장.

**필드**
- `int numDataBytes`
- `List<XDataRecord> data`

---

### `DwgProxyEntity`, `DwgProxyObject` — `AbstractDwgEntity/AbstractDwgObject` 상속

**필드**
- `int classId`
- `byte[] entityData`
- `ProxyGraphics graphics`
- `List<DwgHandleRef> objectIds`

---

---

# 상세 설계 보충

---

## 공통 객체 헤더 바이너리 레이아웃 (§20)

모든 DwgObject(`AbstractDwgObject`)의 파일 내 바이너리 구조:

```
// 1. 객체 전체 크기
objectSize   MS    (Modular Short: 전체 객체 데이터 비트 수)

// 2. 객체 타입 코드
typeCode     BS    (Bit Short: 표준=0x01~0xFF, 커스텀=500+)

// 3. 공통 객체 헤더 (모든 객체)
handle       H     (핸들 코드 4bit + counter 4bit + value)
xDataSize    BS    (XData 크기. 0이면 이하 없음)
[xData 데이터 = XDataParser로 별도 파싱]
numReactors  BL
isXDicMissing B   (R2004+, true면 xDicHandle 없음)
[xDicHandle  H   (isXDicMissing == false일 때)]
ownerHandle  H

// 4. 엔티티 추가 헤더 (isEntity() == true인 경우만)
→ 아래 "공통 엔티티 헤더" 참조

// 5. 타입별 고유 데이터 (각 엔티티 클래스 참조)

// 6. 핸들 참조 데이터 (각 엔티티의 연관 핸들들)
```

---

## 공통 엔티티 헤더 바이너리 레이아웃

`AbstractDwgEntity`에 해당하는 추가 헤더. 공통 객체 헤더 직후 위치:

```
entityMode      BB    (2bits: 비트0=inPaperSpace, 비트1=?)
numReactors     BL    (공통 객체 헤더의 numReactors와 동일 위치에 병합)
noLinks         B     (R2000+: true이면 prev/next 핸들 생략)

// 색상 (버전별 분기)
if version < R2004:
    colorIndex  BS

if version >= R2004:
    colorIndex  BS
    if (colorIndex & 0x2000) != 0:  // has RGB
        rgb     BL
    if (colorIndex & 0x4000) != 0:  // has name/book
        colorName TV
        bookName  TV

linetypeFlags   BB    (2bits: 00=ByLayer, 01=ByBlock, 10=Continuous, 11=handle 존재)
if version >= R2000:
    plotstyleFlags BB
if version >= R2007:
    materialFlags  BB
    shadowFlags    RC

invisibility    BS    (0=visible, 1=invisible)
lineWeight      RC    (0xFF=ByLayer 등, DWG lineweight code)

// 이 이후 핸들 참조:
layerHandle     H
if linetypeFlags == 0b11:  lineTypeHandle H
if version >= R2000 && plotstyleFlags == 0b11: plotStyleHandle H
if version >= R2007 && materialFlags == 0b11:  materialHandle  H
```

---

## 엔티티별 바이너리 레이아웃 상세 (§20)

### BLOCK_HEADER (type=0x31)

```
name         TV     (블록 이름, e.g. "DETAIL_1" 또는 "*MODEL_SPACE")
64flag       B      (R2000+: 0이면 이후 64바이트 블록 플래그 없음)
if !64flag:
    xRefIndex BS    (외부 참조 인덱스)
isXDependent B
isAnonymous  B
hasAttribs   B
isXRef       B
isOverlaid   B
isLoaded     B      (R2000+)
numOwnedObjs BL     (R2004+: 소유 객체 수)
basePoint   3BD     (기준점 X,Y,Z)
xRefPath    TV      (외부 참조 경로, 있을 때)
hasDescription B
if hasDescription: description TV
insertCount  RC     (삽입 카운트, 0이면 이후 없음)
blockFlags2  RC     (R2000+)
previewIcon  BL     (미리보기 아이콘 바이트 수)
[previewIcon bytes]

// 핸들 참조
blockControl   H    (→ BLOCK_CONTROL 객체)
nullHandle     H    (항상 null)
blockEntity    H    (→ 이 블록의 BLOCK 엔티티)
if !isXRef && !isOverlaid:
    firstEntity  H  (→ 첫 번째 소유 엔티티)
    lastEntity   H  (→ 마지막 소유 엔티티)
if numOwnedObjs > 0:  // R2004+
    for each: ownedObjHandle H
endBlkEntity   H    (→ ENDBLK 엔티티)
for each INSERT: insertHandle H
layerHandle    H    (→ LAYER 객체)
```

### LAYER (type=0x33)

```
name         TV     (레이어 이름)
64flag       B
xRefIndex    BS
isDep        B
frozen       B      (동결 여부)
off          B      (꺼짐 여부)
frozenNew    B      (새 뷰포트에서 동결)
locked       B      (잠금)
colorIndex   BS     (ACI 색상 인덱스, 음수=꺼짐)
if version >= R2000:
    lineWeight RC
    plotFlag   B
    if plotFlag:
        plotStyleHandle H

// 핸들 참조
layerControl   H    (→ LAYER_CONTROL)
nullHandle     H
xRefHandle     H    (외부 참조가 있을 때)
lineTypeHandle H    (→ LTYPE 객체)
if version >= R2000 && plotFlag: plotStyleHandle H
if version >= R2007: materialHandle H
```

### LTYPE (type=0x39)

```
name          TV
64flag        B
xRefIndex     BS
isDep         B
description   TV
patternLength BD     (패턴 전체 길이)
alignment     RC     (항상 'A'=0x41)
numElements   RC     (대시/공백/도형 요소 수)
반복 numElements:
    length    BD     (양수=대시, 음수=공백, 0=점)
    shapeFlag BS     (비트0=도형, 비트1=텍스트 등)
    if shapeFlag & 0x02:  // 텍스트
        textArea  BD
        textStyle H
    if shapeFlag & 0x01:  // 도형
        shapeNum  BS
        shapeStyle H
    offset     2RD   (X,Y 오프셋)
    rotation   BD    (회전각)
    scale      BD    (스케일)
    if shapeFlag & 0x01 || shapeFlag & 0x02:
        text   TV

// 핸들 참조
ltypeControl   H
nullHandle     H
xRefHandle     H
```

### DICTIONARY (type=0x2A)

```
numEntries    BL
// R2000+에서만:
isHardOwner   B
cloning       BS    (0=NotApplicable, 1=IgnoreDups, 2=KeepExisting, 3=UseSrcName)

반복 numEntries:
    entryName  TV
    entryHandle H
```

### DIMSTYLE (type=0x45)

```
name          TV
64flag        B
xRefIndex     BS
isDep         B
// 150+개의 치수 변수 (DIMSCALE, DIMASZ 등)
// 주요 변수 (모두 해당 타입 사용):
DIMSCALE      BD
DIMASZ        BD
DIMEXO        BD
DIMDLI        BD
DIMEXE        BD
DIMRND        BD
DIMDLE        BD
DIMTP         BD
DIMTM         BD
DIMTXT        BD
DIMCEN        BD
DIMTSZ        BD
DIMALTF       BD
DIMLFAC       BD
DIMTVP        BD
DIMTFAC       BD
DIMGAP        BD
// boolean 플래그들 (각 B 또는 BS)
DIMTOL        B
DIMLIM        B
DIMTIH        B
DIMTOH        B
DIMSE1        B
DIMSE2        B
...
// R2000+에서 추가:
DIMLTYPE      H    (→ linetype 핸들)
DIMLTEX1      H
DIMLTEX2      H
...
```

---

## 핸들 그래프 구성 (두 단계 해석)

### 1단계: Handle Registry 구축

`HandlesSectionParser`가 완료되면 `HandleRegistry`에 `handle → fileOffset` 맵이 완성된다.

### 2단계: 객체 파싱 중 핸들 저장

`ObjectsSectionParser`는 각 객체 내의 핸들 참조를 `DwgHandleRef(rawHandle)` 형태로 저장. 이 시점에서는 실제 객체 참조를 해석하지 않는다.

### 3단계: 지연 해석 (Lazy Resolution)

`DwgReadConfig.lazyObjectResolution == true` (기본값):
- `DwgDocument.objectByHandle(long h, Class<T> type)` 호출 시점에 해석
- `HandleRegistry.offsetFor(rawHandle)` → 파일 오프셋 조회 → 해당 객체 반환

`lazyObjectResolution == false`:
- `ObjectsSectionParser` 완료 후 별도 `resolveGraph()` 패스에서 모든 핸들 한 번에 해석
- `DwgHandleRef` → 실제 `DwgObject` 참조로 교체 (또는 ID 기반 캐시 구성)

---

## DwgObjectType.isEntity() 판별 기준

스펙 §20의 타입 코드 범위를 기반으로 엔티티/비엔티티 구분:

```java
boolean isEntity() {
    // 0x01~0x29: 그래픽 엔티티 (LINE, CIRCLE, ARC, TEXT 등)
    // 0x2A~: 비엔티티 (DICTIONARY, MTEXT부터는 혼합)
    // 정확히는 스펙 §20 각 타입의 'entity/object' 구분 참조
    int code = typeCode();
    return (code >= 0x01 && code <= 0x29)
        || code == 0x2C  // MTEXT
        || code == 0x2D  // LEADER
        || code == 0x2E  // TOLERANCE
        || code == 0x2F  // MLINE
        || code == 0x4D  // LWPOLYLINE
        || code == 0x4E  // HATCH
        || code == 0x22  // VIEWPORT
        || code == 0x1F4; // PROXY_ENTITY
}
```

---

## 특수 엔티티 처리 규칙

### POLYLINE_2D / POLYLINE_3D와 VERTEX 관계

```
POLYLINE_2D:
  firstVertexHandle → VERTEX_2D (연결 리스트)
  lastVertexHandle  → 마지막 VERTEX_2D
  seqEndHandle      → SEQEND

파싱 후 그래프 구성:
  DwgPolyline2D.vertices = 핸들 체인 순회로 수집
  SEQEND = 리스트 종료 마커 (데이터 없음)
```

### HATCH 경계 루프 타입

```
HatchBoundary.boundaryType (int 비트필드):
  bit0: outermost loop (외곽)
  bit1: external (외부)
  bit2: non-closed polyline (닫히지 않은 폴리라인)
  bit3: derived (derived loop)
  bit4: textbox (텍스트 박스)
  bit5: outermost (outermost, mpolygon)

경계 구성 요소 타입:
  type 0: Line segment (start 2RD, end 2RD)
  type 1: Circular arc (center 2RD, radius RD, startAngle RD, endAngle RD, isCCW B)
  type 2: Elliptic arc (center 2RD, endpoint 2RD, minorToMajorRatio RD, startAngle RD, endAngle RD, isCCW B)
  type 3: Spline (degree BL, rational B, periodic B, numKnots BL, numCtrlPts BL, knots[] BD, ctrlPts[] 2RD [+ weights BD if rational])
```

### DIMENSION 계층 구조

모든 치수 엔티티는 `DwgDimension` 추상 클래스에서 공통 필드를 파싱하고, 타입별로 추가 필드를 파싱한다:

```
// 공통 (모든 DIMENSION):
extrusion     BE
textMidpoint  2RD
elevation     BD
flags         RC     (치수 타입 비트)
text          TV     (사용자 정의 텍스트, 없으면 "")
textRotation  BD
horizontalDir BD
insScale      3BD
insRotation   BD
if version >= R2000:
    dimType     BS   (치수 타입 코드)
    attachPoint BS
    lineSpace   BS
    lineSpaceFactor BD
    actualMeasurement BD
if version >= R2004:
    unknown     B
    flipped     B
insertPt      2RD

// 타입별 추가 필드 예시:
DwgDimLinear:
    xLine1Pt  2RD
    xLine2Pt  2RD
    dimLinePt 2RD
    extLineAngle BD
    dimRotation  BD

DwgDimRadial:
    centerPt  2RD
    endPt     2RD
    leaderLen BD
```
