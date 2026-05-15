package com.btl.n8.Service;

import com.btl.n8.Connection.AuctionDAO;
import com.btl.n8.Connection.BidDAO;
import com.btl.n8.Model.entity.Auction;
import com.btl.n8.Model.enums.AuctionStatus;
import com.btl.n8.Model.entity.Bid;
import com.btl.n8.Model.enums.BidStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AutoBidService {
    private final AuctionDAO auctionDAO;
    private final BidDAO bidDAO;

    public AutoBidService(AuctionDAO auctionDAO, BidDAO bidDAO) {
        this.auctionDAO = auctionDAO;
        this.bidDAO = bidDAO;
    }

    public boolean autoBid(int auctionId, int bidderId, BigDecimal maxPrice, BigDecimal step) {
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null || auction.getStatus() != AuctionStatus.OPEN) return false;

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(auction.getStartTime()) || now.isAfter(auction.getEndTime())) return false;

        // Lấy bid cao nhất hiện tại
        Bid highestBid = bidDAO.findHighestBid(auctionId);
        BigDecimal currentPrice = (highestBid != null) ? highestBid.getAmount() : auction.getCurrentPrice();

        // Tính giá mới
        BigDecimal newPrice = currentPrice.add(step);
        if (newPrice.compareTo(maxPrice) > 0) return false;

        // Tạo bid mới
        Bid bid = new Bid();
        bid.setAuctionId(auctionId);
        bid.setBidderId(bidderId);
        bid.setAmount(newPrice);
        bid.setStatus(BidStatus.ACTIVE);

        boolean success = bidDAO.insert(bid);
        if (success) {
            // Cập nhật giá hiện tại của phiên đấu giá
            auctionDAO.updateCurrentPrice(auctionId, newPrice);
            // Đánh dấu các bid cũ là OUTBID
            bidDAO.updateOutbid(auctionId);
        }
        return success;
    }
}
