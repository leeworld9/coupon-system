package com.example.coupon.dto;

import com.example.coupon.entity.Coupon;
import com.example.coupon.entity.CouponIssue;
import com.example.coupon.entity.CouponIssueStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CouponIssueResponseDto {

    private final Long id;
    private final Long couponId;
    private final Long userId;
    private final LocalDateTime issueDate;
    private final CouponIssueStatus status;

    public CouponIssueResponseDto(CouponIssue couponIssue) {
        this.id = couponIssue.getId();
        this.couponId = couponIssue.getCouponId();
        this.userId = couponIssue.getUserId();
        this.issueDate = couponIssue.getIssueDate();
        this.status = couponIssue.getStatus();
    }

    public static CouponIssueResponseDto from(CouponIssue couponIssue) {
        return new CouponIssueResponseDto(couponIssue);
    }
}
