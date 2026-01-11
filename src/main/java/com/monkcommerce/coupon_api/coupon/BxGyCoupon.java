package com.monkcommerce.coupon_api.coupon;

import com.monkcommerce.coupon_api.exception.CouponException;
import com.monkcommerce.coupon_api.model.Coupon;

import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class BxGyCoupon implements CouponHandler {

    private final Coupon coupon;
    private final String uniquenessKey;

    /**
     * Index used to ensure uniqueness of BXGY coupons
     * Format: buyProductIds|buyQty -> getProductIds|getQty
     */
    private final Set<String> bxgyIndex;
    private final Map<String, Coupon> couponMap;

    public BxGyCoupon(Coupon coupon, Set<String> bxgyIndex, Map<String, Coupon> couponMap) {
        this.uniquenessKey = generateUniqueKey(coupon);
        this.coupon = coupon;
        this.bxgyIndex = bxgyIndex;
        this.couponMap = couponMap;
    }

    @Override
    public void validateAndIndex() {

        // Validate First
        String exString = validation(coupon);
        if(exString != null) {
            throw new CouponException(exString);
        }

        // Index the active coupon after successful validation
        if(coupon.getDetails().isActive()) {
            bxgyIndex.add(uniquenessKey);
            couponMap.put(uniquenessKey, coupon);
        }
    }

    // Generate unique key for BxGy coupon || uniqueness key format: buyProductIds|buyQty -> getProductIds|getQty
    private String generateUniqueKey(Coupon temCoupon) {
        // Extract BXGY configuration
        List<Integer> buyProductIds = temCoupon.getDetails().buyProducts;
        List<Integer> getProductIds = temCoupon.getDetails().getProducts;
        Integer buyQuantity = temCoupon.getDetails().buyQuantity;
        Integer getQuantity = temCoupon.getDetails().getQuantity;

        // Generate deterministic key for uniqueness check
        String buyProductKey = buyProductIds.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        String getProductKey = getProductIds.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        return buyProductKey + "|" + buyQuantity + "->" + getProductKey + "|" + getQuantity;
    }

    // Validate the coupon before create and update
    private String validation(Coupon temCoupon) {
        // Extract BXGY configuration
        List<Integer> buyProductIds = temCoupon.getDetails().buyProducts;
        List<Integer> getProductIds = temCoupon.getDetails().getProducts;
        Integer buyQuantity = temCoupon.getDetails().buyQuantity;
        Integer getQuantity = temCoupon.getDetails().getQuantity;

        // Validation: Buy products must be present
        if (buyProductIds == null || buyProductIds.isEmpty()) {
            return "BXGY coupon must contain at least one buy product";
        }

        // Validation: Get products must be present
        if (getProductIds == null || getProductIds.isEmpty()) {
            return "BXGY coupon must contain at least one get product";
        }

        // Validation: Buy quantity must be positive
        if (buyQuantity == null || buyQuantity <= 0) {
            return "BXGY coupon must have a valid buyQuantity (> 0)";
        }

        // Validation: Get quantity must be positive
        if (getQuantity == null || getQuantity <= 0) {
            return "BXGY coupon must have a valid getQuantity (> 0)";
        }

        String newKey = generateUniqueKey(temCoupon);
        // Validation: Ensure no duplicate BXGY coupon exists
        if (bxgyIndex.contains(newKey)) {
            return "BXGY coupon already exists with the same buy/get products and quantities";
        }

        return null;
    }

    @Override
    public void validateAndUpdate(Coupon updateCoupon) {

        if(bxgyIndex.contains(uniquenessKey)) {
            bxgyIndex.remove(uniquenessKey);
        }

        String exString = validation(updateCoupon);
        if(exString != null) {
            if(coupon.getDetails().isActive()) {
                bxgyIndex.add(uniquenessKey);
            }
            throw new CouponException(exString);
        } 

        // unique key for update BxGy coupon 
        String newKey = generateUniqueKey(updateCoupon);

        // Index the active coupon after successful validation
        if(updateCoupon.getDetails().isActive()) {
            bxgyIndex.add(newKey);
            couponMap.put(newKey, updateCoupon);
        }
    }

    @Override
    public void removeFromIndex() {
        // unique key for update BxGy coupon 
        String newKey = generateUniqueKey(coupon);
        if(bxgyIndex.contains(newKey)) {
            bxgyIndex.remove(newKey);
            couponMap.remove(newKey);
        }
    }
}