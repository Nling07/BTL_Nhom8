package com.btl.n8.Service;

import com.btl.n8.Connection.*;

import java.sql.Connection;

/**
 * Factory tập trung tạo Service.
 *
 * Giải quyết vấn đề Dependency Injection:
 * Trước đây mỗi class tự new Service(new DAOImpl(conn)) rải rác ở nhiều nơi
 * (RequestHandler, SettlementHandler, ...) → khó test, khó maintain.
 *
 * Bây giờ tất cả đều gọi ServiceFactory.create*(conn) từ 1 chỗ duy nhất.
 * Nếu sau này muốn đổi implementation (VD: dùng mock DAO để test), chỉ cần
 * sửa ở đây thay vì tìm từng chỗ new trong code.
 */
public class ServiceFactory {

    private ServiceFactory() {} // utility class, không cho new

    public static UserService               createUserService(Connection conn)               { return new UserService(new UserDAOImpl(conn)); }
    public static BidService                createBidService(Connection conn)                { return new BidService(new BidDAOImpl(conn)); }
    public static ItemService               createItemService(Connection conn)               { return new ItemService(new ItemDAOImpl(conn)); }
    public static AuctionService            createAuctionService(Connection conn)            { return new AuctionService(new AuctionDAOImpl(conn)); }
    public static AdminService              createAdminService(Connection conn)              { return new AdminService(new UserDAOImpl(conn), new AuctionDAOImpl(conn), new ItemDAOImpl(conn)); }

    /** Service xử lý logic cho màn hình danh sách đấu giá (BidController). */
    public static BidControllerService      createBidControllerService()                    { return new BidControllerService(); }

    /** Service xử lý logic cho màn hình chi tiết đấu giá (BidDetailController). */
    public static BidDetailControllerService createBidDetailControllerService()             { return new BidDetailControllerService(); }
}
