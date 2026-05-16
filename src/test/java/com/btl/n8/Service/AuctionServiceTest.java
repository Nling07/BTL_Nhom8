package com.btl.n8.Service;

import com.btl.n8.Connection.AuctionDAO;
import com.btl.n8.Model.Entity.Auction;
import com.btl.n8.Model.Enums.AuctionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuctionService Tests")
class AuctionServiceTest {

    @Mock
    private AuctionDAO auctionDAO;

    private AuctionService auctionService;

    // Auction mẫu: OPEN, đang trong thời gian hợp lệ, giá hiện tại 100
    private Auction openAuction() {
        return new Auction(
                1, 10,
                new BigDecimal("100"),
                new BigDecimal("100"),
                LocalDateTime.now().minusHours(1),   // đã bắt đầu
                LocalDateTime.now().plusHours(23),    // chưa kết thúc
                AuctionStatus.OPEN
        );
    }

    @BeforeEach
    void setUp() {
        auctionService = new AuctionService(auctionDAO);
    }

    // ── createAuction ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("createAuction: thời gian hợp lệ → insert được gọi, trả về true")
    void createAuction_validTime_callsInsert() {
        Auction auction = new Auction();
        auction.setStartTime(LocalDateTime.now());
        auction.setEndTime(LocalDateTime.now().plusDays(2));
        when(auctionDAO.insert(any())).thenReturn(true);

        assertTrue(auctionService.createAuction(auction));
        // Service phải tự set status = OPEN trước khi insert
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
        verify(auctionDAO).insert(auction);
    }

    @Test
    @DisplayName("createAuction: startTime sau endTime → false, không gọi DB")
    void createAuction_startAfterEnd_returnsFalse() {
        Auction auction = new Auction();
        auction.setStartTime(LocalDateTime.now().plusDays(3));
        auction.setEndTime(LocalDateTime.now());

        assertFalse(auctionService.createAuction(auction));
        verifyNoInteractions(auctionDAO);
    }

    // ── placeBid ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("placeBid: giá hợp lệ cao hơn current → true")
    void placeBid_validHigherPrice_returnsTrue() {
        when(auctionDAO.findById(1)).thenReturn(openAuction());
        when(auctionDAO.updateCurrentPrice(1, new BigDecimal("200"))).thenReturn(true);

        assertTrue(auctionService.placeBid(1, new BigDecimal("200")));
        verify(auctionDAO).updateCurrentPrice(1, new BigDecimal("200"));
    }

    @Test
    @DisplayName("placeBid: giá bằng current → false")
    void placeBid_equalPrice_returnsFalse() {
        when(auctionDAO.findById(1)).thenReturn(openAuction());

        assertFalse(auctionService.placeBid(1, new BigDecimal("100")));
        verify(auctionDAO, never()).updateCurrentPrice(anyInt(), any());
    }

    @Test
    @DisplayName("placeBid: giá thấp hơn current → false")
    void placeBid_lowerPrice_returnsFalse() {
        when(auctionDAO.findById(1)).thenReturn(openAuction());

        assertFalse(auctionService.placeBid(1, new BigDecimal("50")));
        verify(auctionDAO, never()).updateCurrentPrice(anyInt(), any());
    }

    @Test
    @DisplayName("placeBid: auction không tồn tại → false")
    void placeBid_auctionNotFound_returnsFalse() {
        when(auctionDAO.findById(99)).thenReturn(null);

        assertFalse(auctionService.placeBid(99, new BigDecimal("500")));
    }

    @Test
    @DisplayName("placeBid: auction đã CLOSED → false")
    void placeBid_closedAuction_returnsFalse() {
        Auction closed = openAuction();
        closed.setStatus(AuctionStatus.CLOSED);
        when(auctionDAO.findById(1)).thenReturn(closed);

        assertFalse(auctionService.placeBid(1, new BigDecimal("999")));
    }

    @Test
    @DisplayName("placeBid: auction đã CANCELLED → false")
    void placeBid_cancelledAuction_returnsFalse() {
        Auction cancelled = openAuction();
        cancelled.setStatus(AuctionStatus.CANCELLED);
        when(auctionDAO.findById(1)).thenReturn(cancelled);

        assertFalse(auctionService.placeBid(1, new BigDecimal("999")));
    }

    @Test
    @DisplayName("placeBid: auction chưa bắt đầu → false")
    void placeBid_notStartedYet_returnsFalse() {
        Auction future = new Auction(
                1, 10,
                new BigDecimal("100"), new BigDecimal("100"),
                LocalDateTime.now().plusHours(1),   // chưa bắt đầu
                LocalDateTime.now().plusDays(2),
                AuctionStatus.OPEN
        );
        when(auctionDAO.findById(1)).thenReturn(future);

        assertFalse(auctionService.placeBid(1, new BigDecimal("200")));
    }

    @Test
    @DisplayName("placeBid: auction đã hết giờ → false")
    void placeBid_expiredAuction_returnsFalse() {
        Auction expired = new Auction(
                1, 10,
                new BigDecimal("100"), new BigDecimal("100"),
                LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusHours(1),  // đã hết giờ
                AuctionStatus.OPEN
        );
        when(auctionDAO.findById(1)).thenReturn(expired);

        assertFalse(auctionService.placeBid(1, new BigDecimal("200")));
    }

    // ── closeAuction ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("closeAuction: auction tồn tại → status CLOSED, update được gọi")
    void closeAuction_exists_setsClosedAndUpdates() {
        Auction auction = openAuction();
        when(auctionDAO.findById(1)).thenReturn(auction);
        when(auctionDAO.update(auction)).thenReturn(true);

        assertTrue(auctionService.closeAuction(1));
        assertEquals(AuctionStatus.CLOSED, auction.getStatus());
        verify(auctionDAO).update(auction);
    }

    @Test
    @DisplayName("closeAuction: auction không tồn tại → false")
    void closeAuction_notFound_returnsFalse() {
        when(auctionDAO.findById(99)).thenReturn(null);

        assertFalse(auctionService.closeAuction(99));
        verify(auctionDAO, never()).update(any());
    }

    // ── cancelAuction ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelAuction: auction tồn tại → status CANCELLED")
    void cancelAuction_exists_setsCancelled() {
        Auction auction = openAuction();
        when(auctionDAO.findById(1)).thenReturn(auction);
        when(auctionDAO.update(auction)).thenReturn(true);

        assertTrue(auctionService.cancelAuction(1));
        assertEquals(AuctionStatus.CANCELLED, auction.getStatus());
    }

    @Test
    @DisplayName("cancelAuction: auction không tồn tại → false")
    void cancelAuction_notFound_returnsFalse() {
        when(auctionDAO.findById(99)).thenReturn(null);

        assertFalse(auctionService.cancelAuction(99));
    }
}