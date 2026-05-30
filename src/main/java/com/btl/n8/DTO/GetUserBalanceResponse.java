package com.btl.n8.DTO;

import java.math.BigDecimal;

public class GetUserBalanceResponse extends Response {

    private boolean    success;
    private BigDecimal balance;
    private BigDecimal frozenBalance;

    public GetUserBalanceResponse() { super(); }

    public GetUserBalanceResponse(String sessionId, boolean success,
                                  BigDecimal balance, BigDecimal frozenBalance,
                                  String message) {
        super("USER_BALANCE_RESULT", message, sessionId);
        this.success       = success;
        this.balance       = balance;
        this.frozenBalance = frozenBalance;
    }

    public boolean    isSuccess()       { return success; }
    public BigDecimal getBalance()      { return balance; }
    public BigDecimal getFrozenBalance(){ return frozenBalance; }
}