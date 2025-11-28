package com.payservice.paymentservice.mapper;

import com.payservice.paymentservice.dto.CustomerSummaryDTO;
import com.payservice.paymentservice.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {
    public CustomerSummaryDTO toCustomer(Customer c) {
        if (c == null) return null;
        return new CustomerSummaryDTO(
                c.getNames(),
                c.getLastname(),
                c.getEmail()
        );
    }
}
