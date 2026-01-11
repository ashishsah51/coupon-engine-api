package com.monkcommerce.coupon_api;

import com.monkcommerce.coupon_api.exception.CouponException;
import com.monkcommerce.coupon_api.model.Coupon;
import com.monkcommerce.coupon_api.model.CouponDetails;
import com.monkcommerce.coupon_api.model.CouponType;
import com.monkcommerce.coupon_api.service.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProductWiseCouponServiceTest {

    private CouponService service;

    @BeforeEach
    void setup() {
        service = new CouponService();
    }

    private Coupon buildProductCoupon(int productId, double discount, Boolean isActive) {
        CouponDetails details = new CouponDetails();
        details.productId = productId;
        details.discount = discount;
        details.isActive = isActive;

        Coupon coupon = new Coupon();
        coupon.setType(CouponType.PRODUCT_WISE);
        coupon.setDetails(details);
        return coupon;
    }

    @Test
    void productCouponCreationFlow() {
        // Step 1: inactive coupon
        Coupon inactive = buildProductCoupon(101, 20, false);
        Coupon c1 = service.createCoupon(inactive);
        assertEquals(101, c1.getDetails().productId);
        assertFalse(c1.getDetails().isActive());

        // Step 2: active coupon → allowed
        Coupon active = buildProductCoupon(101, 20, true);
        Coupon c2 = service.createCoupon(active);
        assertEquals(101, c2.getDetails().productId);
        assertTrue(c2.getDetails().isActive());

        // Step 3: duplicate active → exception
        Coupon duplicate = buildProductCoupon(101, 25, true);
        CouponException ex = assertThrows(CouponException.class,
                () -> service.createCoupon(duplicate));
        assertTrue(ex.getMessage().contains("Active product-wise coupon already exists for productId"));

        // Step 4: another inactive coupon → allowed
        Coupon inactive2 = buildProductCoupon(102, 15, false);
        Coupon c4 = service.createCoupon(inactive2);
        assertEquals(102, c4.getDetails().productId);
        assertFalse(c4.getDetails().isActive());

        // Step 5: Create coupon with productId null
        Coupon inactive3 = buildProductCoupon(103, 0, false);
        ex = assertThrows(CouponException.class,
                () -> service.createCoupon(inactive3));
        assertTrue(ex.getMessage().contains("Product-wise coupon must have a valid discount"));
    }

    @Test
    void productCouponGetFlow() {
        // Pre-create some coupons
        service.createCoupon(buildProductCoupon(101, 20, false));
        service.createCoupon(buildProductCoupon(101, 20, true));
        service.createCoupon(buildProductCoupon(102, 15, false));

        // Fetch active coupons
        List<Coupon> activeCoupons = service.getCoupon(true);
        assertEquals(1, activeCoupons.size());
        assertEquals(101, activeCoupons.get(0).getDetails().productId);
        assertTrue(activeCoupons.get(0).getDetails().isActive());

        // Fetch inactive coupons
        List<Coupon> inactiveCoupons = service.getCoupon(false);
        assertEquals(2, inactiveCoupons.size());
        assertTrue(inactiveCoupons.stream().anyMatch(c -> c.getDetails().productId == 101));
        assertTrue(inactiveCoupons.stream().anyMatch(c -> c.getDetails().productId == 102));
    }

    @Test
    void couponGetByIdFlow() {
        // Pre-create coupons
        service.createCoupon(buildProductCoupon(101, 20, false));
        service.createCoupon(buildProductCoupon(101, 20, true));

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
        service.createCoupon(buildProductCoupon(101, 20, false));

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
