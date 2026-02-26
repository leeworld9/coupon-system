package com.example.coupon.service;

import com.example.coupon.dto.CouponIssueResponseDto;
import com.example.coupon.entity.Coupon;
import com.example.coupon.entity.CouponIssue;
import com.example.coupon.entity.CouponIssueStatus;
import com.example.coupon.repository.CouponIssueRepository;
import com.example.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CouponIssueService {

    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;

    @Transactional
    public CouponIssueResponseDto issueWithPessimisticLock(
            Long couponId, Long userId) {

        // 1. 쿠폰 조회 + Lock
        Coupon coupon = couponRepository
                .findByIdWithPessimisticLock(couponId)
                .orElseThrow(() -> new RuntimeException("쿠폰 없음"));

        // 2. 재고 확인
        if (coupon.getIssuedQuantity() >= coupon.getTotalQuantity()) {
            throw new RuntimeException("품절");
        }

        // 3. 중복 발급 체크
        if (couponIssueRepository
                .existsByUserIdAndCouponId(userId, couponId)) {
            throw new RuntimeException("이미 발급받음");
        }

        // 4. 발급 처리
        CouponIssue issue = CouponIssue.builder()
                .couponId(couponId)
                .userId(userId)
                .build();

        couponIssueRepository.save(issue);

        // 5. 재고 증가
        coupon.increaseIssuedQuantity();

        return CouponIssueResponseDto.from(issue);
    }
}
