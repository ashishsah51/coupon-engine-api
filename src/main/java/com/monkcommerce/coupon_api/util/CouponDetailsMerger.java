package com.monkcommerce.coupon_api.util;

import com.monkcommerce.coupon_api.model.CouponDetails;

public class CouponDetailsMerger {

    public static void merge(CouponDetails target, CouponDetails source) {
        if (source == null) return;

        /* -------- COMMON -------- */
        if (source.isActive != null && target.isActive == null) {
            target.isActive = source.isActive;
        }

        if (source.startDate != null && target.startDate == null) {
            target.startDate = source.startDate;
        }

        if (source.expiryDate != null && target.expiryDate == null) {
            target.expiryDate = source.expiryDate;
        }

        /* -------- CART-WISE -------- */
        if (source.threshold != null && target.threshold == null) {
            target.threshold = source.threshold;
        }

        if (source.discount != null && target.discount == null) {
            target.discount = source.discount;
        }

        /* -------- PRODUCT-WISE -------- */
        if (source.productId != null && target.productId == null) {
            target.productId = source.productId;
        }

        /* -------- BXGY -------- */
        if (source.buyProducts != null && target.buyProducts == null) {
            target.buyProducts = source.buyProducts;
        }

        if (source.buyQuantity != null && target.buyQuantity == null) {
            target.buyQuantity = source.buyQuantity;
        }

        if (source.getProducts != null && target.getProducts == null) {
            target.getProducts = source.getProducts;
        }

        if (source.getQuantity != null && target.getQuantity == null) {
            target.getQuantity = source.getQuantity;
        }

        if (source.repetitionLimit != null && target.repetitionLimit == null) {
            target.repetitionLimit = source.repetitionLimit;
        }
    }
}
