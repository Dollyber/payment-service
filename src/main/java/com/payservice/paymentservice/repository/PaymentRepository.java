package com.payservice.paymentservice.repository;

import com.payservice.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    List<Payment> findByCustomerIdOrderByPaymentDateDesc(Integer customerId);

}