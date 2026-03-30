# 최적화 과정 상세

쿠폰 발급 시스템의 단계별 성능 최적화 과정을 상세히 기록합니다.

---

## 📊 전체 개선 결과

| 단계 | TPS | 평균 응답시간 | 개선율 |
|------|-----|--------------|--------|
| Before | 456.5 | 5427ms | - |
| Step 1 (Redis) | 738.5 | 3090ms | +62% |
| Step 2 (트랜잭션 분리) | 1263.4 | 1560ms | +177% |

---

## Step 1: Pessimistic Lock (기준선)

### 구현 방식
```java
@Transactional
public CouponIssueResponseDto issueCoupon(Long couponId, CouponIssueRequestDto request) {
    // 1. DB에서 중복 체크
    if (couponIssueRepository.existsByUserIdAndCouponId(request.getUserId(), couponId)) {
        throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
    }
    
    // 2. Pessimistic Lock으로 재고 확인 및 차감
    Coupon coupon = couponRepository.findByIdWithPessimisticLock(couponId)
           .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다. id=" + couponId));
   
    coupon.increaseIssuedQuantity();
    
    // 3. DB에 발급 기록 저장
    CouponIssue couponIssue = CouponIssue.builder()
        .couponId(couponId)
        .userId(request.getUserId())
        .build();
    
    CouponIssue saved = couponIssueRepository.save(couponIssue);
    return new CouponIssueResponseDto(saved);
}
```

### 문제점 분석

**1. DB Lock 대기**
```
요청 1: Lock 획득 → 처리 → Lock 해제 (100ms)
요청 2: Lock 대기 → Lock 획득 → 처리 → Lock 해제 (200ms)
요청 3: Lock 대기 → Lock 대기 → Lock 획득 → 처리 (300ms)
...
```
→ 순차 처리로 인한 대기 시간 누적

**2. 디스크 기반 I/O**
- PostgreSQL은 디스크 기반
- 읽기/쓰기 작업이 밀리초(ms) 단위
- 메모리 대비 느린 처리 속도

**3. 모든 작업을 DB에서 처리**
- 중복 체크
- 재고 확인
- 재고 차감
- 발급 기록

→ DB에 과도한 부하

### 성능 결과
```
TPS: 456.5/sec
평균 응답시간: 5427ms
```

---

## Step 2: Redis 도입

### 왜 Redis를 선택했나?

**1. 메모리 기반 처리**
- DB(디스크): 밀리초(ms) 단위
- Redis(메모리): 마이크로초(μs) 단위
- **약 1000배 빠른 처리 속도**

**2. Lock 없는 원자적 연산**

Redis는 싱글 스레드로 동작하며, 명령어 자체가 원자적:

- `DECR`: 값을 원자적으로 1 감소
- `SETNX`: 키가 없을 때만 설정
```
5000명이 동시에 DECR 요청
→ Redis가 순차적으로 하나씩 처리
→ Lock 없이도 정합성 보장
```

**3. 분산 환경 지원**
- 여러 서버에서 동일한 Redis 접근
- 중앙 집중식 재고 관리

### 구현 방식
```java
@Transactional
public CouponIssueResponseDto issueCouponWithRedis(Long couponId, CouponIssueRequestDto request) {
    Long userId = request.getUserId();
    String stockKey = "coupon:stock:" + couponId;
    String userKey = "coupon:user:" + userId + ":" + couponId;
    
    // 1. Redis로 중복 체크 (SETNX)
    Boolean isFirstRequest = redisTemplate.opsForValue()
        .setIfAbsent(userKey, "1", Duration.ofMinutes(10));
    
    if (Boolean.FALSE.equals(isFirstRequest)) {
        throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
    }
    
    // 2. Redis로 재고 차감 (DECR)
    Long stock = redisTemplate.opsForValue().decrement(stockKey);
    
    if (stock == null || stock < 0) {
        // 재고 복구
        redisTemplate.opsForValue().increment(stockKey);
        redisTemplate.delete(userKey);
        throw new IllegalStateException("쿠폰이 모두 소진되었습니다.");
    }
    
    try {
        // 3. DB에 최종 기록만 저장
        Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));
        
        CouponIssue couponIssue = CouponIssue.builder()
            .couponId(couponId)
            .userId(userId)
            .build();
        
        CouponIssue saved = couponIssueRepository.save(couponIssue);
        coupon.increaseIssuedQuantity();
        
        return new CouponIssueResponseDto(saved);
        
    } catch (Exception e) {
        // 실패 시 Redis 재고 복구
        redisTemplate.opsForValue().increment(stockKey);
        redisTemplate.delete(userKey);
        throw e;
    }
}
```

### 개선 효과
```
TPS: 738.5/sec (+62%)
평균 응답시간: 3090ms (-43%)
```

**핵심:**
- 빠른 검증 및 차감 (Redis)
- DB는 기록만 담당

---

## Step 3: 트랜잭션 분리

### 문제 발견

Redis 도입으로 개선되었지만, 기대만큼은 아님.

