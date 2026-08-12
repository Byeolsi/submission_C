# 아키텍처 및 시스템 설계 (Architecture & System Design)

본 시스템은 수집된 가상의 비정형 데이터(문서 조각)를 정규화하고 심사하는 **데이터 처리 파이프라인(Data Processing Pipeline)** 구조로 설계되었습니다.

## 1. 핵심 구성요소 (Components)

- **Data Generator (`DataGenerator.java`)**: 
  - Seed 값을 바탕으로 결정론적(Deterministic) 가상 데이터를 합성합니다. `SCENARIOS.md`에 정의된 각종 충돌 및 오류를 확률적으로 주입하여 테스트 베드를 제공합니다.
- **Data Normalizer (`DataNormalizer.java`)**: 
  - 비정형 텍스트(RawDocument)로부터 도메인 지식에 기반한 정규식(Regex)/규칙을 통해 핵심 수치(LTV, 금액 등)를 추출하고, 이를 시스템의 Canonical Model인 `InvestmentProposal` 객체로 변환합니다.
- **Rule Engine (`RuleEngine.java`)**: 
  - LTV 상한선 규제(예: 70% 초과 금지)나 필수 서류 누락 여부와 같은 '결정론적이고 엄격한(Hard-coded)' 비즈니스 룰을 검증하여 1차 심사 합격/불합격을 판정합니다.
- **AI Review Service (`GeminiAIService.java`)**: 
  - Rule Engine이 스스로 판단할 수 없는 모호한 텍스트 뉘앙스나 두 문서 간의 데이터 충돌(Conflict)이 발생했을 때 개입하여, 문맥을 바탕으로 값을 조정하고 **인용구(Quote)**를 반환합니다.

## 2. 데이터 흐름 (Data Flow)

파이프라인 내 데이터 흐름은 철저히 **단방향(Unidirectional)**으로 흐르며 각 단계의 결과가 다음 단계의 입력이 됩니다.

1. **Generation Phase (`--generate`)**
   - 시나리오 설정(Config) $\rightarrow$ `DataGenerator` $\rightarrow$ `RawDocument` (JSON 파일 덤프)
2. **Review Phase (`--review`)**
   - `RawDocument` 파싱 $\rightarrow$ `DataNormalizer` $\rightarrow$ `InvestmentProposal` (초기 상태: PENDING 또는 REVIEW_REQUIRED)
   - $\rightarrow$ `RuleEngine` (1차 검증) 
   - $\rightarrow$ `GeminiAIService` (충돌 해소 및 보정, Lineage 추가)
   - $\rightarrow$ `RuleEngine` (최종 재검증)
   - $\rightarrow$ `output/review_results.json` (최종 리포트 생성)

## 3. 확장 지점 (Extensibility Points)

본 아키텍처는 향후 실제 운영 환경(Production) 도입을 대비해 유연한 확장이 가능하도록 설계되었습니다.

- **파서(Parser)의 모듈화**: 현재 정규식 기반인 `DataNormalizer` 내에 향후 실제 OCR 모듈(Tesseract 등)이나 LLM 기반 추출기(Information Extractor)를 플러그인(Plug-in) 형태로 교체 삽입할 수 있습니다.
- **AI 모델 추상화**: `GeminiAIService`는 인터페이스로 분리하기 쉬운 구조입니다. 필요에 따라 보안이 강화된 On-Premise sLLM 모델 서비스로 손쉽게 교체할 수 있습니다.
- **파이프라인 비동기화**: 단일 쓰레드로 동작하는 현재 파이프라인을 Spring Batch나 Apache Kafka를 활용하여 대규모 비동기 이벤트 스트리밍 아키텍처로 확장할 수 있습니다.

## 4. 실패, 재실행, 관찰 가능성 (Failure, Retry, Observability)

데이터의 정합성이 생명인 금융 도메인 특성에 맞춰 시스템의 견고성(Robustness)을 보장합니다.

- **안전한 실패 (Fail-Safe)**: 
  - 파싱 도중 단위(Unit) 변환이 불가능하거나 두 문서의 값이 모순될 경우, 시스템은 임의로 추측(Guess)하지 않습니다. 즉각 `hasConflict = true` 및 상태를 `REVIEW_REQUIRED`로 격리시켜 인간 심사역의 판단을 강제하는 Fail-Safe 메커니즘을 따릅니다.
- **멱등성과 재실행 (Idempotency & Retry)**: 
  - `DataGenerator`는 Seed 기반으로 구동되므로 언제든 똑같은 모순 상황을 100% 재현(Reproduce)할 수 있습니다. 
  - 파이프라인은 입력 JSON이 동일하다면 언제나 동일한 출력 리포트를 생산하는 멱등성을 지녀, 서버 오류 시 단순히 파이프라인을 재시작(Retry)하면 됩니다.
- **관찰 가능성 (Observability)과 투명성**:
  - `TraceableField<T>` 객체가 가진 `lineage(History)` 속성을 통해, 최종 산출된 값이 언제, 어떤 시스템 모듈(또는 AI)에 의해, 무슨 근거(인용구)로 도출되었는지 100% 역추적 및 관찰이 가능합니다. 이는 차후 금융 당국의 감사(Audit) 시 투명성을 증명하는 완벽한 증거 자료로 활용됩니다.
