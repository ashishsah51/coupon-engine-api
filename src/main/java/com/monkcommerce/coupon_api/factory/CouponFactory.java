package com.monkcommerce.coupon_api.factory;

import com.monkcommerce.coupon_api.coupon.*;
import com.monkcommerce.coupon_api.model.Coupon;
import com.monkcommerce.coupon_api.store.CouponIndexes;

public class CouponFactory {

    public static CouponHandler create(Coupon coupon, CouponIndexes indexes) {

        return switch (coupon.getType()) {

            case CART_WISE ->
                    new CartWiseCoupon(coupon, indexes.cartIndex, indexes.couponMap);

            case PRODUCT_WISE ->
                    new ProductWiseCoupon(coupon, indexes.productIndex, indexes.couponMap);

            case BXGY ->
                    new BxGyCoupon(coupon, indexes.bxgyIndex, indexes.couponMap);
        };
    }
}
