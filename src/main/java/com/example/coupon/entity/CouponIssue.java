package com.example.coupon.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "coupon_issues",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_coupon_issue_user_coupon",
            columnNames = {"user_id", "coupon_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime issueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponIssueStatus status;

    @Builder
    public CouponIssue(Long couponId, Long userId) {
        this.couponId = couponId;
        this.userId = userId;
        this.issueDate = LocalDateTime.now();
        this.status = CouponIssueStatus.ISSUED;
    }

    public void use() {
        if (this.status != CouponIssueStatus.ISSUED) {
            throw new IllegalStateException("이미 사용된 쿠폰입니다.");
        }
        this.status = CouponIssueStatus.USED;
    }
}
