package com.btl.n8.DTO;

import java.math.BigDecimal;

public class AuctionSettledResponse extends Response {

    private int        auctionId;
    private int        winnerId;       // -1 nếu không có ai đặt giá
    private String     winnerAccount;
    private BigDecimal winningPrice;
    private boolean    isWinner;       // true nếu receiver chính là winner

    public AuctionSettledResponse() { super(); }

    public AuctionSettledResponse(String sessionId, int auctionId,
                                  int winnerId, String winnerAccount,
                                  BigDecimal winningPrice) {
        super("AUCTION_SETTLED",
                winnerId == -1 ? "Phiên đấu giá kết thúc — không có người thắng"
                        : "Phiên đấu giá kết thúc. Người thắng: " + winnerAccount,
                sessionId);
        this.auctionId    = auctionId;
        this.winnerId     = winnerId;
        this.winnerAccount = winnerAccount;
        this.winningPrice  = winningPrice;
        this.isWinner      = false; // server set per-client trước khi gửi
    }

    public int        getAuctionId()    { return auctionId; }
    public int        getWinnerId()     { return winnerId; }
    public String     getWinnerAccount(){ return winnerAccount; }
    public BigDecimal getWinningPrice() { return winningPrice; }
    public boolean    isWinner()        { return isWinner; }
    public void       setIsWinner(boolean w) { this.isWinner = w; }
}