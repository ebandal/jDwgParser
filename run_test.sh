#!/bin/bash

# =====================================================================
# DWG 파싱 단계별 테스트 실행 스크립트
# =====================================================================

echo "DWG 파싱 단계별 테스트"
echo "=====================================================================\n"

# 1. 샘플 파일 경로 설정
SAMPLE_DWG="${1:-./${SAMPLE:-sample.dwg}}"

# 2. 샘플 파일 확인
if [ ! -f "$SAMPLE_DWG" ]; then
    echo "[⚠] 샘플 DWG 파일 없음: $SAMPLE_DWG"
    echo "사용법: ./run_test.sh <dwg_file_path>"
    echo ""
    echo "예시:"
    echo "  ./run_test.sh ./samples/drawing.dwg"
    echo "  ./run_test.sh ../AutoCAD/test.dwg"
    exit 1
fi

# 3. 클래스 경로 준비
CLASSPATH="target/classes:target/dependency/*"

# 4. 테스트 실행
echo "[1] 테스트 시작..."
echo "샘플 파일: $SAMPLE_DWG"
echo ""

java -cp "$CLASSPATH" io.dwg.test.DwgParsingStageTest "$SAMPLE_DWG"

TEST_RESULT=$?

echo ""
if [ $TEST_RESULT -eq 0 ]; then
    echo "✓ 테스트 완료"
else
    echo "✗ 테스트 실패 (종료 코드: $TEST_RESULT)"
fi

exit $TEST_RESULT
