package com.monkcommerce.coupon_api.model.response;

import java.util.List;

public class ApplicableCouponsResponse {

    public List<ApplicableCouponItem> applicable_coupons;

    public ApplicableCouponsResponse(List<ApplicableCouponItem> applicableCoupons) {
        this.applicable_coupons = applicableCoupons;
    }
}
