package com.btl.n8.DTO;

import java.math.BigDecimal;

public class DepositResponse extends Response {
    private boolean success;
    private BigDecimal newBalance;

    public DepositResponse() { super(); }

    public DepositResponse(String message, String sessionId,
                           boolean success, BigDecimal newBalance) {
        super("DEPOSIT_RESULT", message, sessionId);
        this.success    = success;
        this.newBalance = newBalance;
    }

    public boolean isSuccess()        { return success; }
    public BigDecimal getNewBalance() { return newBalance; }
}
