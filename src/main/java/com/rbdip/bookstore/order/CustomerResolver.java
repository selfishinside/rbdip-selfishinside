package com.rbdip.bookstore.order;

import com.rbdip.bookstore.customer.Customer;
import com.rbdip.bookstore.customer.CustomerName;
import com.rbdip.bookstore.customer.CustomerRepository;
import org.springframework.stereotype.Component;

@Component
class CustomerResolver {

    private final CustomerRepository customerRepository;

    CustomerResolver(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    Customer resolve(CreateOrderRequest request) {
        CustomerName name = CustomerName.from(request.customerFullName());
        return customerRepository
                .findFirstByFirstNameAndLastNameAndAddressAndPhone(
                        name.firstName(), name.lastName(), request.customerAddress(), request.customerPhone())
                .orElseGet(() -> customerRepository.save(new Customer(
                        name.firstName(),
                        name.lastName(),
                        request.customerAddress(),
                        request.customerPhone())));
    }
}
