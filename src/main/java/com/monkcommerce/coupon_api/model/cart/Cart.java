package com.monkcommerce.coupon_api.model.cart;

import java.util.List;
import lombok.Data; // Requires Lombok dependency
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data // Generates getters, setters, toString, etc. automatically
@AllArgsConstructor
@NoArgsConstructor
public class Cart {
    public List<CartItem> items;
}