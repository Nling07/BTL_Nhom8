package com.btl.n8.Service;

import com.btl.n8.Connection.BidDAO;
import com.btl.n8.Model.Entity.Bid;
import com.btl.n8.Model.Enums.BidStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class BidService {
    private final BidDAO bidDAO;

    // Lock per-auctionId để tránh race condition giữa nhiều ClientHandler instance
    private static final ConcurrentHashMap<Integer, ReentrantLock> auctionLocks =
            new ConcurrentHashMap<>();

    public BidService(BidDAO bidDAO) {
        this.bidDAO = bidDAO;
    }

    private ReentrantLock getLock(int auctionId) {
        return auctionLocks.computeIfAbsent(auctionId, id -> new ReentrantLock(true));
    }

    /**
     * Insert bid trực tiếp mà không kiểm tra lại amount (dùng bên trong transaction
     * ở RequestHandler, nơi validation đã được thực hiện trước khi bắt đầu tx).
     * Tránh gọi findHighestBid() trong transaction → không gây stale read dưới REPEATABLE READ.
     */
    public boolean insertBid(com.btl.n8.Model.Entity.Bid bid) {
        return bidDAO.insert(bid);
    }

    public boolean placeBid(int auctionId, int bidderId, BigDecimal amount) {
        ReentrantLock lock = getLock(auctionId);
        lock.lock();
        try {
            Bid highest = bidDAO.findHighestBid(auctionId);
            if (highest != null && amount.compareTo(highest.getAmount()) <= 0) {
                return false;
            }

            Bid newBid = new Bid();
            newBid.setAuctionId(auctionId);
            newBid.setBidderId(bidderId);
            newBid.setAmount(amount);
            newBid.setBidTime(LocalDateTime.now());
            newBid.setStatus(BidStatus.ACTIVE);

            bidDAO.updateOutbid(auctionId);
            return bidDAO.insert(newBid);
        } finally {
            lock.unlock();
        }
    }

    public List<Bid> getBidsByAuction(int auctionId) {
        return bidDAO.findByAuction(auctionId);
    }

    public List<Bid> getBidsByBidder(int bidderId) {
        return bidDAO.findByBidder(bidderId);
    }

    public Bid getHighestBid(int auctionId) {
        return bidDAO.findHighestBid(auctionId);
    }

    public boolean updateBidStatus(int bidId, BidStatus status) {
        return bidDAO.updateStatus(bidId, status);
    }

    public boolean markWinner(int auctionId) {
        Bid highest = bidDAO.findHighestBid(auctionId);
        if (highest != null) {
            return bidDAO.updateStatus(highest.getId(), BidStatus.WINNER);
        }
        return false;
    }

    /**
     * Mark tất cả bid ACTIVE của auction thành OUTBID (dùng khi settle).
     */
    public boolean markAllOutbid(int auctionId) {
        return bidDAO.updateOutbid(auctionId);
    }

    public boolean deleteBidById(int id) {
        return bidDAO.deleteById(id);
    }
}