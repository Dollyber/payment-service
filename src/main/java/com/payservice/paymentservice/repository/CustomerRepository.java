package com.payservice.paymentservice.repository;

import com.payservice.paymentservice.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {}