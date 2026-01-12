package com.monkcommerce.coupon_api.service;

import com.monkcommerce.coupon_api.coupon.CouponHandler;
import com.monkcommerce.coupon_api.coupon.ProductWiseCoupon;
import com.monkcommerce.coupon_api.exception.CouponException;
import com.monkcommerce.coupon_api.factory.CouponFactory;
import com.monkcommerce.coupon_api.model.Coupon;
import com.monkcommerce.coupon_api.model.CouponDetails;
import com.monkcommerce.coupon_api.model.cart.Cart;
import com.monkcommerce.coupon_api.model.cart.CartItem;
import com.monkcommerce.coupon_api.model.response.ApplicableCouponItem;
import com.monkcommerce.coupon_api.model.response.ApplicableCouponsResponse;
import com.monkcommerce.coupon_api.model.response.ApplyCouponResponse;
import com.monkcommerce.coupon_api.store.CouponIndexes;
import com.monkcommerce.coupon_api.coupon.BxGyCoupon;
import com.monkcommerce.coupon_api.coupon.CartWiseCoupon;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

public class CouponService {

    // In-memory store: couponId -> coupon
    private final Map<Long, Coupon> store = new HashMap<>();
    private final CouponIndexes indexes = new CouponIndexes();
    private long idSeq = 1;

    //Create a new coupon
    public Coupon createCoupon(Coupon coupon) {

        // Assign ID
        coupon.setId(idSeq++);

        // Get the proper handler based on coupon type
        CouponHandler handler = CouponFactory.create(coupon, indexes);

        // Validate and index in-memory
        handler.validateAndIndex();

        // Store in memory
        store.put(coupon.getId(), coupon);

        return coupon;
    }

    /**
     * Get all active coupons based on date and isActive flag
     */
    @GetMapping
    public List<Coupon> getCoupon(boolean active) {
        return store.values()
            .stream()
            .filter(c -> c != null && c.getDetails() != null && c.getType() != null)
            .filter(c -> {
                Boolean isActive = c.getDetails().isActive == null || c.getDetails().isActive;
                return Boolean.TRUE.equals(isActive) == active;
            })
            .collect(Collectors.toList());
    }

    //  Get specific coupons with id 
    @GetMapping
    public Coupon getCouponById(long couponId) {
        Coupon coupon = store.get(couponId);
        if (coupon == null) {
            throw new CouponException("Coupon not available with id: " + couponId);
        }
        return coupon;
    }

    // Update Coupon
    @PutMapping
    public Coupon updateCouponById(long couponId, Coupon updatedCoupon) {

        // Fetch existing coupon
        Coupon existing = store.get(couponId);
        if (existing == null) {
            throw new CouponException("Coupon not available with id: " + couponId);
        }

        // Coupon type cannot change
        if (updatedCoupon.getType() != null && existing.getType() != updatedCoupon.getType()) {
            throw new CouponException("Coupon type cannot be modified");
        }

        // Merge existing coupon that are not changes
        updatedCoupon.setId(couponId);
        updatedCoupon.setDetails(merge(
            updatedCoupon.getDetails(),
            existing.getDetails()
        ));

        // Get the proper handler based on coupon type
        CouponHandler handler = CouponFactory.create(existing, indexes);

        // Validate and update index in-memory
        handler.validateAndUpdate(updatedCoupon);

        // Store in memory
        store.put(couponId, updatedCoupon);

        return updatedCoupon;
    }

