package com.example.coupon.repository;

import com.example.coupon.entity.CouponIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long> {

    boolean existsByUserIdAndCouponId(Long userId, Long couponId);

    List<CouponIssue> findAllByUserId(Long userId);

    Optional<CouponIssue> findByUserIdAndCouponId(Long userId, Long couponId);
}
