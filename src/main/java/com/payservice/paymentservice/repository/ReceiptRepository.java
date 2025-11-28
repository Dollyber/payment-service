package com.payservice.paymentservice.repository;

import com.payservice.paymentservice.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ReceiptRepository extends JpaRepository<Receipt, Integer> {
    List<Receipt> findByServiceIdAndCustomerIdOrderByDueDateAsc(Integer serviceId, Integer customerId);
    List<Receipt> findByServiceIdAndCustomerIdAndDueDateBeforeOrderByDueDateAsc(Integer serviceId, Integer customerId, LocalDate dueDate);
}
