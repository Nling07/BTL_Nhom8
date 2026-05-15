package com.btl.n8.Service;

import com.btl.n8.Connection.UserDAO;
import com.btl.n8.Model.entity.Bidder;
import com.btl.n8.Model.entity.Seller;
import com.btl.n8.Model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserDAO userDAO;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userDAO);
    }

    // ── register ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register: account và password hợp lệ → true")
    void register_validInput_returnsTrue() {
        when(userDAO.findByAccount("alice")).thenReturn(null);
        when(userDAO.insert(any(User.class))).thenReturn(true);

        assertTrue(userService.register("alice", "123456"));
    }

    @Test
    @DisplayName("register: account đã tồn tại → false")
    void register_duplicateAccount_returnsFalse() {
        Bidder existing = new Bidder(1, "alice", "123456", BigDecimal.ZERO);
        when(userDAO.findByAccount("alice")).thenReturn(existing);

        assertFalse(userService.register("alice", "password"));
        // insert không được gọi khi account đã tồn tại
        verify(userDAO, never()).insert(any());
    }

    @Test
    @DisplayName("register: account null → false, không gọi DB")
    void register_nullAccount_returnsFalse() {
        assertFalse(userService.register(null, "123456"));
        verifyNoInteractions(userDAO);
    }

    @Test
    @DisplayName("register: account rỗng → false, không gọi DB")
    void register_blankAccount_returnsFalse() {
        assertFalse(userService.register("   ", "123456"));
        verifyNoInteractions(userDAO);
    }

    @Test
    @DisplayName("register: password null → false, không gọi DB")
    void register_nullPassword_returnsFalse() {
        assertFalse(userService.register("alice", null));
        verifyNoInteractions(userDAO);
    }

    @Test
    @DisplayName("register: password rỗng → false, không gọi DB")
    void register_blankPassword_returnsFalse() {
        assertFalse(userService.register("alice", ""));
        verifyNoInteractions(userDAO);
    }

    // ── login ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login: đúng account và password → trả về User")
    void login_correctCredentials_returnsUser() {
        Bidder bidder = new Bidder(1, "alice", "secret", BigDecimal.TEN);
        when(userDAO.findByAccount("alice")).thenReturn(bidder);

        User result = userService.login("alice", "secret");

        assertNotNull(result);
        assertEquals("alice", result.getAccount());
    }

    @Test
    @DisplayName("login: sai password → null")
    void login_wrongPassword_returnsNull() {
        Bidder bidder = new Bidder(1, "alice", "secret", BigDecimal.ZERO);
        when(userDAO.findByAccount("alice")).thenReturn(bidder);

        assertNull(userService.login("alice", "wrongpass"));
    }

    @Test
    @DisplayName("login: account không tồn tại → null")
    void login_unknownAccount_returnsNull() {
        when(userDAO.findByAccount(anyString())).thenReturn(null);

        assertNull(userService.login("ghost", "123"));
    }

    // ── upgradeToSeller ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("upgradeToSeller: Bidder hợp lệ → true")
    void upgradeToSeller_validBidder_returnsTrue() {
        Bidder bidder = new Bidder(1, "alice", "pass", BigDecimal.ZERO);
        when(userDAO.findById(1)).thenReturn(bidder);
        when(userDAO.upgradeToSeller(1)).thenReturn(true);

        assertTrue(userService.upgradeToSeller(1));
    }

    @Test
    @DisplayName("upgradeToSeller: user đã là Seller → false")
    void upgradeToSeller_alreadySeller_returnsFalse() {
        Seller seller = new Seller(2, "bob", "pass");
        when(userDAO.findById(2)).thenReturn(seller);

        assertFalse(userService.upgradeToSeller(2));
        verify(userDAO, never()).upgradeToSeller(anyInt());
    }

    @Test
    @DisplayName("upgradeToSeller: user không tồn tại → false")
    void upgradeToSeller_notFound_returnsFalse() {
        when(userDAO.findById(99)).thenReturn(null);

        assertFalse(userService.upgradeToSeller(99));
    }

    // ── phân quyền ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("canBid: Bidder → true")
    void canBid_bidder_returnsTrue() {
        assertTrue(userService.canBid(new Bidder(1, "a", "p", BigDecimal.ZERO)));
    }

    @Test
    @DisplayName("canBid: Seller → true (Seller cũng được bid)")
    void canBid_seller_returnsTrue() {
        assertTrue(userService.canBid(new Seller(2, "b", "p")));
    }

    @Test
    @DisplayName("canBid: null → false")
    void canBid_null_returnsFalse() {
        assertFalse(userService.canBid(null));
    }

    @Test
    @DisplayName("canSell: Seller → true")
    void canSell_seller_returnsTrue() {
        assertTrue(userService.canSell(new Seller(1, "b", "p")));
    }

    @Test
    @DisplayName("canSell: Bidder → false")
    void canSell_bidder_returnsFalse() {
        assertFalse(userService.canSell(new Bidder(1, "a", "p", BigDecimal.ZERO)));
    }
}