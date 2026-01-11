package com.monkcommerce.coupon_api.model;

import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CouponDetails {

    /* -------- COMMON (OPTIONAL) -------- */
    public Boolean isActive;
    public LocalDate startDate;
    public LocalDate expiryDate;

    /* -------- CART-WISE -------- */
    public Integer threshold;
    public Double discount;

    /* -------- PRODUCT-WISE -------- */
    public Integer productId;

    /* -------- BXGY -------- */
    public List<Integer> buyProducts;
    public Integer buyQuantity;
    public List<Integer> getProducts;
    public Integer getQuantity;
    public Integer repetitionLimit;

    /* -------- DEFAULTING LOGIC -------- */
    public boolean isActive() {
        return isActive == null ? true : isActive;
    }

    public LocalDate getStartDate() {
        return startDate == null ? LocalDate.now() : startDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate == null
                ? LocalDate.now().plusYears(1)
                : expiryDate;
    }
}