    // Update Coupon
    @DeleteMapping
    public Coupon deleteCoupon(long couponId) {

        Coupon existing = store.get(couponId);
        if (existing == null) {
            throw new CouponException("Coupon not available with id: " + couponId);
        }

        // Remove from index ONLY if coupon is active
        if (existing.getDetails().isActive()) {
            CouponHandler handler = CouponFactory.create(existing, indexes);
            handler.removeFromIndex();
        }

        store.remove(couponId);
        return existing;
    }

   
    // Applicable all product wise coupon, best Cart-wise coupon and all BXGY coupon and get best discount || return the result with highest discount first in order
    @PostMapping
    public ApplicableCouponsResponse getApplicableCoupons(Cart cart) {

        if (cart == null || cart.items == null || cart.items.isEmpty()) {
            throw new CouponException("Cart items cannot be empty");
        }

        List<CartItem> cartItems = cart.items;
        double totalPrice = 0.0;
        ApplicableCouponsResponse response = new ApplicableCouponsResponse(new ArrayList<>());
        Map<Integer, Double> productCouponIndex = indexes.productIndex;
        Map<String, Coupon> couponMap = indexes.couponMap;

        for (CartItem item : cartItems) {
            if (item == null || item.price<=0 || item.quantity <= 0) {
                throw new CouponException("Invalid cart item data");
            }
            totalPrice += item.price * item.quantity;
            // Apply all product wise coupon
            if (productCouponIndex.containsKey(item.productId)) {
                double percent = productCouponIndex.get(item.productId);
                double discount = (item.price * item.quantity) * percent / 100;
                Coupon coupon = couponMap.get("" + item.productId);
                response.applicable_coupons.add(
                new ApplicableCouponItem(
                        coupon.getId(),
                        "PRODUCT_WISE",
                        discount
                    )
                );
            }
        }

        // Apply nearest threshold coupon instead of all
        TreeMap<Integer, Double> cartIndex = indexes.cartIndex;
        Map.Entry<Integer, Double> entry = cartIndex.floorEntry((int) totalPrice);
        if (entry != null) {
            double percent = entry.getValue();
            double discount = totalPrice * percent / 100;
            Coupon coupon = couponMap.get("" + entry.getKey());
            response.applicable_coupons.add(
                new ApplicableCouponItem(
                    coupon.getId(),
                    "CART_WISE",
                    discount
                )
            );
        }

        // Apply BXGY coupon
        for (String key : indexes.bxgyIndex) {
            Coupon coupon = couponMap.get(key);
            BxGyCoupon handler = new BxGyCoupon(coupon, null, null);
            ApplyCouponResponse bxgyResponse = handler.getApplyCouponOnCart(coupon, cart);
            response.applicable_coupons.add(
                new ApplicableCouponItem(
                    coupon.getId(),
                    "BXGY",
                    bxgyResponse.getTotalDiscount()
                )
            );
        }
        Collections.sort(response.applicable_coupons, (a, b) -> Double.compare(b.discount, a.discount));
        return response;
    }

    // Apply coupons with provided coupon id and get maximum discount
    @PostMapping
    public ApplyCouponResponse applyCouponToCart(long couponId, Cart cart) {

        if (cart == null || cart.items == null || cart.items.isEmpty()) {
            throw new CouponException("Cart items cannot be empty");
        }

        Coupon coupon = store.get(couponId);
        if (coupon == null || coupon.getDetails() == null || !coupon.getDetails().isActive()) {
            throw new CouponException("Coupon not found or inactive");
        }

        String ctype = coupon.getType().name();
        if (ctype.equals("CART_WISE")) { // For CART WISE
            CartWiseCoupon handler = new CartWiseCoupon(coupon, null, null);
            return handler.getApplyCouponOnCart(coupon, cart);
        } else if (ctype.equals("PRODUCT_WISE")) { // For PRODUCT WISE
            ProductWiseCoupon handler = new ProductWiseCoupon(coupon, null, null);
            return handler.getApplyCouponOnCart(coupon, cart);
        } else if (ctype.equals("BXGY")) { // For BXGY
            BxGyCoupon handler = new BxGyCoupon(coupon, null, null);
            return handler.getApplyCouponOnCart(coupon, cart);
        } else {
            throw new CouponException("Unsupported coupon type");
        }
    }



    public CouponDetails merge(CouponDetails target, CouponDetails source) {
        if (source == null) return target;

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

        return target;
    }
}
