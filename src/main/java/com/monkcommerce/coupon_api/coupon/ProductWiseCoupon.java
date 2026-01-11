package com.monkcommerce.coupon_api.coupon;

import com.monkcommerce.coupon_api.exception.CouponException;
import com.monkcommerce.coupon_api.model.Coupon;

import java.util.Map;

public class ProductWiseCoupon implements CouponHandler {

    private final Coupon coupon;

    /**
     * Index to track active product-wise coupons
     * Key   : productId
     * Value : discount percentage
     */
    private final Map<Integer, Double> productCouponIndex;
    private final Map<String, Coupon> couponMap;

    public ProductWiseCoupon(Coupon coupon, Map<Integer, Double> productCouponIndex, Map<String, Coupon> couponMap) {
        this.coupon = coupon;
        this.productCouponIndex = productCouponIndex;
        this.couponMap = couponMap;
    }

    @Override
    public void validateAndIndex() {

        Integer productId = coupon.getDetails().productId;
        Double discount = coupon.getDetails().discount;

        // Validate First
        String exString = validation(coupon);
        if(exString != null) {
            throw new CouponException(exString);
        }

        if(coupon.getDetails().isActive()) {
            productCouponIndex.put(productId, discount);
            couponMap.put(productId+"", coupon);
        }
    }

     // Validate the coupon before create and update
    private String validation(Coupon temCoupon) {

        Integer productId = temCoupon.getDetails().productId;
        Double discount = temCoupon.getDetails().discount;

        // Validation: Product ID must be present
        if (productId == null) {
            return "Product-wise coupon must have a valid productId";
        }

        // Validation: Discount must be present and positive
        if (discount == null || discount <= 0) {
            return "Product-wise coupon must have a valid discount (> 0)";
        }

        // Validation: Only one active coupon allowed per product
        if (productCouponIndex.containsKey(productId)) {
            return "Active product-wise coupon already exists for productId " + productId;
        }

        return null;
    }

    @Override
    public void validateAndUpdate(Coupon updateCoupon) {

        if(productCouponIndex.containsKey(coupon.getDetails().productId)) {
            productCouponIndex.remove(coupon.getDetails().productId);
        }

        // Validate First
        String exString = validation(updateCoupon);

        if(exString != null) {
            if(coupon.getDetails().isActive()) {
                productCouponIndex.put(coupon.getDetails().productId, coupon.getDetails().discount);
            }
            throw new CouponException(exString);
        } 

        if(updateCoupon.getDetails().isActive) {
            productCouponIndex.put(updateCoupon.getDetails().productId, updateCoupon.getDetails().discount);
            couponMap.put(updateCoupon.getDetails().productId+"", updateCoupon);
        }

    }

    @Override
    public void removeFromIndex() {
        Integer threshold = coupon.getDetails().threshold;
        if(productCouponIndex.containsKey(threshold)) {
            productCouponIndex.remove(threshold);
            couponMap.remove(threshold+"");
        }
    }

    
}
