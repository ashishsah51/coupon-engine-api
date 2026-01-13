package com.monkcommerce.coupon_api;

import com.monkcommerce.coupon_api.exception.CouponException;
import com.monkcommerce.coupon_api.model.Coupon;
import com.monkcommerce.coupon_api.model.CouponDetails;
import com.monkcommerce.coupon_api.model.CouponType;
import com.monkcommerce.coupon_api.model.cart.Cart;
import com.monkcommerce.coupon_api.model.cart.CartItem;
import com.monkcommerce.coupon_api.model.response.ApplicableCouponsResponse;
import com.monkcommerce.coupon_api.model.response.ApplyCouponResponse;
import com.monkcommerce.coupon_api.service.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BxGyCouponServiceTest {

    private CouponService service;

    @BeforeEach
    void setup() {
        service = new CouponService();
    }

    /**
     * Helper to build BXGY coupon
     */
    private Coupon buildBxGyCoupon(
            List<Integer> buyProducts,
            int buyQuantity,
            List<Integer> getProducts,
            int getQuantity,
            int repetitionLimit,
            Boolean isActive
    ) {
        CouponDetails details = new CouponDetails();
        details.buyProducts = buyProducts;
        details.buyQuantity = buyQuantity;
        details.getProducts = getProducts;
        details.getQuantity = getQuantity;
        details.repetitionLimit = repetitionLimit;
        details.isActive = isActive;

        Coupon coupon = new Coupon();
        coupon.setType(CouponType.BXGY);
        coupon.setDetails(details);
        return coupon;
    }

    @Test
    void bxGyCouponCreationFlow() {

        try {
            // Step 1: inactive coupon → allowed
            Coupon inactive = buildBxGyCoupon(Arrays.asList(1, 2), 2, Arrays.asList(3), 1, 2, false);
            Coupon c1 = service.createCoupon(inactive);
            assertFalse(c1.getDetails().isActive());
            assertEquals(2, c1.getDetails().buyQuantity);
            assertEquals(1, c1.getDetails().getQuantity);
        } catch (Exception e) {
            fail("Exception not expected in Step 1: " + e.getMessage());
        }

        try {
            // Step 2: same coupon active → allowed
            Coupon active = buildBxGyCoupon(Arrays.asList(1, 2), 2, Arrays.asList(3), 1, 2,true);
            Coupon c2 = service.createCoupon(active);
            assertTrue(c2.getDetails().isActive());
        } catch (Exception e) {
            fail("Exception not expected in Step 2: " + e.getMessage());
        }

        try {
            // Step 3: duplicate active → exception expected
            Coupon duplicate = buildBxGyCoupon(Arrays.asList(1, 2), 2, Arrays.asList(3), 1, 2, true);
            service.createCoupon(duplicate);
            fail("Expected CouponException for duplicate BXGY coupon");
        } catch (CouponException ex) {
            assertTrue(ex.getMessage().contains("BXGY coupon already exists"));
        }

        try {
            // Step 6: invalid buyQuantity → exception
            Coupon invalid3 = buildBxGyCoupon(Arrays.asList(1, 2), 0, Arrays.asList(3), 1, 2, true);
            service.createCoupon(invalid3);
            fail("Expected CouponException for invalid buyQuantity");
        } catch (CouponException ex) {
            assertEquals(
                "BXGY coupon must have a valid buyQuantity (> 0)",
                ex.getMessage()
            );
        }

        try {
            // Step 7: invalid getQuantity → exception
            Coupon invalid4 = buildBxGyCoupon(Arrays.asList(1, 2), 2, Arrays.asList(3), 0, 2, true);
            service.createCoupon(invalid4);
            fail("Expected CouponException for invalid getQuantity");
        } catch (CouponException ex) {
            assertTrue(
                ex.getMessage().contains("BXGY coupon must have a valid getQuantity (> 0)")
            );
        }
    }


    @Test
    void bxGyCouponGetFlow() {
        // Pre-create coupons
        service.createCoupon(buildBxGyCoupon(Arrays.asList(1, 2), 2, Arrays.asList(3), 1, 2, false));
        service.createCoupon(buildBxGyCoupon(Arrays.asList(1, 2), 2, Arrays.asList(3), 1, 2, true));
        service.createCoupon(buildBxGyCoupon(Arrays.asList(4, 5), 1, Arrays.asList(6), 1, 2, false));

        // Fetch active coupons
        List<Coupon> activeCoupons = service.getCoupon(true);
        assertEquals(1, activeCoupons.size());
        assertTrue(activeCoupons.get(0).getDetails().isActive());

        // Fetch inactive coupons
        List<Coupon> inactiveCoupons = service.getCoupon(false);
        assertEquals(2, inactiveCoupons.size());
    }

    @Test
    void bxGyCouponGetByIdFlow() {
        // Pre-create coupons
        service.createCoupon(buildBxGyCoupon(Arrays.asList(1, 2), 2, Arrays.asList(3), 1, 2, false));
        service.createCoupon(buildBxGyCoupon(Arrays.asList(1, 2), 2, Arrays.asList(3), 1, 2, true));

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
    void bxGyCouponDelete() {
        // Pre-create coupons
        service.createCoupon(buildBxGyCoupon(Arrays.asList(1, 2), 2, Arrays.asList(3), 1, 2, false));

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
    void bxgyApplyCoupon() {
        // Pre-create coupons
        service.createCoupon(buildBxGyCoupon(Arrays.asList(1, 2, 3), 3, Arrays.asList(3, 4), 2, 3, true));
        service.createCoupon(buildBxGyCoupon(Arrays.asList(1, 2, 3), 3, Arrays.asList(4, 5, 6), 2, 1, true));


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
    void bxGyApplyCouponById() {
        // Pre-create coupons
        service.createCoupon(buildBxGyCoupon(Arrays.asList(1, 2, 3), 3, Arrays.asList(3, 4), 2, 3, true));
        service.createCoupon(buildBxGyCoupon(Arrays.asList(1, 2, 3), 3, Arrays.asList(4, 5, 6), 2, 1, true));

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
        ci3.price = 15.00;
        ci3.quantity = 3;

        cart.items.add(ci3);

        ApplyCouponResponse response = service.applyCouponToCart(1l, cart);
        assertTrue(response.getTotalDiscount() == 130.00);

        response = service.applyCouponToCart(2, cart);
        assertTrue(response.getTotalDiscount() > 0.00);
    }
}
