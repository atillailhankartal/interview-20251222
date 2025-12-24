package com.brokage.common.exception;

public class InsufficientBalanceException extends BusinessException {

    public InsufficientBalanceException(String asset, String required, String available) {
        super("INSUFFICIENT_BALANCE",
              String.format("Insufficient %s balance. Required: %s, Available: %s", asset, required, available));
    }
}
