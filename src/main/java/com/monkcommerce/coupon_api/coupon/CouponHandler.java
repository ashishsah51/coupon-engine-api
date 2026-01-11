package com.monkcommerce.coupon_api.coupon;

import com.monkcommerce.coupon_api.model.Coupon;

public interface CouponHandler {

    /**
     * Validates the coupon against business rules
     * and indexes it if valid.
     *
     * @throws com.monkcommerce.coupon_api.exception.CouponException
     *         if validation fails
     */
    void validateAndIndex();

    // Validates the coupon against business rules and update it if valid
    void validateAndUpdate(Coupon coupon);

    void removeFromIndex();
}
