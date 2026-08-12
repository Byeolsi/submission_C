# 테스트 케이스 및 기대 결과 (Test Cases & Expected Results)

본 프로젝트는 감독관님의 원활한 평가를 위해, 핵심 로직에 대한 **자동화된 단위 테스트(JUnit)**를 `tests/` 폴더 하위에 구성하였습니다. `./gradlew test` 명령어를 통해 전체 테스트를 즉시 검증할 수 있습니다.

아래는 본 시스템의 핵심 철학을 검증하는 주요 테스트 케이스와 그 기대 결과입니다.

---

## 1. TraceableField (이력 추적 모델) 검증
- **테스트 파일**: `tests/com/noaats/reviewsystem/domain/TraceableFieldTest.java`
- **목적**: 데이터 래퍼 클래스가 값의 변경 이력을 유실 없이 추적하는지 검증합니다.

### 1-1. `testTraceableFieldCreation()`
- **상황 (Given)**: "IM Document" 출처를 가진 LTV 65.0% 데이터를 생성.
- **행동 (When)**: `TraceableField.of()` 호출.
- **기대 결과 (Then)**: 
  - `getValue()` == 65.0
  - `getSource()` == "IM Document"
  - `hasConflict()` == false
  - `getLineage()` == 비어 있음 (초기 상태)

### 1-2. `testTraceableFieldUpdateWithHistory()`
- **상황 (Given)**: 위에서 생성한 초기 LTV(65.0%) 데이터 존재.
- **행동 (When)**: 시스템(또는 AI)이 충돌을 감지하고, "Gemini AI"라는 출처로 값을 75.0%로 변경하며 `hasConflict=true` 마킹을 수행 (`withValue()` 호출).
- **기대 결과 (Then)**:
  - 기존 객체를 덮어쓰지 않고 새로운 객체 반환.
  - 새 객체의 `getValue()` == 75.0, `hasConflict()` == true.
  - **가장 중요한 결과**: `getLineage()` 사이즈가 1로 증가하며, 내부에 `History` 객체가 생성됨. 
  - `History` 안에는 **"누가(Gemini AI)", "어떤 이유로", "기존 값(65.0)이 무엇이었는지"**가 정확히 기록되어야 함.

---

## 2. DataGenerator (결정론적 데이터 합성기) 검증
- **테스트 파일**: `tests/com/noaats/reviewsystem/generator/DataGeneratorTest.java`
- **목적**: 시스템이 파이프라인 처리를 위해 항상 일관된(Deterministic) 모순 데이터를 생성해 내는지 검증합니다.

### 2-1. `testDataGenerationReproducibility()`
- **상황 (Given)**: 시드(Seed) 값이 12345L로 고정된 동일한 `ScenarioConfig` 두 개를 생성.
- **행동 (When)**: 두 개의 개별 `DataGenerator` 인스턴스에서 각각 `generate()` 호출.
- **기대 결과 (Then)**:
  - 첫 번째 실행 결과와 두 번째 실행 결과의 **문서 개수(size)가 완벽히 동일**해야 함.
  - 두 결과의 **첫 번째 문서 내용(content)이 완벽히 동일**해야 함.
  - 30개의 딜(Deal) 설정 시, 1딜당 복수의 문서가 생성되므로 최소 30개 이상의 Raw Document가 생성됨을 보장해야 함.

---

## 💡 감독관 직접 실행 안내
본 테스트들은 CI/CD 환경에서 즉각적인 검증이 가능하도록 Gradle 환경에 통합되어 있습니다.
```bash
# 전체 테스트 실행 및 결과 확인
./gradlew test
```
