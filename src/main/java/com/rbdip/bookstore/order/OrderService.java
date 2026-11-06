package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final String DEFAULT_CUSTOMER_TYPE = "regular";
    private static final String INITIAL_ORDER_STATUS = "new";

    private final OrderRequestValidator requestValidator;
    private final OrderItemResolver itemResolver;
    private final PricingCalculator pricingCalculator;
    private final OrderWriter orderWriter;
    private final OrderConfirmationSender confirmationSender;

    public OrderService(
            OrderRequestValidator requestValidator,
            OrderItemResolver itemResolver,
            PricingCalculator pricingCalculator,
            OrderWriter orderWriter,
            OrderConfirmationSender confirmationSender) {
        this.requestValidator = requestValidator;
        this.itemResolver = itemResolver;
        this.pricingCalculator = pricingCalculator;
        this.orderWriter = orderWriter;
        this.confirmationSender = confirmationSender;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        requestValidator.validate(request);
        List<ResolvedOrderItem> items = itemResolver.resolve(request.items());
        BigDecimal total = pricingCalculator.calculateOrderTotal(
                items.stream().map(ResolvedOrderItem::toLineItem).toList(),
                resolveCustomerType(request.customerType()),
                request.couponCode());
        Order order = orderWriter.save(request, items, INITIAL_ORDER_STATUS);
        confirmationSender.send(request.customerFullName(), order.getId(), total);
        return order;
    }

    private String resolveCustomerType(String customerType) {
        return customerType == null ? DEFAULT_CUSTOMER_TYPE : customerType;
    }
}