**원인 분석:**
```java
@Transactional  // ← 전체를 감싸는 트랜잭션
public void issueCouponWithRedis() {
    // Redis 작업 (매우 빠름)
    redis.decrement();  // 예) 0.001초
    
    // DB 작업 (느림)
    db.save();          // 예) 0.1초
    
    // 문제: DB 커넥션을 0.101초 동안 점유
}
```

**병목 원인:**

1. **DB 커넥션 풀 고갈**
   - 기본 커넥션 풀: 10개
   - Redis 작업은 빠르지만 DB 작업이 상대적으로 느림
   - 전체 로직이 하나의 트랜잭션으로 묶여있어 DB 커넥션을 오래 점유
   - 결과적으로 커넥션 대기 발생

2. **불필요한 트랜잭션 범위**
    - Redis는 자체적으로 원자성 보장
    - Redis 작업까지 트랜잭션에 포함시킬 필요 없음

### 해결 방법

**Redis 로직을 트랜잭션 밖으로 분리:**
```java
// 트랜잭션 없음 - Redis만 처리
public CouponIssueResponseDto issueCouponWithRedis(Long couponId, CouponIssueRequestDto request) {
    Long userId = request.getUserId();
    
    // 1. Redis 검증 및 차감 (트랜잭션 X)
    validateAndDecrementStock(couponId, userId);
    
    // 2. DB 저장 (트랜잭션 O)
    return saveToDatabase(couponId, userId);
}

// Redis 작업
private void validateAndDecrementStock(Long couponId, Long userId) {
    String stockKey = "coupon:stock:" + couponId;
    String userKey = "coupon:user:" + userId + ":" + couponId;
    
    // 중복 체크
    Boolean isFirstRequest = redisTemplate.opsForValue()
        .setIfAbsent(userKey, "1", Duration.ofMinutes(10));
    
    if (Boolean.FALSE.equals(isFirstRequest)) {
        throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
    }
    
    // 재고 차감
    Long stock = redisTemplate.opsForValue().decrement(stockKey);
    
    if (stock == null || stock < 0) {
        redisTemplate.opsForValue().increment(stockKey);
        redisTemplate.delete(userKey);
        throw new IllegalStateException("쿠폰이 모두 소진되었습니다.");
    }
}

// DB 작업
@Transactional
public CouponIssueResponseDto saveToDatabase(Long couponId, Long userId) {
    Coupon coupon = couponRepository.findById(couponId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));
    
    CouponIssue couponIssue = CouponIssue.builder()
        .couponId(couponId)
        .userId(userId)
        .build();
    
    CouponIssue saved = couponIssueRepository.save(couponIssue);
    coupon.increaseIssuedQuantity();
    
    return new CouponIssueResponseDto(saved);
}
```

### 개선 효과

**커넥션 점유 시간 비교:**
```
Before (트랜잭션 통합):
Redis (0.001초) + DB (0.1초) = 0.101초 커넥션 점유

After (트랜잭션 분리):
Redis (0.001초, 커넥션 X) + DB (0.1초, 커넥션 O) = 0.1초 커넥션 점유
```

**결과:**
```
TPS: 1263.4/sec (+177%)
평균 응답시간: 1560ms (-71%)
```

**핵심:**
- DB 커넥션 점유 시간 최소화
- 더 많은 요청을 동시에 처리 가능

---

## ⚠️ 주의사항: 분산 트랜잭션 문제

### 현재 구조의 한계

Redis와 DB가 분리되어 있어 **원자성 보장 안 됨**:
```
정상 시나리오:
1. Redis 재고 차감 성공
2. DB 저장 성공
→ OK

문제 시나리오:
1. Redis 재고 차감 성공
2. DB 저장 실패 (네트워크 끊김, DB 장애 등)
→ Redis는 차감, DB는 기록 없음 (불일치!)
```

### 현재 대응
```java
try {
    // DB 저장
    saveToDatabase();
} catch (Exception e) {
    // 실패 시 Redis 복구 시도
    redisTemplate.opsForValue().increment(stockKey);
    redisTemplate.delete(userKey);
    throw e;
}
```

**한계:**
- 복구 로직도 실패할 수 있음
- 완벽한 원자성 보장 불가

### 프로덕션 개선 방향

1. **재시도 로직**
    - DB 저장 실패 시 자동 재시도
    - 최대 3회 재시도 후 실패 처리

2. **정합성 배치 작업**
```
   매일 새벽:
   Redis 재고 vs DB 발급 수량 비교
   → 불일치 발견 시 알림
```

3. **보상 트랜잭션**
    - 실패 기록을 별도 테이블에 저장
    - 추후 수동/자동 보상 처리

4. **메시지 큐 활용**
    - Redis 성공 → 메시지 큐에 전송
    - Worker가 메시지 꺼내서 DB 저장
    - 실패 시 재처리

---

## 정리

| 단계 | 방식 | TPS | 핵심 개선 |
|------|------|-----|----------|
| Before | Pessimistic Lock | 456.5 | - |
| Step 1 | Redis 도입 | 738.5 | 메모리 기반 처리 |
| Step 2 | 트랜잭션 분리 | 1263.4 | 커넥션 최적화 |

**최종 개선율: 2.8배**