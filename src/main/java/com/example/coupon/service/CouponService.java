package com.example.coupon.service;

import com.example.coupon.dto.CouponIssueRequestDto;
import com.example.coupon.dto.CouponIssueResponseDto;
import com.example.coupon.dto.CouponResponseDto;
import com.example.coupon.entity.Coupon;
import com.example.coupon.entity.CouponIssue;
import com.example.coupon.repository.CouponIssueRepository;
import com.example.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional(readOnly = true)
    public List<CouponResponseDto> getCoupons() {
        return couponRepository.findAll().stream()
                .map(CouponResponseDto::new)
                .toList();
    }

    @Transactional
    public CouponIssueResponseDto issueCoupon(Long couponId, CouponIssueRequestDto request) {
        if (couponIssueRepository.existsByUserIdAndCouponId(request.getUserId(), couponId)) {
            throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
        }

        Coupon coupon = couponRepository.findByIdWithPessimisticLock(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다. id=" + couponId));

        coupon.increaseIssuedQuantity();

        CouponIssue couponIssue = CouponIssue.builder()
                .couponId(couponId)
                .userId(request.getUserId())
                .build();

        CouponIssue saved = couponIssueRepository.save(couponIssue);
        return new CouponIssueResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStock(Long couponId) {
        // Redis 재고
        String stockKey = "coupon:stock:" + couponId;
        String redisStock = redisTemplate.opsForValue().get(stockKey);

        // DB 재고
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        return Map.of(
                "couponId", couponId,
                "redisStock", redisStock != null ? redisStock : "N/A",
                "dbIssuedQuantity", coupon.getIssuedQuantity(),
                "dbTotalQuantity", coupon.getTotalQuantity(),
                "dbRemainingQuantity", coupon.getTotalQuantity() - coupon.getIssuedQuantity()
        );
    }


    @Transactional(readOnly = true)
    public List<CouponIssueResponseDto> getMyCoupons(Long userId) {
        return couponIssueRepository.findAllByUserId(userId).stream()
                .map(CouponIssueResponseDto::new)
                .toList();
    }

    public CouponIssueResponseDto issueCouponWithRedis(Long couponId, CouponIssueRequestDto request) {
        Long userId = request.getUserId();

        // 1. Redis 검증/차감 (트랜잭션 밖에서)
        validateAndDecrementStock(couponId, userId);

        // 2. DB 저장 (트랜잭션 안에서)
        return saveToDatabase(couponId, userId);
    }

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

    // 트랜잭션! DB만 처리
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
}
