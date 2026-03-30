# 트러블슈팅

프로젝트 진행 중 발생한 문제와 해결 과정을 기록합니다.

---

## 문제: Optimistic Lock 충돌

### 증상
```
StaleObjectStateException: Row was updated or deleted by another transaction
```

5000명 동시 요청 테스트 시 다수 발생

---

### 원인

**1. Entity에 `@Version` 존재**
```java
@Entity
public class Coupon {
    @Version
    private Long version;  // ← 문제!
}
```

**2. Optimistic Lock 자동 적용**

`@Version`이 있으면 JPA가 자동으로 Optimistic Lock 적용:
```sql
UPDATE coupons 
SET issued_quantity = ?, version = version + 1 
WHERE id = ? AND version = ?
```

**3. 다른 Lock 방식과 충돌**

- **Pessimistic Lock**: DB Lock + Optimistic Lock 이중 적용 → 충돌
- **Redis 방식**: Redis 동시성 제어 + Optimistic Lock → 충돌

---

### 해결

**@Version 제거:**
```java
// Before
@Entity
public class Coupon {
    @Version
    private Long version;  // 삭제
}

// After
@Entity
public class Coupon {
    // @Version 제거
}
```

Pessimistic Lock과 Redis로 이미 동시성 제어를 하고 있으므로 Optimistic Lock 불필요.

---

### 결과
```
Optimistic Lock 충돌 해결
5000명 동시 요청 처리 성공
```

---

## 💡 교훈

**Lock 전략은 하나만 선택하고 일관되게 사용.**

`@Version`은 Optimistic Lock 전용이므로 다른 방식과 혼용 금지.