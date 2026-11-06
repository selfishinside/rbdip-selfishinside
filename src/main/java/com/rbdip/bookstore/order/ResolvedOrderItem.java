package com.rbdip.bookstore.order;

import com.rbdip.bookstore.product.Product;

record ResolvedOrderItem(Product product, int quantity) {

    PricingCalculator.LineItem toLineItem() {
        return new PricingCalculator.LineItem(product.getPrice(), quantity);
    }

    OrderItem toOrderItem(Long orderId) {
        return new OrderItem(orderId, product.getName(), product.getPrice(), quantity);
    }
}
