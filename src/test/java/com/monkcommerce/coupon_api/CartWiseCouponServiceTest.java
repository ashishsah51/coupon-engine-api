package com.monkcommerce.coupon_api;

import com.monkcommerce.coupon_api.exception.CouponException;
import com.monkcommerce.coupon_api.model.Coupon;
import com.monkcommerce.coupon_api.model.CouponDetails;
import com.monkcommerce.coupon_api.model.CouponType;
import com.monkcommerce.coupon_api.model.cart.Cart;
import com.monkcommerce.coupon_api.model.cart.CartItem;
import com.monkcommerce.coupon_api.model.response.ApplicableCouponsResponse;
import com.monkcommerce.coupon_api.model.response.ApplyCouponResponse;

import org.junit.jupiter.api.BeforeEach;
import com.monkcommerce.coupon_api.service.CouponService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CartWiseCouponServiceTest {

    private CouponService service;

    @BeforeEach
    void setup() {
        service = new CouponService();
    }

    // Helper to build cart-wise coupon
    private Coupon buildCartCoupon(int threshold, double discount, Boolean isActive) {
        CouponDetails details = new CouponDetails();
        details.threshold = threshold;
        details.discount = discount;
        details.isActive = isActive;

        Coupon coupon = new Coupon();
        coupon.setType(CouponType.CART_WISE);
        coupon.setDetails(details);
        return coupon;
    }

    @Test
    void cartwiseCouponCreationFlow() {
        // Step 1: inactive coupon
        Coupon inactive = buildCartCoupon(100, 10, false);
        Coupon c1 = service.createCoupon(inactive);
        assertFalse(c1.getDetails().isActive);

        // Step 2: same coupon active → allowed
        Coupon active = buildCartCoupon(100, 10, true);
        Coupon c2 = service.createCoupon(active);
        assertTrue(c2.getDetails().isActive);

        // Step 3: invalid coupon → lower threshold higher discount → exception
        Coupon invalid = buildCartCoupon(80, 15, true);
        try {
            service.createCoupon(invalid);
        } catch (CouponException e) {
            assertTrue(e.getMessage().contains("higher cart threshold"));
        }

        // Step 4: another inactive coupon → check
        Coupon inactive2 = buildCartCoupon(120, 5, false);
        try {
            service.createCoupon(inactive2);
        } catch (CouponException e1) {
            assertTrue(e1.getMessage().contains("lower cart threshold"));
        }
    }

    @Test
    void cartwiseCouponGetFlow() {
        // Pre-create coupons
        service.createCoupon(buildCartCoupon(100, 10, false));
        service.createCoupon(buildCartCoupon(100, 10, true));
        service.createCoupon(buildCartCoupon(200, 12, true));

        // Fetch active coupons
        List<Coupon> activeCoupons = service.getCoupon(true);
        assertEquals(2, activeCoupons.size());
        assertTrue(activeCoupons.get(0).getDetails().isActive());

        // Fetch inactive coupons
        List<Coupon> inactiveCoupons = service.getCoupon(false);
        assertEquals(1, inactiveCoupons.size());
    }

    @Test
    void couponGetByIdFlow() {
        // Pre-create coupons
        service.createCoupon(buildCartCoupon(100, 10, false));
        service.createCoupon(buildCartCoupon(100, 10, true));

        // Fetch in active coupons
        Coupon coupon = service.getCouponById(1);
        assertFalse(coupon.getDetails().isActive());

        // Fetch active coupons
        coupon = service.getCouponById(2);
        assertTrue(coupon.getDetails().isActive());

        // Invalid Id
        try {
            service.getCouponById(3);
        } catch (CouponException ex) {
            assertTrue(ex.getMessage().equals("Coupon not available with id: 3"));
        }
    }

    @Test
    void couponDelete() {
        // Pre-create coupons
        service.createCoupon(buildCartCoupon(100, 10, false));

        // Fetch in active coupons
        Coupon coupon = service.deleteCoupon(1);
        assertFalse(coupon.getDetails().isActive());

        // Invalid Id
        try {
            service.deleteCoupon(2);
        } catch (CouponException ex) {
            assertTrue(ex.getMessage().contains("Coupon not available with id:"));
        }
    }

    @Test
    void cartWiseApplyCoupon() {
        // Pre-create coupons
        service.createCoupon(buildCartCoupon(200, 20, true));
        service.createCoupon(buildCartCoupon(100, 10, true));


        // Create cart
        Cart cart = new Cart();
        cart.items = new ArrayList<CartItem>();
        CartItem ci1 = new CartItem();
        ci1.productId = 2;
        ci1.price = 50.00;
        ci1.quantity = 7;

        cart.items.add(ci1);

        CartItem ci2 = new CartItem();
        ci2.productId = 3;
        ci2.price = 25.00;
        ci2.quantity = 7;

        cart.items.add(ci2);

        CartItem ci3 = new CartItem();
        ci3.productId = 4;
        ci3.price = 25.00;
        ci3.quantity = 3;

        cart.items.add(ci3);

        ApplicableCouponsResponse response = service.getApplicableCoupons(cart);
        assertTrue(response.applicable_coupons.size() > 0);

        // Invalid Id
        try {
            cart.items.add(null);
            service.getApplicableCoupons(cart);
        } catch (CouponException ex) {
            assertTrue(ex.getMessage().equals("Invalid cart item data"));
        }

        // cart null
        try {
            service.getApplicableCoupons( null);
        } catch (CouponException ex) {
            assertTrue(ex.getMessage().equals("Cart items cannot be empty"));
        }
    }

    @Test
    void cartWiseApplyCouponById() {
        // Pre-create coupons
        service.createCoupon(buildCartCoupon(100, 10, true));
        service.createCoupon(buildCartCoupon(500, 20, true));


        // Create cart
        Cart cart = new Cart();
        cart.items = new ArrayList<CartItem>();
        CartItem ci1 = new CartItem();
        ci1.productId = 2;
        ci1.price = 50.00;
        ci1.quantity = 7;

        cart.items.add(ci1);

        CartItem ci2 = new CartItem();
        ci2.productId = 3;
        ci2.price = 25.00;
        ci2.quantity = 7;

        cart.items.add(ci2);

        CartItem ci3 = new CartItem();
        ci3.productId = 4;
        ci3.price = 25.00;
        ci3.quantity = 3;

        cart.items.add(ci3);

        ApplyCouponResponse response = service.applyCouponToCart(1l, cart);
        assertTrue(response.getTotalDiscount() == 60.00);

        response = service.applyCouponToCart(2, cart);
        assertTrue(response.getTotalDiscount() == 120.00);
    }
}
