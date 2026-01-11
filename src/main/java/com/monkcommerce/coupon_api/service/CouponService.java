package com.monkcommerce.coupon_api.service;

import com.monkcommerce.coupon_api.coupon.CouponHandler;
import com.monkcommerce.coupon_api.exception.CouponException;
import com.monkcommerce.coupon_api.factory.CouponFactory;
import com.monkcommerce.coupon_api.model.Coupon;
import com.monkcommerce.coupon_api.model.CouponDetails;
import com.monkcommerce.coupon_api.model.CouponType;
import com.monkcommerce.coupon_api.model.cart.Cart;
import com.monkcommerce.coupon_api.model.cart.CartItem;
import com.monkcommerce.coupon_api.model.response.ApplicableCouponItem;
import com.monkcommerce.coupon_api.model.response.ApplicableCouponsResponse;
import com.monkcommerce.coupon_api.model.response.ApplyCouponResponse;
import com.monkcommerce.coupon_api.model.response.applyCouponResponse;
import com.monkcommerce.coupon_api.store.CouponIndexes;
import com.monkcommerce.coupon_api.util.CouponDetailsMerger;

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
            // Defensive checks
            .filter(c -> c != null && c.getDetails() != null && c.getType() != null)

            // ONLY isActive flag matters
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

        // System.out.println(" updatedCoupon "+updatedCoupon.getDetails().discount + " , "+updatedCoupon.getDetails().productId);

        // Merge existing coupon that are not changes
        updatedCoupon.setId(couponId);
        updatedCoupon.setDetails(merge(
            updatedCoupon.getDetails(),
            existing.getDetails()
        ));

        // System.out.println(" existing "+existing.getDetails().discount + " , "+existing.getDetails().productId);

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

    // Update Coupon
    /**
     * @param cart
     * @return
     * */
    @PostMapping
    public ApplicableCouponsResponse getApplicableCoupons(Cart cart) {

        if (cart == null || cart.items == null || cart.items.isEmpty()) {
            throw new CouponException("Cart items cannot be empty");
        }

        List<CartItem> cartItems = cart.items;
        double totalPrice = 0.0;

        ApplicableCouponsResponse response =
                new ApplicableCouponsResponse(new ArrayList<>());

        Map<Integer, Double> productCouponIndex = indexes.productIndex;
        Map<String, Coupon> couponMap = indexes.couponMap;

        /* ---------------- PRODUCT-WISE ---------------- */

        for (CartItem item : cartItems) {

            if (item == null) {
                throw new CouponException("Invalid cart item data");
            }

            totalPrice += item.price * item.quantity;

            if (productCouponIndex.containsKey(item.productId)) {
                double percent = productCouponIndex.get(item.productId);
                double discount = (item.price * item.quantity) * percent / 100;

                Coupon coupon = couponMap.get("" + item.productId);

                response.applicable_coupons.add(
                    new ApplicableCouponItem(
                        coupon.getId(),
                        "product-wise",
                        discount
                    )
                );
            }
        }

        /* ---------------- CART-WISE (BEST COUPON) ---------------- */

        TreeMap<Integer, Double> cartIndex = indexes.cartIndex;

        Map.Entry<Integer, Double> entry =
                cartIndex.floorEntry((int) totalPrice);

        if (entry != null) {
            double percent = entry.getValue();
            double discount = totalPrice * percent / 100;

            Coupon coupon = couponMap.get("" + entry.getKey());

            response.applicable_coupons.add(
                new ApplicableCouponItem(
                    coupon.getId(),
                    "cart-wise",
                    discount
                )
            );
        }

        /* ---------------- BXGY COUPONS ---------------- */
        for (String key : indexes.bxgyIndex) {

            Coupon coupon = couponMap.get(key);
            CouponDetails d = coupon.getDetails();
            if (!d.isActive) continue;
            applyBxGy(coupon, cart, response);
        }


        return response;
    }

    /**
     * BXGY IMPLEMENTATION
     */
    private void applyBxGy(
            Coupon coupon,
            Cart cart,
            ApplicableCouponsResponse response
    ) {
        CouponDetails d = coupon.getDetails();

        Set<Integer> buySet = new HashSet<>(d.buyProducts);
        Set<Integer> getSet = new HashSet<>(d.getProducts);

        // Count total BUY quantity (mixed allowed)
        int totalBuyQty = 0;
        for (CartItem item : cart.items) {
            if (buySet.contains(item.productId)) {
                totalBuyQty += item.quantity;
            }
        }

        int possibleSets = totalBuyQty / d.buyQuantity;
        int applicableSets =
                Math.min(possibleSets, d.repetitionLimit);

        if (applicableSets <= 0) return;

        int totalFreeItems =
                applicableSets * d.getQuantity;

        // Collect GET product prices
        List<Double> eligiblePrices = new ArrayList<>();

        for (CartItem item : cart.items) {
            if (getSet.contains(item.productId)) {
                for (int i = 0; i < item.quantity; i++) {
                    eligiblePrices.add(item.price);
                }
            }
        }

        if (eligiblePrices.isEmpty()) return;

        // 3️⃣ Maximize discount
        eligiblePrices.sort(Collections.reverseOrder());

        double discount = 0;
        for (int i = 0; i < Math.min(totalFreeItems, eligiblePrices.size()); i++) {
            discount += eligiblePrices.get(i);
        }

        if (discount > 0) {
            response.applicable_coupons.add(
                new ApplicableCouponItem(
                    coupon.getId(),
                    "bxgy",
                    discount
                )
            );
        }
    }

    @PostMapping
    public ApplyCouponResponse applyCouponToCart(long couponId, Cart cart) {

        if (cart == null || cart.items == null || cart.items.isEmpty()) {
            throw new CouponException("Cart items cannot be empty");
        }

        Coupon coupon = store.get(couponId);
        if (coupon == null || coupon.getDetails() == null || !coupon.getDetails().isActive()) {
            throw new CouponException("Coupon not found or inactive");
        }

        List<CartItem> cartItems = cart.items;
        double totalPrice = 0.0;
        double totalDiscount = 0.0;

        String ctype = coupon.getType().name();

        /* ---------------- CART WISE ---------------- */
        if (ctype.equals("CART_WISE")) {

            for (CartItem item : cartItems) {
                if (item == null) throw new CouponException("Invalid cart item data");

                totalPrice += item.price * item.quantity;
                item.totalDiscount = 0.0;
            }

            totalDiscount = (totalPrice * coupon.getDetails().discount) / 100.0;
            double finalPrice = totalPrice - totalDiscount;

            return new ApplyCouponResponse(
                    cartItems,
                    totalPrice,
                    totalDiscount,
                    finalPrice
            );
        }

        /* ---------------- PRODUCT WISE ---------------- */
        else if (ctype.equals("PRODUCT_WISE")) {

            for (CartItem item : cartItems) {
                if (item == null) throw new CouponException("Invalid cart item data");

                totalPrice += item.price * item.quantity;

                if (item.productId == coupon.getDetails().productId) {
                    item.totalDiscount =
                            (item.price * item.quantity * coupon.getDetails().discount) / 100.0;
                } else {
                    item.totalDiscount = 0.0;
                }
            }

            totalDiscount = cartItems.stream()
                    .mapToDouble(i -> i.totalDiscount)
                    .sum();

            double finalPrice = totalPrice - totalDiscount;

            return new ApplyCouponResponse(
                    cartItems,
                    totalPrice,
                    totalDiscount,
                    finalPrice
            );
        }

        /* ---------------- BXGY ---------------- */
        else if (ctype.equals("BXGY")) {

            CouponDetails d = coupon.getDetails();

            Set<Integer> buyIds = new HashSet<>(d.buyProducts);
            Set<Integer> getIds = new HashSet<>(d.getProducts);

            int totalBuyQty = 0;

            for (CartItem item : cartItems) {
                if (buyIds.contains(item.productId)) {
                    totalBuyQty += item.quantity;
                }
                totalPrice += item.price * item.quantity;
                item.totalDiscount = 0.0;
            }

            int possibleSets = totalBuyQty / d.buyQuantity;
            int applicableSets = Math.min(possibleSets, d.repetitionLimit);

            if (applicableSets > 0) {

                int totalFreeItems = applicableSets * d.getQuantity;

                List<CartItem> eligibleGetItems = cartItems.stream()
                        .filter(ci -> getIds.contains(ci.productId))
                        .collect(Collectors.toList());

                List<Double> prices = new ArrayList<>();
                Map<Double, CartItem> priceToItem = new HashMap<>();

                for (CartItem ci : eligibleGetItems) {
                    for (int i = 0; i < ci.quantity; i++) {
                        prices.add(ci.price);
                        priceToItem.put(ci.price, ci);
                    }
                }

                prices.sort(Collections.reverseOrder());

                int used = 0;
                for (Double price : prices) {
                    if (used >= totalFreeItems) break;
                    CartItem ci = priceToItem.get(price);
                    ci.totalDiscount += price;
                    totalDiscount += price;
                    used++;
                }
            }

            double finalPrice = totalPrice - totalDiscount;

            return new ApplyCouponResponse(
                    cartItems,
                    totalPrice,
                    totalDiscount,
                    finalPrice
            );
        }

        else {
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
