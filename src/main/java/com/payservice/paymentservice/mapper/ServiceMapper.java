package com.payservice.paymentservice.mapper;

import com.payservice.paymentservice.dto.ServiceSummaryDTO;
import com.payservice.paymentservice.entity.ServiceEntity;
import org.springframework.stereotype.Component;

@Component
public class ServiceMapper {
    public ServiceSummaryDTO toService(ServiceEntity s) {
        if (s == null) return null;
        return new ServiceSummaryDTO(

                s.getServiceName(),
                s.getIsActive(),
                s.getDescription()
        );
    }
}
