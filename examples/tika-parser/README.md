# jDwgParser Tika 파서

[Apache Tika](https://tika.apache.org/)에서 DWG 파일(R13~R2018)의 텍스트와 메타데이터를 뽑는 파서입니다. 문서관리 시스템이나 검색엔진(Solr, Elasticsearch 등)이 Tika로 파일을 색인한다면, 이 jar를 클래스패스에 넣기만 하면 DWG 도면의 글자도 검색됩니다.

## 무엇을 뽑는가

- **본문:** 도면의 글자를 텍스트 하나당 문단(`<p>`) 하나로 냅니다.
  - 대상은 TEXT, ATTRIB(블록 속성), MTEXT, 표 셀, 다중 지시선입니다.
  - 모형 공간, 배치, 블록 안의 글자를 모두 포함합니다.
  - MTEXT의 서식 코드(글꼴, 높이, 줄바꿈 등)는 걷어내고 사람이 읽는 글자만 남깁니다.
- **메타데이터:**
  - `Content-Type`: `image/vnd.dwg`
  - `dwg:version`: `R2000`, `R2018` 등
  - `dwg:objects`: 객체 수
  - `dwg:readIssues`: 읽지 못한 부분의 수. 손상된 파일도 나머지는 읽습니다.

## 사용법

먼저 저장소 루트에서 라이브러리를 설치한 뒤 이 프로젝트를 빌드합니다.

```sh
mvn install            # 저장소 루트
cd examples/tika-parser
mvn package            # target/jdwgparser-tika-0.1.0.jar
```

Tika가 `META-INF/services`로 파서를 찾으므로, 두 jar(`jdwgparser`, `jdwgparser-tika`)를 클래스패스에 넣으면 됩니다.

```java
AutoDetectParser parser = new AutoDetectParser();
BodyContentHandler text = new BodyContentHandler(-1);
Metadata metadata = new Metadata();
try (InputStream in = Files.newInputStream(Paths.get("drawing.dwg"))) {
    parser.parse(in, text, metadata, new ParseContext());
}
System.out.println(metadata.get("dwg:version") + ": " + text);
```

`new DwgParser().parse(...)`처럼 직접 호출해도 됩니다. `setMaxBytes(long)`로 파일 하나에 쓰는 메모리 상한을 정할 수 있고, 기본값은 512MB입니다.

## Tika 표준 파서와 함께 쓸 때

`tika-parsers-standard-package`에도 DWG 파서가 있지만, 요약 정보(제목, 작성자 등)만 읽습니다. 두 파서가 모두 클래스패스에 있으면 `tika-config.xml`에서 어느 쪽을 쓸지 정하세요. 예를 들어 표준 파서 묶음에서 DWG를 빼고 이 파서를 넣습니다.

```xml
<properties>
  <parsers>
    <parser class="org.apache.tika.parser.DefaultParser">
      <mime-exclude>image/vnd.dwg</mime-exclude>
    </parser>
    <parser class="io.dwg.tika.DwgParser"/>
  </parsers>
</properties>
```

## 테스트

`DwgParserTest`는 저장소의 `samples` 폴더(`-Ddwg.samples`로 바꿀 수 있음)의 도면으로 확인합니다.
- 텍스트와 메타데이터 추출
- `AutoDetectParser`가 이 파서를 자동으로 고르는지
- DWG가 아닌 파일을 `TikaException`으로 거부하는지
