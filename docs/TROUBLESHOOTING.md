# 트러블슈팅

프로젝트 진행 중 발생한 문제와 해결 과정을 기록합니다.

---

## 문제 1: Optimistic Lock 충돌

### 증상
```
org.springframework.orm.ObjectOptimisticLockingFailureException:
Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect)

Caused by: org.hibernate.StaleObjectStateException:
Row was updated or deleted by another transaction
```

**발생 시점:**
- Pessimistic Lock 방식 테스트 중
- Redis 방식 테스트 중
- 5000명 동시 요청 시 다수 발생

---

### 원인 분석

**1. Entity에 `@Version` 존재**
```java
@Entity
@Table(name = "coupons")
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version  // ← 문제의 원인!
    private Long version;

    // ...
}
```

**2. Optimistic Lock 자동 적용**

`@Version` 애노테이션이 있으면 JPA가 자동으로 Optimistic Lock 적용:
```sql
-- UPDATE 시 WHERE 절에 version 조건 추가
UPDATE coupons
SET issued_quantity = ?, version = version + 1
WHERE id = ? AND version = ?
```

**3. Pessimistic Lock과 충돌**
```java
// Pessimistic Lock 사용
@Lock(LockModeType.PESSIMISTIC_WRITE)
Coupon findById(Long id);
```

→ Pessimistic Lock + Optimistic Lock 이중 적용
→ 충돌 발생

**4. Redis 방식에서도 문제**
```java
@Transactional
public void issueCouponWithRedis() {
    // Redis로 검증
    redis.decrement();

    // DB 저장 시 version 체크
    coupon.increaseIssuedQuantity();  // ← UPDATE 발생
    // WHERE id = ? AND version = ?
    // → 다른 트랜잭션이 먼저 수정했으면 실패
}
```

---

### 해결

**@Version 제거:**
```java
// Before
@Entity
public class Coupon {
    @Version
    private Long version;  // ← 삭제
}

// After
@Entity
public class Coupon {
    // @Version 제거
}
```

Pessimistic Lock과 Redis로 이미 동시성 제어를 하고 있으므로 Optimistic Lock은 불필요.

---

### 왜 @Version을 제거했나?

**Optimistic Lock이 불필요한 이유:**

1. **Pessimistic Lock 방식**
   - DB Lock으로 이미 동시성 제어
   - 추가 version 체크는 중복

2. **Redis 방식**
   - Redis 원자적 연산으로 동시성 제어
   - DB는 최종 기록만 담당
   - version 체크 불필요

3. **충돌 발생**
   - 두 개의 Lock 메커니즘이 충돌
   - 성능 저하 및 에러 발생

---

### 결과

**수정 전:**
```
StaleObjectStateException 다수 발생
테스트 실패
```

**수정 후:**
```
Optimistic Lock 충돌 해결
5000명 동시 요청 처리 성공
```

---

## 💡 교훈

> **핵심:** Lock 전략은 하나만 선택!

### 1. Lock 전략 선택
- **Pessimistic Lock**
- **Optimistic Lock**
- **Redis 기반 동시성 제어**

이 중 **하나만** 사용해야 함

### 2. @Version 사용 시 주의
⚠️ Optimistic Lock이 필요한 경우에만 사용  
⚠️ 다른 Lock 방식과 혼용 금지

### 3. Entity 설계 원칙
✅ 동시성 제어 전략 먼저 결정  
✅ 그에 맞는 Entity 설계  
✅ 불필요한 애노테이션 제거

---

<details>
<summary>📚 <strong>참고: Optimistic Lock vs Pessimistic Lock</strong></summary>

### Optimistic Lock (낙관적 잠금)

**개념:**
- 충돌이 드물다고 가정
- 수정 시점에 version 체크
- 충돌 시 예외 발생 → 재시도

**장점:**
- Lock 대기 없음
- 읽기 성능 우수

**단점:**
- 충돌 시 롤백 및 재시도 필요
- 충돌이 잦으면 비효율

**적합한 경우:**
- 읽기가 많고 쓰기가 적은 경우
- 충돌이 드문 경우

### Pessimistic Lock (비관적 잠금)

**개념:**
- 충돌이 자주 발생한다고 가정
- 읽기 시점에 Lock 획득
- 처리 완료 후 Lock 해제

**장점:**
- 충돌 방지 확실
- 재시도 불필요

**단점:**
- Lock 대기 시간
- 읽기/쓰기 성능 저하

**적합한 경우:**
- 쓰기가 많은 경우
- 충돌이 빈번한 경우

### 본 프로젝트의 선택

**선택:** Redis 기반 동시성 제어

**이유:**
1. Lock 없는 원자적 연산
2. 메모리 기반 빠른 처리
3. 분산 환경 지원

**결과:**
- TPS 2.8배 향상
- 응답시간 71% 단축

</details>

