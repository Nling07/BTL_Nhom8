package com.btl.n8.DTO;

import com.btl.n8.Model.Entity.Auction;
import com.btl.n8.Model.Entity.Bid;

import java.util.List;

/**
 * Response trả về chi tiết một phiên đấu giá.
 *
 * Đã bổ sung:
 *   - itemName     : tên sản phẩm (lấy từ bảng items)
 *   - itemType     : loại sản phẩm (POSTER / FIGURE / CARD)
 *   - imageBase64  : ảnh sản phẩm encode Base64 (có thể null nếu không có ảnh)
 *
 * Server điền 3 field này trong RequestHandler.handleGetAuctionDetail().
 * Client (BidDetailController) đọc và hiển thị lên ImageView / Label.
 */
public class GetAuctionDetailResponse extends Response {

    private boolean      success;
    private Auction      auction;
    private List<Bid>    bidHistory;

    // ── Thông tin Item đi kèm (thêm mới để fix lỗi không tải được ảnh) ──────
    private String itemName;
    private String itemType;
    private String imageBase64;

    public GetAuctionDetailResponse() { super(); }

    public GetAuctionDetailResponse(String message, String sessionId,
                                    boolean success,
                                    Auction auction,
                                    List<Bid> bidHistory) {
        super("AUCTION_DETAIL_RESULT", message, sessionId);
        this.success    = success;
        this.auction    = auction;
        this.bidHistory = bidHistory;
    }

    // ── Fluent setter để gắn thêm thông tin Item ─────────────────────────────
    public GetAuctionDetailResponse withItem(String name, String type, String imageBase64) {
        this.itemName    = name;
        this.itemType    = type;
        this.imageBase64 = imageBase64;
        return this;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public boolean      isSuccess()        { return success; }
    public Auction      getAuction()       { return auction; }
    public List<Bid>    getBidHistory()    { return bidHistory; }
    public String       getItemName()      { return itemName; }
    public String       getItemType()      { return itemType; }
    public String       getImageBase64()   { return imageBase64; }
}
