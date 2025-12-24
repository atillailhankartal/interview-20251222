package com.brokage.common.enums;

public enum CustomerTier {
    STANDARD(3),
    PREMIUM(2),
    VIP(1);

    private final int priority;

    CustomerTier(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
