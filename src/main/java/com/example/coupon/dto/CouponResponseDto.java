package com.example.coupon.dto;

import com.example.coupon.entity.Coupon;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CouponResponseDto {

    private final Long id;
    private final String name;
    private final Integer discountRate;
    private final Integer totalQuantity;
    private final Integer issuedQuantity;
    private final Integer remainingQuantity;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final LocalDateTime createdAt;

    public CouponResponseDto(Coupon coupon) {
        this.id = coupon.getId();
        this.name = coupon.getName();
        this.discountRate = coupon.getDiscountRate();
        this.totalQuantity = coupon.getTotalQuantity();
        this.issuedQuantity = coupon.getIssuedQuantity();
        this.remainingQuantity = coupon.getTotalQuantity() - coupon.getIssuedQuantity();
        this.startDate = coupon.getStartDate();
        this.endDate = coupon.getEndDate();
        this.createdAt = coupon.getCreatedAt();
    }

    public static CouponResponseDto from(Coupon coupon) {
        return new CouponResponseDto(coupon);
    }
}
