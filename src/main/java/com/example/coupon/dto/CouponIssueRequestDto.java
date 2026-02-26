package com.example.coupon.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CouponIssueRequestDto {

    @NotNull(message = "유저 ID는 필수입니다.")
    @Positive(message = "유저 ID는 양수여야 합니다.")
    private Long userId;
}
