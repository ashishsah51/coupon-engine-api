package com.monkcommerce.coupon_api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Coupon {

    private long coupon_id;
    private CouponType type;
    private CouponDetails details;

    public long getId() {
        return coupon_id;
    }

    public CouponType getType() {
        return type;
    }

    public CouponDetails getDetails() {
        return details;
    }

    public void setId(long coupon_id) {
        this.coupon_id = coupon_id;
    }

    public void setDetails(CouponDetails details) {
        this.details = details;
    }

    public void setType(CouponType type) {
        this.type = type;
    }
}
