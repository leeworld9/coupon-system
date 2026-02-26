package com.example.coupon.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class CouponCreateRequestDto {

    @NotBlank(message = "쿠폰명은 필수입니다.")
    private String name;

    @NotNull(message = "할인율은 필수입니다.")
    @Min(value = 1, message = "할인율은 1% 이상이어야 합니다.")
    @Max(value = 100, message = "할인율은 100% 이하여야 합니다.")
    private Integer discountRate;

    @NotNull(message = "총 발급 수량은 필수입니다.")
    @Positive(message = "총 발급 수량은 1개 이상이어야 합니다.")
    private Integer totalQuantity;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDateTime startDate;

    @NotNull(message = "종료일은 필수입니다.")
    private LocalDateTime endDate;
}
