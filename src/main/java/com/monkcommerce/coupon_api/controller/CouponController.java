package com.monkcommerce.coupon_api.controller;

import com.monkcommerce.coupon_api.exception.CouponException;
import com.monkcommerce.coupon_api.model.ApiResponse;
import com.monkcommerce.coupon_api.model.Coupon;
import com.monkcommerce.coupon_api.service.CouponService;
import org.springframework.web.bind.annotation.*;

import com.monkcommerce.coupon_api.model.cart.Cart;
import com.monkcommerce.coupon_api.model.response.ApplicableCouponsResponse;
import com.monkcommerce.coupon_api.model.response.ApplyCouponResponse;

import java.util.List;

@RestController
public class CouponController {

    private final CouponService service = new CouponService();

    /* ---------------- COUPON CRUD ---------------- */

    @PostMapping("/coupons")
    public ApiResponse<Coupon> create(@RequestBody Coupon coupon) {
        try {
            return new ApiResponse<>(service.createCoupon(coupon));
        } catch (CouponException ex) {
            return new ApiResponse<>(ex.getMessage());
        } catch (Exception ex) {
            return new ApiResponse<>("Internal server error: " + ex.getMessage());
        }
    }

    @GetMapping("/coupons")
    public ApiResponse<List<Coupon>> getCoupon(
            @RequestParam(value = "active", defaultValue = "true") boolean active) {
        try {
            return new ApiResponse<>(service.getCoupon(active));
        } catch (Exception ex) {
            return new ApiResponse<>("Internal server error: " + ex.getMessage());
        }
    }

    @GetMapping("/coupons/{id}")
    public ApiResponse<Coupon> getCouponById(@PathVariable long id) {
        try {
            return new ApiResponse<>(service.getCouponById(id));
        } catch (CouponException ex) {
            return new ApiResponse<>(ex.getMessage());
        } catch (Exception ex) {
            return new ApiResponse<>("Internal server error: " + ex.getMessage());
        }
    }

    @PutMapping("/coupons/{id}")
    public ApiResponse<Coupon> updateCouponById(
            @PathVariable long id,
            @RequestBody Coupon coupon) {
        try {
            return new ApiResponse<>(service.updateCouponById(id, coupon));
        } catch (CouponException ex) {
            return new ApiResponse<>(ex.getMessage());
        } catch (Exception ex) {
            return new ApiResponse<>("Internal server error: " + ex.getMessage());
        }
    }

    @DeleteMapping("/coupons/{id}")
    public ApiResponse<Coupon> deleteCoupon(@PathVariable long id) {
        try {
            return new ApiResponse<>(service.deleteCoupon(id));
        } catch (CouponException ex) {
            return new ApiResponse<>(ex.getMessage());
        } catch (Exception ex) {
            return new ApiResponse<>("Internal server error: " + ex.getMessage());
        }
    }

    /* ---------------- APPLICABLE COUPONS ---------------- */

    @PostMapping("/applicable-coupons")
    public ApiResponse<ApplicableCouponsResponse> getApplicableCoupons(
            @RequestBody Cart cart) {
        try {
            ApplicableCouponsResponse response =
                    service.getApplicableCoupons(cart);
            return new ApiResponse<>(response);
        } catch (CouponException ex) {
            return new ApiResponse<>(ex.getMessage());
        } catch (Exception ex) {
            return new ApiResponse<>("Internal server error: " + ex.getMessage());
        }
    }

    /**
     * POST /apply-coupon/{id}
     * Apply a specific coupon to the cart
     */
    @PostMapping("/apply-coupon/{id}")
    public ApiResponse<ApplyCouponResponse> applyCoupon(
            @PathVariable long id,
            @RequestBody Cart cart) {

        try {
            ApplyCouponResponse updatedCart = service.applyCouponToCart(id, cart);
            return new ApiResponse<>(updatedCart);
        } catch (CouponException ex) {
            return new ApiResponse<>(ex.getMessage());
        } catch (Exception ex) {
            return new ApiResponse<>("Internal server error: " + ex.getMessage());
        }
    }

}
