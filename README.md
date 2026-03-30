# 쿠폰 발급 시스템 (Coupon Issue System)

선착순 쿠폰 발급 시스템의 동시성 제어 성능 비교 및 최적화 학습 프로젝트

## 📌 프로젝트 개요

본 프로젝트는 **동시성 제어 방식에 따른 성능 차이를 학습하고 비교**하기 위해 제작되었습니다.

5000명이 동시에 쿠폰을 발급받는 상황에서 발생하는 동시성 문제를 해결하고,
각 방식의 성능을 측정하여 비교 분석했습니다.

**실제 서비스가 아닌 학습 목적의 프로젝트로, 핵심 동시성 제어에 집중하기 위해
로그인, 인증 등의 부가 기능은 최소화했습니다.**

---

## 🎯 주요 성과

| 지표 | Before | After | 개선율 |
|------|--------|-------|--------|
| **TPS** | 456.5/sec | 1263.4/sec | **+177% (2.8배)** |
| **평균 응답시간** | 5427ms | 1560ms | **-71%** |
| **에러율** | 2.45% | 2.51% | - |

**테스트 환경:** MacBook Pro M1, Docker (PostgreSQL + Redis), 5000명 동시 요청

---

## 🛠 기술 스택

**Backend**
- Java 17, Spring Boot 3.2, Spring Data JPA
- PostgreSQL 16, Redis 7

**Infrastructure**
- Docker & Docker Compose

**Testing**
- Apache JMeter

---

## 🔧 최적화 과정

### Step 1: Pessimistic Lock (기준)
- DB Lock을 통한 동시성 제어
- **TPS: 456.5/sec**

### Step 2: Redis 도입
- 메모리 기반 처리로 Lock 대기 감소
- Redis DECR/SETNX를 활용한 원자적 연산
- **TPS: 738.5/sec (+62%)**

### Step 3: 트랜잭션 분리
- Redis 로직을 트랜잭션 밖으로 분리
- DB 커넥션 점유 시간 최소화
- **TPS: 1263.4/sec (+177%)**

📖 **[상세 최적화 과정 보기](docs/OPTIMIZATION.md)**

---

## 📊 성능 테스트 결과

### 측정값 비교

| 방식 | TPS | 평균 응답시간 | 에러율 |
|------|-----|--------------|--------|
| Pessimistic Lock | 456.5 | 5427ms | 2.45% |
| Redis 도입 | 738.5 | 3090ms | 2.58% |
| 트랜잭션 분리 | 1263.4 | 1560ms | 2.51% |

**테스트 조건:** 5000명 동시 요청, Ramp-up 2초, 각 3회 측정 후 평균

📖 **[상세 성능 테스트 결과 보기](docs/PERFORMANCE.md)**

---

## 🐛 트러블슈팅

### Optimistic Lock 충돌
- Entity의 `@Version` 애노테이션으로 인한 충돌 발생
- Pessimistic Lock/Redis와 중복 적용되어 에러 발생
- `@Version` 제거로 해결

📖 **[트러블슈팅 상세 보기](docs/TROUBLESHOOTING.md)**

---

## 📡 API 명세

### 관리자 API

| Method | Endpoint | Request Body | 설명 |
|--------|----------|--------------|------|
| POST | `/api/admin/coupons` | `name`, `discountRate`, `totalQuantity`, `startDate`, `endDate` | 쿠폰 생성 |

### 사용자 API

| Method | Endpoint | Request Body | 설명 |
|--------|----------|--------------|------|
| GET | `/api/coupons` | - | 전체 쿠폰 조회 |
| POST | `/api/coupons/{id}/issue` | `userId` | 쿠폰 발급 (Pessimistic Lock) |
| POST | `/api/coupons/{id}/issue-redis` | `userId` | 쿠폰 발급 (Redis) |
| GET | `/api/coupons/{id}/stock` | - | 재고 확인 |
| GET | `/api/coupons/my-coupons` | Query: `userId` | 내 쿠폰 조회 |

---

## 💡 학습 내용

- Pessimistic Lock의 특징과 한계
- Redis를 활용한 분산 환경 동시성 제어
- 트랜잭션 범위 최적화의 중요성
- JMeter를 활용한 성능 측정 및 분석
- 분산 환경에서의 데이터 정합성 이슈

---

## 🔜 고려할 수 있는 개선 방안

- Optimistic Lock 방식 추가 및 성능 비교
- 커넥션 풀 크기 최적화
- 로그 출력 최적화
- 비동기 처리를 통한 응답 속도 개선
- 재시도 로직 및 정합성 배치 작업 추가

---

## 👤 Author

**이도헌**
- GitHub: https://github.com/leeworld9/coupon-system