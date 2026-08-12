# 공통 데이터 모델 설계서 (Data Model Specification)

본 프로젝트는 불완전한 대체투자 데이터를 일관된 기준으로 심사하기 위해 데이터를 세 가지 계층(Source, Canonical, Target)으로 분리하여 관리하며, 변경 추적(Lineage)과 오류 모델을 코어 데이터 구조에 내재화했습니다.

## 1. 3계층 데이터 모델 아키텍처

### 1) Source Model (원천 데이터)
- **정의**: IM, 엑셀 재무모델, 이메일 등 외부에서 수집되거나 스크래핑된 비정형/반정형 데이터의 원형입니다.
- **구현체**: `RawDocument.java`
- **특징**: 스키마가 고정되어 있지 않으며 문자열(String) 위주의 원시 형태를 유지합니다. 본 프로젝트에서는 `DataGenerator`가 이 Source Model을 덤프합니다.

### 2) Canonical Model (공통 정규화 모델)
- **정의**: 다양한 Source에서 추출된 데이터를 통합하여 내부 시스템이 이해할 수 있도록 표준화한 형태입니다.
- **구현체**: `InvestmentProposal.java`, `TraceableField<T>.java`
- **특징**: 도메인 내의 모든 비즈니스 로직(Rule Engine, AI Service)은 이 Canonical Model만을 바라보고 동작합니다.

### 3) Target Model (목표 출력 모델)
- **정의**: 심사(Review)가 완료된 후 외부 시스템(예: 리포팅 대시보드, 타 결제 시스템)으로 전달되는 최종 결과 포맷입니다.
- **특징**: 불필요한 중간 변환 과정을 숨기고, 최종 심사 결과(APPROVED/REJECTED)와 주요 근거만을 요약하여 내보냅니다. 본 과제에서는 `output/review_results.json`이 이 역할을 수행합니다.

---

## 2. 메타데이터 (Metadata) 속성

### 1) 식별 키 (Key)
- 모든 딜(Deal)은 고유한 `dealId` (예: `DEAL-020`)를 가집니다.
- `RawDocument`는 `dealId`와 `docType`의 복합키(Composite Key) 성격을 띄어 딜과 문서를 매핑합니다.

### 2) 유효기간 (Validity Period)
- 대체투자의 가정(예: 금리, 환율)은 시간에 따라 무효화될 수 있습니다. 
- 현재 코드 상에서는 `History` 객체의 `timestamp`를 통해 데이터의 최신성을 보장하고, 향후 유효기간(TTL: Time To Live) 만료에 따른 자동 갱신 로직을 추가할 수 있는 구조적 기반을 갖췄습니다.

---

## 3. 이력 추적 및 오류 모델 설계 (Core)

대체투자 심사에서는 하나의 숫자(LTV, 금액 등)가 여러 문서를 거치면서 계속 변동됩니다. 기존의 단순 변수 할당 방식(`double ltv = 70.0;`)은 이러한 변동 내역을 잃어버리는 치명적인 단점이 있어, 이를 해결하기 위해 `TraceableField<T>` 래퍼를 도입했습니다.

### 1) TraceableField<T> 설계
- **`T value`**: 파싱 또는 정규화된 현재 값.
- **`double confidence`**: 이 값의 신뢰도 (0.0 ~ 1.0). AI가 모호한 텍스트에서 추론했을수록 낮아지도록 설계되었습니다.
- **`String source`**: 값이 최초 도출된 출처 문서.

### 2) Lineage와 이력 (History) 모델
- **`List<History> lineage`**: 이 필드가 어떠한 변경(AI 개입, 룰 엔진 보정 등)을 거쳐왔는지 기록하는 불변 리스트.
- **불변성 (Immutability)**: 값이 갱신될 때 객체를 덮어쓰지 않고 `withValue()` 를 호출해 새 객체를 만듭니다. 이때 기존 값은 `History(modifiedBy, timestamp, reason, previousValue)` 레코드로 변환되어 `lineage`에 영구 누적(Append-only)됩니다. 
- 이를 통해 최종 심사자는 "Gemini AI가 이메일을 바탕으로 70%에서 75%로 변경했음"을 명확히 추적(Trace)할 수 있습니다.

### 3) 오류 모델 (Error & Conflict Model)
- **`boolean hasConflict`**: 다른 문서 출처에서 상충되는 값이 발견될 때 시스템이 임의로 한쪽을 선택하지 않고 `true`로 설정(Fail-Safe).
- **`ReviewStatus.REVIEW_REQUIRED`**: `hasConflict`가 `true`인 항목은 즉시 시스템 승인 프로세스에서 격리되며, AI나 심사역의 강제 개입(Manual Review)을 유도하는 오류 처리 모델로 기능합니다.
