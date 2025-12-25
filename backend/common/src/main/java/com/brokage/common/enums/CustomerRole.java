package com.brokage.common.enums;

/**
 * Customer roles - determines what actions a customer can perform
 */
public enum CustomerRole {
    /**
     * Regular customer - can create orders for themselves
     */
    CUSTOMER,

    /**
     * Broker - manages other customers, cannot have orders created for them
     */
    BROKER,

    /**
     * Admin - system administrator, cannot have orders created for them
     */
    ADMIN
}
