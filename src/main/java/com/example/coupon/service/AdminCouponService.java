package com.example.coupon.service;

import com.example.coupon.dto.CouponCreateRequestDto;
import com.example.coupon.dto.CouponResponseDto;
import com.example.coupon.entity.Coupon;
import com.example.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminCouponService {

    private final CouponRepository couponRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public CouponResponseDto createCoupon(CouponCreateRequestDto request) {
        // 1. DB에 쿠폰 저장
        Coupon coupon = Coupon.builder()
                .name(request.getName())
                .discountRate(request.getDiscountRate())
                .totalQuantity(request.getTotalQuantity())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        Coupon saved = couponRepository.save(coupon);

        // 2. Redis에 초기 재고 설정
        String stockKey = "coupon:stock:" + saved.getId();
        redisTemplate.opsForValue().set(stockKey,
                String.valueOf(request.getTotalQuantity()));

        return CouponResponseDto.from(saved);
    }
}
