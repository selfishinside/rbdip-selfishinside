package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class OrderConfirmationSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderConfirmationSender.class);

    void send(String customerName, Long orderId, BigDecimal total) {
        LOGGER.info("Order confirmation sent to {} for order {} with total {}", customerName, orderId, total);
    }
}
