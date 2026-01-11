# 🎟️ Coupon Engine API

A Spring Boot–based backend service that allows creating, managing, and applying different types of discount coupons on a shopping cart. It supports **Cart-wise**, **Product-wise**, and **Buy X Get Y (BXGY)** coupons with strong validation rules to ensure fair and logical discounts.

The project uses **in-memory storage** (no database) and is easy to extend.

---

## 📋 Table of Contents

- [How to Run](#-how-to-run-the-project)
- [About the Project](#-about-the-project)
- [Create Coupon](#-create-coupon)
- [Coupon Validations](#-coupon-validations)
- [Get All Coupons](#-get-all-coupons)
- [Get Coupon by ID](#-get-coupon-by-id)
- [Update Coupon](#-update-coupon)
- [Delete Coupon](#-delete-coupon)
- [Cart JSON Input](#-cart-json-input)
- [Get Applicable Coupons](#-get-applicable-coupons-for-cart)
- [Apply Coupon](#-apply-specific-coupon-by-id)
- [Architecture](#-architecture-overview)
- [Project Structure](#-project-structure)

---

## 🚀 How to Run the Project

### Prerequisites

- Java 17+
- Maven
- Git

### Steps

```bash
# Clone repository
git clone https://github.com/<your-username>/coupon-engine-api.git

# Go to project folder
cd coupon-engine-api

# Build & run
mvn clean install
mvn spring-boot:run
```

**Server runs on:** `http://localhost:8080`

---

## 📖 About the Project

### Supported Coupon Types

| Type | Description |
|------|-------------|
| `CART_WISE` | Discount on total cart value when threshold is met |
| `PRODUCT_WISE` | Discount on a specific product |
| `BXGY` | Buy X items and get Y items free |

### Key Features

✅ Create, update, delete coupons  
✅ Fetch all coupons (active & inactive)  
✅ Find applicable coupons for a cart  
✅ Apply a specific coupon using ID  
✅ Strong validation to prevent invalid coupons  
✅ Clean, predictable API responses  

---

## ➕ Create Coupon

### Create CART_WISE Coupon

**Endpoint:** `POST /coupons`

**Request Body:**
```json
{
  "type": "CART_WISE",
  "details": {
    "threshold": 400,
    "discount": 10,
    "startDate": "2026-01-11",
    "expiryDate": "2027-01-11",
    "isActive": true
  }
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 6,
    "type": "CART_WISE",
    "details": {
      "active": true,
      "discount": 10.0,
      "expiryDate": "2027-01-11",
      "isActive": true,
      "startDate": "2026-01-11",
      "threshold": 400
    }
  },
  "timestamp": "2026-01-11T21:36:30.75528"
}
```

---

### Create PRODUCT_WISE Coupon

**Endpoint:** `POST /coupons`

**Request Body:**
```json
{
  "type": "PRODUCT_WISE",
  "details": {
    "productId": 101,
    "discount": 20,
    "startDate": "2026-01-11",
    "expiryDate": "2027-01-11",
    "isActive": true
  }
}
```

---

### Create BXGY Coupon

**Endpoint:** `POST /coupons`

**Request Body:**
```json
{
  "type": "BXGY",
  "details": {
    "buyProducts": [1, 2],
    "buyQuantity": 3,
    "getProducts": [3],
    "getQuantity": 1,
    "repetitionLimit": 3,
    "isActive": true
  }
}
```

**Explanation:**
- Buy any **3 items** from products `[1, 2]`
- Get **1 item** from products `[3]` **FREE**
- Can be applied up to **3 times** per cart

---

## ✅ Coupon Validations

### Common Rules

| Rule | Description |
|------|-------------|
| `discount` | Must be between 1–100 |
| `isActive` | Required field |
| Duplicates | Duplicate active coupons not allowed |

### CART_WISE Rules

| Rule | Description |
|------|-------------|
| `threshold` | Must be greater than 0 |
| Unique Threshold | Same threshold cannot exist twice as active |
| Monotonic Higher | Higher threshold cannot have lower discount |
| Monotonic Lower | Lower threshold cannot have higher discount |
| Inactive Bypass | Inactive coupons do not block new ones |

**Example of Invalid Scenarios:**
- Coupon with threshold `100` gives `15%` discount
- New coupon with threshold `200` tries to give `10%` discount → ❌ **Rejected** (higher threshold must give higher discount)

### PRODUCT_WISE Rules

| Rule | Description |
|------|-------------|
| `productId` | Must be non-null |
| `discount` | Must be greater than 0 |
| Unique Product | Only one active coupon per product |

### BXGY Rules

| Rule | Description |
|------|-------------|
| `buyProducts` | Must not be empty |
| `getProducts` | Must not be empty |
| `buyQuantity` | Must be greater than 0 |
| `getQuantity` | Must be greater than 0 |
| Unique Config | Duplicate active BXGY coupons not allowed |

---

## 📋 Get All Coupons

**Endpoint:** `GET /coupons`

**Query Parameters:**
| Parameter | Default | Description |
|-----------|---------|-------------|
| `active` | `true` | Filter by active/inactive status |

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "type": "CART_WISE",
      "details": {
        "active": true,
        "discount": 2.0,
        "expiryDate": "2027-01-11",
        "isActive": true,
        "startDate": "2026-01-11",
        "threshold": 100
      }
    },
    {
      "id": 2,
      "type": "BXGY",
      "details": {
        "active": true,
        "buyProducts": [1, 2],
        "buyQuantity": 3,
        "expiryDate": "2027-01-11",
        "getProducts": [3],
        "getQuantity": 1,
        "isActive": true,
        "repetitionLimit": 3,
        "startDate": "2026-01-11"
      }
    }
  ],
  "timestamp": "2026-01-11T21:53:57.406411"
}
```

---

## 🔍 Get Coupon by ID

**Endpoint:** `GET /coupons/{id}`

Returns coupon details if ID exists.

**Example:** `GET /coupons/1`

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "type": "CART_WISE",
    "details": {
      "active": true,
      "discount": 10.0,
      "threshold": 100,
      "isActive": true
    }
  },
  "timestamp": "2026-01-11T21:50:00.000000"
}
```

**Error Response (ID not found):**
```json
{
  "success": false,
  "error": "Coupon not available with id: 99",
  "timestamp": "2026-01-11T21:50:00.000000"
}
```

---

## ✏️ Update Coupon

**Endpoint:** `PUT /coupons/{id}`

### Rules

- Coupon must exist
- Coupon type cannot be changed
- Partial updates are supported (only send fields you want to change)

**Example:** `PUT /coupons/1`

**Request Body:**
```json
{
  "details": {
    "discount": 15
  }
}
```

---

## 🗑️ Delete Coupon

**Endpoint:** `DELETE /coupons/{id}`

**Example:** `DELETE /coupons/1`

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "type": "CART_WISE",
    "details": {
      "active": true,
      "discount": 2.0,
      "expiryDate": "2027-01-11",
      "isActive": true,
      "startDate": "2026-01-11",
      "threshold": 100
    }
  },
  "timestamp": "2026-01-11T21:54:48.574684"
}
```

---

## 🛒 Cart JSON Input

All cart-related endpoints accept this format:

```json
{
  "items": [
    { "productId": 1, "quantity": 3, "price": 50 },
    { "productId": 2, "quantity": 1, "price": 300 },
    { "productId": 3, "quantity": 3, "price": 25 },
    { "productId": 5, "quantity": 2, "price": 35 },
    { "productId": 4, "quantity": 2, "price": 10 }
  ]
}
```

| Field | Type | Description |
|-------|------|-------------|
| `productId` | int | Unique product identifier |
| `quantity` | int | Number of units |
| `price` | double | Price per unit |

**Cart Total:** `(3×50) + (1×300) + (3×25) + (2×35) + (2×10) = 615`

---

## 🎯 Get Applicable Coupons for Cart

**Endpoint:** `POST /applicable-coupons`

Returns all coupons that can be applied to the given cart with their calculated discount amounts.

**Request Body:**
```json
{
  "items": [
    { "productId": 1, "quantity": 3, "price": 50 },
    { "productId": 2, "quantity": 1, "price": 300 },
    { "productId": 3, "quantity": 3, "price": 25 },
    { "productId": 5, "quantity": 2, "price": 35 },
    { "productId": 4, "quantity": 2, "price": 10 }
  ]
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "applicable_coupons": [
      {
        "coupon_id": 1,
        "type": "cart-wise",
        "discount": 61.5
      },
      {
        "coupon_id": 2,
        "type": "bxgy",
        "discount": 25.0
      }
    ]
  },
  "timestamp": "2026-01-11T21:56:33.827767"
}
```

---

## 💳 Apply Specific Coupon by ID

**Endpoint:** `POST /apply-coupon/{couponId}`

Applies a specific coupon to the cart and returns the updated cart with discounts.

**Example:** `POST /apply-coupon/2`

**Request Body:**
```json
{
  "items": [
    { "productId": 1, "quantity": 3, "price": 50 },
    { "productId": 2, "quantity": 1, "price": 300 },
    { "productId": 3, "quantity": 3, "price": 25 },
    { "productId": 5, "quantity": 2, "price": 35 },
    { "productId": 4, "quantity": 2, "price": 10 }
  ]
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "items": [
      { "productId": 1, "quantity": 3, "price": 50.0, "totalDiscount": 0.0 },
      { "productId": 2, "quantity": 1, "price": 300.0, "totalDiscount": 0.0 },
      { "productId": 3, "quantity": 3, "price": 25.0, "totalDiscount": 25.0 },
      { "productId": 5, "quantity": 2, "price": 35.0, "totalDiscount": 0.0 },
      { "productId": 4, "quantity": 2, "price": 10.0, "totalDiscount": 0.0 }
    ],
    "totalPrice": 615.0,
    "totalDiscount": 25.0,
    "finalPrice": 590.0
  },
  "timestamp": "2026-01-11T21:58:35.748653"
}
```

### Response Fields

| Field | Description |
|-------|-------------|
| `items` | Cart items with individual discounts applied |
| `totalPrice` | Original cart total before discount |
| `totalDiscount` | Total discount amount |
| `finalPrice` | Final amount to pay (`totalPrice - totalDiscount`) |

---

## 🏗 Architecture Overview

The application follows a layered architecture with **Factory** and **Strategy** design patterns:

```
┌─────────────────────────────────────────────────────────────┐
│                     CouponController                        │
│                   (REST API Layer)                          │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                     CouponService                           │
│                  (Business Logic Layer)                     │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                    CouponFactory                            │
│              (Factory Pattern - Creates Handlers)           │
└───────┬─────────────┬───────────────────┬───────────────────┘
        │             │                   │
┌───────▼─────┐ ┌─────▼───────┐ ┌─────────▼─────────┐
│ CartWise    │ │ ProductWise │ │     BxGy          │
│ Coupon      │ │ Coupon      │ │     Coupon        │
│ Handler     │ │ Handler     │ │     Handler       │
└───────┬─────┘ └─────┬───────┘ └─────────┬─────────┘
        │             │                   │
┌───────▼─────────────▼───────────────────▼───────────────────┐
│                    CouponIndexes                            │
│                 (In-Memory Store)                           │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐            │
│  │  cartIndex  │ │productIndex │ │  bxgyIndex  │            │
│  │  (TreeMap)  │ │  (HashMap)  │ │  (HashSet)  │            │
│  └─────────────┘ └─────────────┘ └─────────────┘            │
└─────────────────────────────────────────────────────────────┘
```

### Key Components

| Component | Responsibility |
|-----------|----------------|
| `CouponController` | HTTP request handling, response formatting |
| `CouponService` | Core business logic, coupon application algorithms |
| `CouponFactory` | Creates appropriate handler based on coupon type |
| `CouponHandler` | Interface for coupon validation and indexing |
| `CouponIndexes` | In-memory storage with optimized data structures |

### Data Structures

| Index | Type | Purpose |
|-------|------|---------|
| `cartIndex` | `TreeMap<Integer, Double>` | O(log n) threshold lookups |
| `productIndex` | `HashMap<Integer, Double>` | O(1) product discount lookups |
| `bxgyIndex` | `HashSet<String>` | O(1) BXGY uniqueness checks |

---

## 📁 Project Structure

```
coupon-engine-api/
├── src/
│   ├── main/
│   │   ├── java/com/monkcommerce/coupon_api/
│   │   │   ├── controller/
│   │   │   │   └── CouponController.java      # REST endpoints
│   │   │   ├── coupon/
│   │   │   │   ├── CouponHandler.java         # Handler interface
│   │   │   │   ├── CartWiseCoupon.java        # Cart-wise implementation
│   │   │   │   ├── ProductWiseCoupon.java     # Product-wise implementation
│   │   │   │   └── BxGyCoupon.java            # BxGy implementation
│   │   │   ├── exception/
│   │   │   │   └── CouponException.java       # Custom exception
│   │   │   ├── factory/
│   │   │   │   └── CouponFactory.java         # Factory for handlers
│   │   │   ├── model/
│   │   │   │   ├── ApiResponse.java           # Unified API response
│   │   │   │   ├── Coupon.java                # Coupon entity
│   │   │   │   ├── CouponDetails.java         # Coupon configuration
│   │   │   │   ├── CouponType.java            # Enum for coupon types
│   │   │   │   ├── cart/
│   │   │   │   │   ├── Cart.java              # Cart model
│   │   │   │   │   └── CartItem.java          # Cart item model
│   │   │   │   └── response/
│   │   │   │       ├── ApplicableCouponItem.java
│   │   │   │       ├── ApplicableCouponsResponse.java
│   │   │   │       └── ApplyCouponResponse.java
│   │   │   ├── service/
│   │   │   │   └── CouponService.java         # Business logic
│   │   │   ├── store/
│   │   │   │   └── CouponIndexes.java         # In-memory indexes
│   │   │   ├── util/
│   │   │   │   └── CouponDetailsMerger.java   # Utility for merging
│   │   │   └── CouponApiApplication.java      # Main application
│   │   └── resources/
│   │       └── application.properties         # Configuration
│   └── test/
│       └── java/com/monkcommerce/coupon_api/
│           ├── CartWiseCouponServiceTest.java
│           ├── ProductWiseCouponServiceTest.java
│           └── BxGyCouponServiceTest.java
├── pom.xml                                    # Maven configuration
├── mvnw                                       # Maven wrapper (Unix)
├── mvnw.cmd                                   # Maven wrapper (Windows)
└── README.md                                  # This file
```

---

## 🧪 Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CartWiseCouponServiceTest
mvn test -Dtest=ProductWiseCouponServiceTest
mvn test -Dtest=BxGyCouponServiceTest
```

---

## 🛠 Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Core programming language |
| Spring Boot | 4.0.1 | Application framework |
| Spring Web MVC | - | REST API development |
| H2 Database | - | In-memory database (optional) |
| Lombok | - | Boilerplate code reduction |
| Maven | - | Dependency management & build |
| JUnit 5 | - | Unit testing |

---

## 📊 API Summary

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/coupons` | Create a new coupon |
| `GET` | `/coupons` | Get all coupons |
| `GET` | `/coupons/{id}` | Get coupon by ID |
| `PUT` | `/coupons/{id}` | Update coupon |
| `DELETE` | `/coupons/{id}` | Delete coupon |
| `POST` | `/applicable-coupons` | Get applicable coupons for cart |
| `POST` | `/apply-coupon/{id}` | Apply coupon to cart |

---

## ✅ Final Summary

| Feature | Status |
|---------|--------|
| Cart-wise coupons | ✅ Supported |
| Product-wise coupons | ✅ Supported |
| BXGY coupons | ✅ Supported |
| Invalid discount prevention | ✅ Implemented |
| Clean API responses | ✅ Implemented |
| In-memory storage | ✅ No database required |

**Ideal for:** E-commerce coupon systems, promotional engines, discount management platforms

---

## 🔮 Future Enhancements

- [ ] Database persistence (PostgreSQL/MySQL)
- [ ] Coupon stacking (multiple coupons at once)
- [ ] Usage limits per user
- [ ] Scheduled activation/deactivation
- [ ] Human-readable coupon codes
- [ ] Analytics and redemption tracking

---

## 📄 License

This project is licensed under the MIT License.

---

<p align="center">
  Built with ❤️ for e-commerce
</p>
