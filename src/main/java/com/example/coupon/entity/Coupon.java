package com.example.coupon.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer discountRate;

    @Column(nullable = false)
    private Integer totalQuantity;

    @Column(nullable = false)
    private Integer issuedQuantity = 0;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @Builder
    public Coupon(String name, Integer discountRate, Integer totalQuantity,
                  LocalDateTime startDate, LocalDateTime endDate) {
        this.name = name;
        this.discountRate = discountRate;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = 0;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public boolean isIssuable() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startDate) && now.isBefore(endDate)
                && issuedQuantity < totalQuantity;
    }

    public void increaseIssuedQuantity() {
        if (!isIssuable()) {
            throw new IllegalStateException("쿠폰 발급이 불가능합니다.");
        }
        this.issuedQuantity++;
    }
}
