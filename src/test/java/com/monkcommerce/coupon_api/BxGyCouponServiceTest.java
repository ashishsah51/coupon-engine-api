package com.monkcommerce.coupon_api;

import com.monkcommerce.coupon_api.exception.CouponException;
import com.monkcommerce.coupon_api.model.Coupon;
import com.monkcommerce.coupon_api.model.CouponDetails;
import com.monkcommerce.coupon_api.model.CouponType;
import com.monkcommerce.coupon_api.service.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
            Boolean isActive
    ) {
        CouponDetails details = new CouponDetails();
        details.buyProducts = buyProducts;
        details.buyQuantity = buyQuantity;
        details.getProducts = getProducts;
        details.getQuantity = getQuantity;
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
            Coupon inactive = buildBxGyCoupon(Arrays.asList(1, 2), 2, Arrays.asList(3), 1, false);
            Coupon c1 = service.createCoupon(inactive);
            assertFalse(c1.getDetails().isActive());
            assertEquals(2, c1.getDetails().buyQuantity);
            assertEquals(1, c1.getDetails().getQuantity);
        } catch (Exception e) {
            fail("Exception not expected in Step 1: " + e.getMessage());
        }

        try {
            // Step 2: same coupon active → allowed
            Coupon active = buildBxGyCoupon(Arrays.asList(1, 2), 2, Arrays.asList(3), 1, true);
            Coupon c2 = service.createCoupon(active);
            assertTrue(c2.getDetails().isActive());
        } catch (Exception e) {
            fail("Exception not expected in Step 2: " + e.getMessage());
        }

        try {
            // Step 3: duplicate active → exception expected
            Coupon duplicate = buildBxGyCoupon(Arrays.asList(1, 2), 2, Arrays.asList(3), 1, true);
            service.createCoupon(duplicate);
            fail("Expected CouponException for duplicate BXGY coupon");
        } catch (CouponException ex) {
            assertTrue(ex.getMessage().contains("BXGY coupon already exists"));
        }

        try {
            // Step 6: invalid buyQuantity → exception
            Coupon invalid3 = buildBxGyCoupon(Arrays.asList(1, 2), 0, Arrays.asList(3), 1, true);
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
            Coupon invalid4 = buildBxGyCoupon(Arrays.asList(1, 2), 2, Arrays.asList(3), 0, true);
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
        service.createCoupon(buildBxGyCoupon(Arrays.asList(1, 2), 2, Arrays.asList(3), 1, false));
        service.createCoupon(buildBxGyCoupon(Arrays.asList(1, 2), 2, Arrays.asList(3), 1, true));
        service.createCoupon(buildBxGyCoupon(Arrays.asList(4, 5), 1, Arrays.asList(6), 1, false));

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
        service.createCoupon(buildBxGyCoupon(Arrays.asList(1, 2), 2, Arrays.asList(3), 1, false));
        service.createCoupon(buildBxGyCoupon(Arrays.asList(1, 2), 2, Arrays.asList(3), 1, true));

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
        service.createCoupon(buildBxGyCoupon(Arrays.asList(1, 2), 2, Arrays.asList(3), 1, false));

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
}
