@echo off
REM =====================================================================
REM DWG 파싱 단계별 테스트 실행 스크립트 (Windows)
REM =====================================================================

setlocal enabledelayedexpansion

echo DWG 파싱 단계별 테스트
echo =====================================================================
echo.

REM 1. 샘플 파일 경로 설정
set SAMPLE_DWG=%1
if "%SAMPLE_DWG%"=="" (
    set SAMPLE_DWG=sample.dwg
)

REM 2. 샘플 파일 확인
if not exist "%SAMPLE_DWG%" (
    echo [⚠] 샘플 DWG 파일 없음: %SAMPLE_DWG%
    echo 사용법: run_test.bat ^<dwg_file_path^>
    echo.
    echo 예시:
    echo   run_test.bat samples\drawing.dwg
    echo   run_test.bat ..\AutoCAD\test.dwg
    exit /b 1
)

REM 3. Maven 패키징 (필요시)
if not exist "target\classes" (
    echo [1] Maven 패키징 중...
    call mvnw compile -q
    if errorlevel 1 (
        echo ✗ 컴파일 실패
        exit /b 1
    )
)

REM 4. 테스트 실행
echo [1] 테스트 시작...
echo 샘플 파일: %SAMPLE_DWG%
echo.

java -cp "target\classes;target\dependency\*" io.dwg.test.DwgParsingStageTest "%SAMPLE_DWG%"
set TEST_RESULT=%errorlevel%

echo.
if %TEST_RESULT% equ 0 (
    echo ✓ 테스트 완료
) else (
    echo ✗ 테스트 실패 (종료 코드: %TEST_RESULT%)
)

exit /b %TEST_RESULT%
