package com.monkcommerce.coupon_api.store;

import java.util.*;
import com.monkcommerce.coupon_api.model.Coupon;

//IN-MEMORY INDEX STORE

public class CouponIndexes {

    // threshold → discount
    public final TreeMap<Integer, Double> cartIndex = new TreeMap<>();

    // productId → discount
    public final Map<Integer, Double> productIndex = new HashMap<>();

    // compositeKey → true
    public final Set<String> bxgyIndex = new HashSet<>();

    // Id vs Coupon
    public final Map<String, Coupon> couponMap = new HashMap<>();
}
