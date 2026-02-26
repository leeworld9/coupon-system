package com.example.coupon.service;

import com.example.coupon.dto.CouponIssueRequestDto;
import com.example.coupon.dto.CouponIssueResponseDto;
import com.example.coupon.dto.CouponResponseDto;
import com.example.coupon.entity.Coupon;
import com.example.coupon.entity.CouponIssue;
import com.example.coupon.repository.CouponIssueRepository;
import com.example.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;

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
    public List<CouponIssueResponseDto> getMyCoupons(Long userId) {
        return couponIssueRepository.findAllByUserId(userId).stream()
                .map(CouponIssueResponseDto::new)
                .toList();
    }
}
