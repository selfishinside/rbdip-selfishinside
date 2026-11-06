package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PricingCalculator {

    private static final int BULK_QUANTITY_THRESHOLD = 10;
    private static final int MONEY_SCALE = 2;
    private static final String VIP_CUSTOMER = "vip";
    private static final String WHOLESALE_CUSTOMER = "wholesale";
    private static final String FIXED_DISCOUNT_COUPON = "SAVE10";
    private static final String PERCENT_DISCOUNT_COUPON = "SAVE20PERCENT";
    private static final BigDecimal BULK_DISCOUNT_FACTOR = new BigDecimal("0.95");
    private static final BigDecimal VIP_DISCOUNT_FACTOR = new BigDecimal("0.9");
    private static final BigDecimal WHOLESALE_DISCOUNT_FACTOR = new BigDecimal("0.85");
    private static final BigDecimal COUPON_DISCOUNT_FACTOR = new BigDecimal("0.8");
    private static final BigDecimal LARGE_ORDER_THRESHOLD = new BigDecimal("1000");
    private static final BigDecimal LARGE_ORDER_DISCOUNT_FACTOR = new BigDecimal("0.98");

    public record LineItem(BigDecimal price, int quantity) {
    }

    public BigDecimal calculateOrderTotal(List<LineItem> items, String customerType, String couponCode) {
        BigDecimal total = items.stream()
                .map(this::calculateLinePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        total = total.multiply(customerDiscountFactor(customerType));
        total = applyCoupon(total, couponCode).max(BigDecimal.ZERO);
        total = applyLargeOrderDiscount(total);
        return total.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateLinePrice(LineItem item) {
        BigDecimal linePrice = item.price().multiply(BigDecimal.valueOf(item.quantity()));
        return item.quantity() > BULK_QUANTITY_THRESHOLD
                ? linePrice.multiply(BULK_DISCOUNT_FACTOR)
                : linePrice;
    }

    private BigDecimal customerDiscountFactor(String customerType) {
        if (VIP_CUSTOMER.equals(customerType)) {
            return VIP_DISCOUNT_FACTOR;
        }
        if (WHOLESALE_CUSTOMER.equals(customerType)) {
            return WHOLESALE_DISCOUNT_FACTOR;
        }
        return BigDecimal.ONE;
    }

    private BigDecimal applyCoupon(BigDecimal total, String couponCode) {
        if (FIXED_DISCOUNT_COUPON.equals(couponCode)) {
            return total.subtract(BigDecimal.TEN);
        }
        if (PERCENT_DISCOUNT_COUPON.equals(couponCode)) {
            return total.multiply(COUPON_DISCOUNT_FACTOR);
        }
        return total;
    }

    private BigDecimal applyLargeOrderDiscount(BigDecimal total) {
        return total.compareTo(LARGE_ORDER_THRESHOLD) > 0
                ? total.multiply(LARGE_ORDER_DISCOUNT_FACTOR)
                : total;
    }
}
