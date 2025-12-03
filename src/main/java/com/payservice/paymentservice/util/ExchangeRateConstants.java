package com.payservice.paymentservice.util;

import java.math.BigDecimal;

public class ExchangeRateConstants {
    private ExchangeRateConstants() {
        // Evita que esta clase sea instanciada
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final BigDecimal USD_RATE = new BigDecimal("3.50");
    public static final BigDecimal DEFAULT_RATE = BigDecimal.ONE;
}
