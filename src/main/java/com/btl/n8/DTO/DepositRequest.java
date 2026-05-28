package com.btl.n8.DTO;

import java.math.BigDecimal;

public class DepositRequest extends Request {
    private int userId;
    private BigDecimal amount;

    public DepositRequest() {}

    public DepositRequest(String sessionId, int userId, BigDecimal amount) {
        super("DEPOSIT", sessionId);
        this.userId = userId;
        this.amount = amount;
    }

    public int getUserId()        { return userId; }
    public BigDecimal getAmount() { return amount; }
}
