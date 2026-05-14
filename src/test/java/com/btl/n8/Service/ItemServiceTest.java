package com.btl.n8.Service;

import com.btl.n8.Connection.ItemDAO;
import com.btl.n8.Model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemService Tests")
class ItemServiceTest {

    @Mock
    private ItemDAO itemDAO;

    private ItemService itemService;

    @BeforeEach
    void setUp() {
        itemService = new ItemService(itemDAO);
    }

    // ── addItem ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("addItem: item hợp lệ → insert được gọi, trả về true")
    void addItem_validItem_returnsTrue() {
        Item item = new Poster("Charizard", 1, null);
        when(itemDAO.insert(item)).thenReturn(true);

        assertTrue(itemService.addItem(item));
        verify(itemDAO).insert(item);
    }

    @Test
    @DisplayName("addItem: tên null → false, không gọi DB")
    void addItem_nullName_returnsFalse() {
        Item item = new Poster(null, 1, null);

        assertFalse(itemService.addItem(item));
        verifyNoInteractions(itemDAO);
    }

    @Test
    @DisplayName("addItem: tên rỗng → false, không gọi DB")
    void addItem_blankName_returnsFalse() {
        Item item = new Poster("   ", 1, null);

        assertFalse(itemService.addItem(item));
        verifyNoInteractions(itemDAO);
    }

    // ── getItemById ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getItemById: item tồn tại → trả về item đúng")
    void getItemById_exists_returnsItem() {
        Item item = new Card(5, "Pikachu", 2, null);
        when(itemDAO.findById(5)).thenReturn(item);

        Item result = itemService.getItemById(5);
        assertNotNull(result);
        assertEquals("Pikachu", result.getName());
    }

    @Test
    @DisplayName("getItemById: item không tồn tại → null")
    void getItemById_notFound_returnsNull() {
        when(itemDAO.findById(99)).thenReturn(null);

        assertNull(itemService.getItemById(99));
    }

    // ── getItemsByType ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getItemsByType: lọc đúng loại POSTER")
    void getItemsByType_poster_returnsOnlyPosters() {
        List<Item> all = List.of(
                new Poster(1, "PosterA", 1, null),
                new Figure(2, "FigureB", 1, null),
                new Card(3, "CardC",   1, null),
                new Poster(4, "PosterD", 2, null)
        );
        when(itemDAO.findAll()).thenReturn(all);

        List<Item> result = itemService.getItemsByType(ItemType.POSTER);

        assertEquals(2, result.size());
        result.forEach(i -> assertEquals(ItemType.POSTER, i.getType()));
    }

    @Test
    @DisplayName("getItemsByType: không có item nào khớp → list rỗng")
    void getItemsByType_noMatch_returnsEmpty() {
        List<Item> all = List.of(new Figure(1, "Fig", 1, null));
        when(itemDAO.findAll()).thenReturn(all);

        List<Item> result = itemService.getItemsByType(ItemType.CARD);
        assertTrue(result.isEmpty());
    }

    // ── getItemsBySeller ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getItemsBySeller: trả về đúng danh sách từ DAO")
    void getItemsBySeller_returnsList() {
        List<Item> sellerItems = List.of(
                new Poster(1, "P1", 3, null),
                new Card(2, "C1",   3, null)
        );
        when(itemDAO.findBySeller(3)).thenReturn(sellerItems);

        List<Item> result = itemService.getItemsBySeller(3);
        assertEquals(2, result.size());
    }

    // ── createItem ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createItem: type POSTER → trả về Poster")
    void createItem_poster_returnsPoster() {
        Item item = itemService.createItem("PosterX", "POSTER", 1, null);
        assertInstanceOf(Poster.class, item);
        assertEquals("PosterX", item.getName());
        assertEquals(ItemType.POSTER, item.getType());
    }

    @Test
    @DisplayName("createItem: type FIGURE → trả về Figure")
    void createItem_figure_returnsFigure() {
        Item item = itemService.createItem("FigureX", "FIGURE", 1, null);
        assertInstanceOf(Figure.class, item);
        assertEquals(ItemType.FIGURE, item.getType());
    }

    @Test
    @DisplayName("createItem: type CARD → trả về Card")
    void createItem_card_returnsCard() {
        Item item = itemService.createItem("CardX", "CARD", 2, null);
        assertInstanceOf(Card.class, item);
        assertEquals(ItemType.CARD, item.getType());
    }

    @Test
    @DisplayName("createItem: type không hợp lệ → ném IllegalArgumentException")
    void createItem_invalidType_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> itemService.createItem("X", "INVALID_TYPE", 1, null));
    }

    // ── deleteItemById ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteItemById: DAO trả về true → true")
    void deleteItemById_success_returnsTrue() {
        when(itemDAO.deleteById(1)).thenReturn(true);
        assertTrue(itemService.deleteItemById(1));
    }

    @Test
    @DisplayName("deleteItemById: item không tồn tại → false")
    void deleteItemById_notFound_returnsFalse() {
        when(itemDAO.deleteById(99)).thenReturn(false);
        assertFalse(itemService.deleteItemById(99));
    }
}