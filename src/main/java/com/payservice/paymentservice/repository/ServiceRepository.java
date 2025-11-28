package com.payservice.paymentservice.repository;

import com.payservice.paymentservice.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Integer> {
    List<ServiceEntity> findByCustomerId(Integer customerId);
}
