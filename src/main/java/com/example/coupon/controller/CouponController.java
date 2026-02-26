package com.example.coupon.controller;

import com.example.coupon.dto.CouponIssueRequestDto;
import com.example.coupon.dto.CouponIssueResponseDto;
import com.example.coupon.dto.CouponResponseDto;
import com.example.coupon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping
    public ResponseEntity<List<CouponResponseDto>> getCoupons() {
        return ResponseEntity.ok(couponService.getCoupons());
    }

    @PostMapping("/{couponId}/issue")
    public ResponseEntity<CouponIssueResponseDto> issueCoupon(
            @PathVariable Long couponId,
            @Valid @RequestBody CouponIssueRequestDto request) {
        CouponIssueResponseDto response = couponService.issueCoupon(couponId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-coupons")
    public ResponseEntity<List<CouponIssueResponseDto>> getMyCoupons(
            @RequestParam Long userId) {
        return ResponseEntity.ok(couponService.getMyCoupons(userId));
    }
}
