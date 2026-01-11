package com.monkcommerce.coupon_api.model.response;

public class ApplicableCouponItem {
    public long coupon_id;
    public String type;
    public double discount;

    public ApplicableCouponItem(long coupon_id, String type, double discount) {
        this.coupon_id = coupon_id;
        this.type = type;
        this.discount = discount;
    }
}
