package com.btl.n8.Service;

import com.btl.n8.Connection.BidDAO;
import com.btl.n8.Model.Entity.Bid;
import com.btl.n8.Model.Enums.BidStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BidService Tests")
class BidServiceTest {

    @Mock
    private BidDAO bidDAO;

    private BidService bidService;

    @BeforeEach
    void setUp() {
        bidService = new BidService(bidDAO);
    }

    // ── placeBid ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("placeBid: không có bid nào trước → insert bid mới, trả về true")
    void placeBid_noExistingBid_insertsAndReturnsTrue() {
        when(bidDAO.findHighestBid(1)).thenReturn(null);
        when(bidDAO.insert(any())).thenReturn(true);

        assertTrue(bidService.placeBid(1, 10, new BigDecimal("500")));
        verify(bidDAO).updateOutbid(1);
        verify(bidDAO).insert(any(Bid.class));
    }

    @Test
    @DisplayName("placeBid: amount cao hơn highest bid → thành công")
    void placeBid_higherThanExisting_returnsTrue() {
        Bid highest = makeBid(1, 1, new BigDecimal("200"));
        when(bidDAO.findHighestBid(1)).thenReturn(highest);
        when(bidDAO.insert(any())).thenReturn(true);

        assertTrue(bidService.placeBid(1, 10, new BigDecimal("300")));
    }

    @Test
    @DisplayName("placeBid: amount bằng highest → false, không insert")
    void placeBid_equalToHighest_returnsFalse() {
        Bid highest = makeBid(1, 1, new BigDecimal("200"));
        when(bidDAO.findHighestBid(1)).thenReturn(highest);

        assertFalse(bidService.placeBid(1, 10, new BigDecimal("200")));
        verify(bidDAO, never()).insert(any());
    }

    @Test
    @DisplayName("placeBid: amount thấp hơn highest → false, không insert")
    void placeBid_lowerThanHighest_returnsFalse() {
        Bid highest = makeBid(1, 1, new BigDecimal("500"));
        when(bidDAO.findHighestBid(1)).thenReturn(highest);

        assertFalse(bidService.placeBid(1, 10, new BigDecimal("100")));
        verify(bidDAO, never()).insert(any());
    }

    @Test
    @DisplayName("placeBid: bid mới có đúng auctionId, bidderId, status ACTIVE")
    void placeBid_newBidHasCorrectFields() {
        when(bidDAO.findHighestBid(2)).thenReturn(null);
        when(bidDAO.insert(any())).thenReturn(true);

        bidService.placeBid(2, 7, new BigDecimal("999"));

        ArgumentCaptor<Bid> captor = ArgumentCaptor.forClass(Bid.class);
        verify(bidDAO).insert(captor.capture());
        Bid inserted = captor.getValue();

        assertEquals(2, inserted.getAuctionId());
        assertEquals(7, inserted.getBidderId());
        assertEquals(new BigDecimal("999"), inserted.getAmount());
        assertEquals(BidStatus.ACTIVE, inserted.getStatus());
        assertNotNull(inserted.getBidTime());
    }

    @Test
    @DisplayName("placeBid: gọi updateOutbid trước khi insert")
    void placeBid_callsUpdateOutbidBeforeInsert() {
        when(bidDAO.findHighestBid(1)).thenReturn(null);
        when(bidDAO.insert(any())).thenReturn(true);

        bidService.placeBid(1, 5, new BigDecimal("100"));

        // Verify thứ tự: outbid trước, insert sau
        var order = inOrder(bidDAO);
        order.verify(bidDAO).updateOutbid(1);
        order.verify(bidDAO).insert(any());
    }

    // ── markWinner ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("markWinner: có highest bid → cập nhật status WINNER")
    void markWinner_hasHighestBid_returnsTrue() {
        Bid highest = makeBid(5, 3, new BigDecimal("1000"));
        when(bidDAO.findHighestBid(1)).thenReturn(highest);
        when(bidDAO.updateStatus(5, BidStatus.WINNER)).thenReturn(true);

        assertTrue(bidService.markWinner(1));
        verify(bidDAO).updateStatus(5, BidStatus.WINNER);
    }

    @Test
    @DisplayName("markWinner: không có bid nào → false")
    void markWinner_noBids_returnsFalse() {
        when(bidDAO.findHighestBid(1)).thenReturn(null);

        assertFalse(bidService.markWinner(1));
        verify(bidDAO, never()).updateStatus(anyInt(), any());
    }

    // ── getBidsByAuction / getBidsByBidder ────────────────────────────────────────

    @Test
    @DisplayName("getBidsByAuction: trả về danh sách từ DAO")
    void getBidsByAuction_returnsList() {
        List<Bid> bids = List.of(makeBid(1, 1, BigDecimal.TEN));
        when(bidDAO.findByAuction(1)).thenReturn(bids);

        assertEquals(1, bidService.getBidsByAuction(1).size());
    }

    @Test
    @DisplayName("getBidsByBidder: trả về danh sách từ DAO")
    void getBidsByBidder_returnsList() {
        List<Bid> bids = List.of(makeBid(1, 3, BigDecimal.TEN), makeBid(2, 3, BigDecimal.ONE));
        when(bidDAO.findByBidder(3)).thenReturn(bids);

        assertEquals(2, bidService.getBidsByBidder(3).size());
    }

    // ── helper ────────────────────────────────────────────────────────────────────

    private Bid makeBid(int id, int bidderId, BigDecimal amount) {
        return new Bid(id, 1, bidderId, amount, LocalDateTime.now(), BidStatus.ACTIVE);
    }
}