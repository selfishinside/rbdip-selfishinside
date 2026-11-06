package com.rbdip.bookstore.order;

import com.rbdip.bookstore.customer.Customer;
import com.rbdip.bookstore.customer.CustomerRepository;
import org.springframework.stereotype.Component;

@Component
class CustomerResolver {

    private final CustomerRepository customerRepository;

    CustomerResolver(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    Customer resolve(CreateOrderRequest request) {
        return customerRepository
                .findFirstByFullNameAndAddressAndPhone(
                        request.customerFullName(), request.customerAddress(), request.customerPhone())
                .orElseGet(() -> customerRepository.save(new Customer(
                        request.customerFullName(), request.customerAddress(), request.customerPhone())));
    }
}
