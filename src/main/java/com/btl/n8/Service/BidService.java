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

    /**
     * Lock per-auctionId: đảm bảo chỉ 1 thread xử lý 1 auction tại 1 thời điểm.
     * Giải quyết vấn đề 2 người đấu giá đồng thời cùng 1 auction.
     */
    private static final ConcurrentHashMap<Integer, ReentrantLock> auctionLocks =
            new ConcurrentHashMap<>();

    public BidService(BidDAO bidDAO) {
        this.bidDAO = bidDAO;
    }

    private ReentrantLock getLock(int auctionId) {
        return auctionLocks.computeIfAbsent(auctionId, id -> new ReentrantLock(true));
    }

    // ── Insert bid (dùng bên trong transaction ở RequestHandler) ──────────────

    public boolean insertBid(Bid bid) {
        return bidDAO.insert(bid);
    }

    // ── placeBid (standalone, có lock) ────────────────────────────────────────

    public boolean placeBid(int auctionId, int bidderId, BigDecimal amount) {
        ReentrantLock lock = getLock(auctionId);
        lock.lock();
        try {
            Bid highest = bidDAO.findHighestBid(auctionId);
            if (highest != null && amount.compareTo(highest.getAmount()) <= 0) return false;

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

    // ── Queries ───────────────────────────────────────────────────────────────

    public List<Bid> getBidsByAuction(int auctionId)  { return bidDAO.findByAuction(auctionId); }
    public List<Bid> getBidsByBidder(int bidderId)    { return bidDAO.findByBidder(bidderId); }
    public Bid getHighestBid(int auctionId)            { return bidDAO.findHighestBid(auctionId); }

    /**
     * Lấy bid ACTIVE hiện tại của 1 bidder trong 1 auction.
     * Dùng để biết bidder đang giữ bao nhiêu frozen khi nâng giá chính mình.
     */
    public Bid getActiveBidByBidder(int auctionId, int bidderId) {
        return bidDAO.findActiveBidByBidder(auctionId, bidderId);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public boolean updateBidStatus(int bidId, BidStatus status) { return bidDAO.updateStatus(bidId, status); }
    public boolean markAllOutbid(int auctionId)                  { return bidDAO.updateOutbid(auctionId); }
    public boolean deleteBidById(int id)                         { return bidDAO.deleteById(id); }

    public boolean markWinner(int auctionId) {
        Bid highest = bidDAO.findHighestBid(auctionId);
        if (highest != null) return bidDAO.updateStatus(highest.getId(), BidStatus.WINNER);
        return false;
    }

    /**
     * Sau khi bid mới được insert và các bid cũ mark OUTBID:
     * Unfreeze balance cho tất cả bidder bị outbid (không phải currentBidderId).
     *
     * Logic: mỗi bidder chỉ được giữ 1 ACTIVE bid tại 1 thời điểm trong mỗi auction
     * → khi bị outbid, lấy bid OUTBID cao nhất (= bid trước đó của họ) để unfreeze đúng amount.
     */
    public void unfreezeOutbidBalances(int auctionId, int currentBidderId, UserService userService) {
        // Lấy tất cả bid OUTBID trong auction, nhóm theo bidder, unfreeze bid cao nhất của mỗi người
        List<Bid> allBids = bidDAO.findByAuction(auctionId);

        // Map bidderId → bid OUTBID cao nhất (= bid đang bị frozen gần nhất)
        java.util.Map<Integer, BigDecimal> outbidAmounts = new java.util.HashMap<>();
        java.util.Map<Integer, BigDecimal> activeFrozen  = new java.util.HashMap<>();

        for (Bid b : allBids) {
            if (b.getBidderId() == currentBidderId) continue;

            if (b.getStatus() == BidStatus.OUTBID) {
                // Giữ lại amount cao nhất của mỗi bidder (bid gần nhất bị outbid)
                outbidAmounts.merge(b.getBidderId(), b.getAmount(),
                        (old, nw) -> old.compareTo(nw) >= 0 ? old : nw);
            }
        }

        for (java.util.Map.Entry<Integer, BigDecimal> entry : outbidAmounts.entrySet()) {
            int bidderId = entry.getKey();
            BigDecimal amount = entry.getValue();
            boolean ok = userService.unfreezeBalance(bidderId, amount);
            if (ok) {
                System.out.println("[BidService] Unfreeze " + amount + " cho bidder " + bidderId);
            }
        }
    }
}