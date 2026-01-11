package com.monkcommerce.coupon_api.model.response;

import java.util.List;
import com.monkcommerce.coupon_api.model.cart.CartItem;

public class ApplyCouponResponse {

    private List<CartItem> items;
    private double totalPrice;
    private double totalDiscount;
    private double finalPrice;

    public ApplyCouponResponse(List<CartItem> items,
                        double totalPrice,
                        double totalDiscount,
                        double finalPrice) {
        this.items = items;
        this.totalPrice = totalPrice;
        this.totalDiscount = totalDiscount;
        this.finalPrice = finalPrice;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public double getTotalDiscount() {
        return totalDiscount;
    }

    public double getFinalPrice() {
        return finalPrice;
    }
}
