package com.monkcommerce.coupon_api.coupon;

import com.monkcommerce.coupon_api.exception.CouponException;
import com.monkcommerce.coupon_api.model.Coupon;
import com.monkcommerce.coupon_api.model.cart.Cart;
import com.monkcommerce.coupon_api.model.cart.CartItem;
import com.monkcommerce.coupon_api.model.response.ApplyCouponResponse;

import java.util.Map;
import java.util.TreeMap;

public class CartWiseCoupon implements CouponHandler {

    private final Coupon coupon;
    private final TreeMap<Integer, Double> cartDiscountIndex;
    private final Map<String, Coupon> couponMap;

    public CartWiseCoupon(Coupon coupon, TreeMap<Integer, Double> cartDiscountIndex, Map<String, Coupon> couponMap) {
        this.coupon = coupon;
        this.cartDiscountIndex = cartDiscountIndex;
        this.couponMap = couponMap;
    }

    @Override
    public void validateAndIndex() {

        int cartThreshold = coupon.getDetails().threshold;
        double discountPercentage = coupon.getDetails().discount;

        // Validate First
        String exString = validation(coupon);
        if(exString != null) {
            throw new CouponException(exString);
        }
        
        // Index the valid cart-wise active coupon
        if(coupon.getDetails().isActive()) {
            cartDiscountIndex.put(cartThreshold, discountPercentage);
            couponMap.put(cartThreshold+"", coupon);
        }
    }

    // Validate the coupon before create and update
    private String validation(Coupon temCoupon) {

        int cartThreshold = temCoupon.getDetails().threshold;
        double discountPercentage = temCoupon.getDetails().discount;

        // Only one coupon is allowed per cart threshold.
        if (cartDiscountIndex.containsKey(cartThreshold)) {
            return String.format("Cart-wise coupon already exists for cart threshold %d",cartThreshold);
        }

        if(cartThreshold <= 0 || discountPercentage < 0 || discountPercentage > 100) {
            return String.format("Invalid cart-wise coupon: cart threshold must be greater than 0 and discount percentage must be between 0 and 100.");
        }

        // Fetch nearest lower and higher thresholds (O(log n))
        Map.Entry<Integer, Double> lowerThresholdEntry = cartDiscountIndex.lowerEntry(cartThreshold);

        Map.Entry<Integer, Double> higherThresholdEntry = cartDiscountIndex.higherEntry(cartThreshold);

        // A lower cart threshold must NOT provide a higher or equal discount percentage.
        // Otherwise, customers would get better discounts for spending less.
        if (lowerThresholdEntry != null &&
                lowerThresholdEntry.getValue() >= discountPercentage) {
                return String.format("Invalid cart-wise coupon: lower cart threshold %d has higher or equal discount %.2f%% than new coupon discount %.2f%%",lowerThresholdEntry.getKey(),lowerThresholdEntry.getValue(),discountPercentage);
        }

        // A higher cart threshold must NOT provide a lower or equal discount percentage.
        // Otherwise, customers spending more would get worse discounts.
        if (higherThresholdEntry != null &&
                higherThresholdEntry.getValue() <= discountPercentage) {
                return String.format("Invalid cart-wise coupon: higher cart threshold %d has lower or equal discount %.2f%% than new coupon discount %.2f%%",
                                higherThresholdEntry.getKey(),
                                higherThresholdEntry.getValue(),
                                discountPercentage
                            );
        }
        return null;
    }

    @Override
    public void validateAndUpdate(Coupon updateCoupon) {
        if(cartDiscountIndex.containsKey(coupon.getDetails().threshold)) {
            cartDiscountIndex.remove(coupon.getDetails().threshold);
        }

        // Validate First
        String exString = validation(updateCoupon);
        if(exString != null) {
            if(coupon.getDetails().isActive()) {
                cartDiscountIndex.put(coupon.getDetails().threshold, coupon.getDetails().discount);
                couponMap.put(coupon.getDetails().threshold+"", coupon);
            }
            throw new CouponException(exString);
        } 

        if(updateCoupon.getDetails().isActive()) {
            cartDiscountIndex.put(updateCoupon.getDetails().threshold, updateCoupon.getDetails().discount);
            couponMap.put(updateCoupon.getDetails().threshold+"", updateCoupon);
        }
    }

    @Override
    public void removeFromIndex() {
        Integer threshold = coupon.getDetails().threshold;
        if(cartDiscountIndex.containsKey(threshold)) {
            cartDiscountIndex.remove(threshold);
            couponMap.remove(threshold+"");
        }
    }

    // Get Discount after applyting coupon on cart.
    public ApplyCouponResponse getApplyCouponOnCart(Coupon coupon, Cart cart) {
        double totalPrice = 0.00, totalDiscount = 0.00;
        for (CartItem item : cart.items) {
            if (item == null || item.price<=0 || item.quantity <= 0) throw new CouponException("Invalid cart item data");
            totalPrice += item.price * item.quantity;
            item.totalDiscount = 0.0;
        }
        totalDiscount = (totalPrice * coupon.getDetails().discount) / 100.0;
        return new ApplyCouponResponse(
                cart.items,
                totalPrice,
                totalDiscount,
                totalPrice - totalDiscount
        );
    }

}
