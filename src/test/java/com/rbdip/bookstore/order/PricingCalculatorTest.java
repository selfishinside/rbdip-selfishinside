package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PricingCalculatorTest {

    private final PricingCalculator calculator = new PricingCalculator();

    @Test
    void appliesBulkDiscountAboveTenItems() {
        assertTotal("104.50", item("10.00", 11), "regular", null);
    }

    @Test
    void doesNotApplyBulkDiscountToTenItems() {
        assertTotal("100.00", item("10.00", 10), "regular", null);
    }

    @Test
    void appliesVipDiscount() {
        assertTotal("90.00", item("100.00", 1), "vip", null);
    }

    @Test
    void appliesWholesaleDiscount() {
        assertTotal("85.00", item("100.00", 1), "wholesale", null);
    }

    @Test
    void appliesFixedCouponWithoutProducingNegativeTotal() {
        assertTotal("0.00", item("5.00", 1), "regular", "SAVE10");
    }

    @Test
    void appliesPercentageCoupon() {
        assertTotal("80.00", item("100.00", 1), "regular", "SAVE20PERCENT");
    }

    @Test
    void appliesLargeOrderDiscountAboveThreshold() {
        assertTotal("980.98", item("1001.00", 1), "regular", null);
    }

    @Test
    void doesNotApplyLargeOrderDiscountAtThreshold() {
        assertTotal("1000.00", item("1000.00", 1), "regular", null);
    }

    @Test
    void roundsTotalHalfUpToTwoDecimalPlaces() {
        assertTotal("0.01", item("0.005", 1), "regular", null);
    }

    @Test
    void returnsZeroForEmptyOrder() {
        BigDecimal total = calculator.calculateOrderTotal(List.of(), "regular", null);

        assertThat(total).isEqualByComparingTo("0.00");
    }

    private PricingCalculator.LineItem item(String price, int quantity) {
        return new PricingCalculator.LineItem(new BigDecimal(price), quantity);
    }

    private void assertTotal(
            String expected, PricingCalculator.LineItem item, String customerType, String couponCode) {
        BigDecimal total = calculator.calculateOrderTotal(List.of(item), customerType, couponCode);

        assertThat(total).isEqualByComparingTo(expected);
    }
}
