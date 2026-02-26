package com.example.coupon.controller;

import com.example.coupon.dto.CouponCreateRequestDto;
import com.example.coupon.dto.CouponResponseDto;
import com.example.coupon.service.AdminCouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final AdminCouponService adminCouponService;

    @PostMapping
    public ResponseEntity<CouponResponseDto> createCoupon(@Valid @RequestBody CouponCreateRequestDto request) {
        CouponResponseDto response = adminCouponService.createCoupon(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
