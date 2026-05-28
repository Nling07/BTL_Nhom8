# 🎮 Pokemon Auction System — BTL Nhóm 8

Hệ thống đấu giá trực tuyến theo chủ đề Pokémon, xây dựng theo mô hình Client–Server với giao tiếp qua TCP Socket. Người dùng có thể đăng ký, đăng nhập, đăng vật phẩm đấu giá, đặt giá thủ công hoặc tự động (Auto-Bid), và xem kết quả thanh toán khi phiên đấu giá kết thúc.

---

## 📌 Mô tả bài toán & phạm vi hệ thống

| Tính năng | Mô tả |
|---|---|
| Đăng ký / Đăng nhập | Xác thực người dùng, phân quyền Seller / Bidder / Admin |
| Đăng vật phẩm | Seller tạo phiên đấu giá với giá khởi điểm và thời gian kết thúc |
| Đặt giá (Bid) | Bidder đặt giá thủ công, server kiểm tra tính hợp lệ |
| Auto-Bid | Bidder đặt giá trần, hệ thống tự động đặt giá tăng dần |
| Kết thúc phiên | Server tự động settle auction khi hết giờ (mỗi 10 giây), trừ số dư người thắng |
| Quản trị | Admin duyệt / từ chối vật phẩm, quản lý người dùng |
| Giao tiếp | Client ↔ Server qua TCP Socket, truyền JSON (Gson) |
| CSDL | MySQL cloud (Railway), dùng ThreadLocal Connection cho thread-safety |

---

## 🛠️ Công nghệ & Môi trường

| Thành phần | Phiên bản |
|---|---|
| Java | 21 (LTS) |
| JavaFX | 23.0.1 |
| Maven | 3.x (có kèm Maven Wrapper `mvnw`) |
| Gson | 2.11.0 |
| MySQL Connector/J | 9.0.0 |
| JUnit 5 | 5.9.3 |
| Mockito | 5.3.1 |
| Database | MySQL — Railway Cloud |

**Yêu cầu cài đặt tối thiểu:**
- JDK 21 trở lên ([tải tại đây](https://adoptium.net))
- Không cần cài thêm JavaFX riêng — đã được đóng gói vào JAR
- Không cần cài MySQL cục bộ — kết nối thẳng đến DB cloud

> ⚠️ **Lưu ý:** `client.jar` chứa JavaFX native libraries. Khi chạy lệnh `java -jar client.jar`, Java cần đọc được các native lib từ JAR. Nếu gặp lỗi JavaFX không tìm thấy, xem mục **Troubleshooting** bên dưới.

---

## 📂 Cấu trúc thư mục

```
BTL_Nhom8/
├── src/
│   ├── main/
│   │   ├── java/com/btl/n8/
│   │   │   ├── Main.java                  # Entry point (JavaFX Application)
│   │   │   ├── Launcher.java              # Wrapper để fat JAR bypass JavaFX module check
│   │   │   ├── Connection/                # DAO interfaces & implementations, DataConnection
│   │   │   │   ├── DataConnection.java    # ThreadLocal MySQL connection pool
│   │   │   │   ├── UserDAOImpl.java
│   │   │   │   ├── AuctionDAOImpl.java
│   │   │   │   ├── BidDAOImpl.java
│   │   │   │   └── ItemDAOImpl.java
│   │   │   ├── Controller/                # JavaFX Controllers (MVC)
│   │   │   │   ├── LoginController.java
│   │   │   │   ├── RegisterController.java
│   │   │   │   ├── HomeController.java
│   │   │   │   ├── BidController.java
│   │   │   │   ├── BidDetailController.java
│   │   │   │   ├── SellController.java
│   │   │   │   ├── AutoBidController.java
│   │   │   │   ├── AutoBidManager.java
│   │   │   │   ├── AdminController.java
│   │   │   │   ├── AccounthomeController.java
│   │   │   │   └── SessionManager.java
│   │   │   ├── DTO/                       # Request/Response objects (JSON mapping)
│   │   │   ├── Model/
│   │   │   │   ├── Entity/                # User, Auction, Bid, Item, Seller, Bidder, Admin...
│   │   │   │   ├── Enums/                 # AuctionStatus, BidStatus, ItemType, Role
│   │   │   │   ├── Factory/               # ItemFactory (Factory Pattern)
│   │   │   │   └── Mapper/                # UserMapper, ItemMapper
│   │   │   ├── Network/
│   │   │   │   ├── Server1.java           # Entry point phía Server, TCP port 9090
│   │   │   │   ├── ClientHandler.java     # Xử lý mỗi client trên thread riêng
│   │   │   │   ├── ClientSocket.java      # Singleton TCP client, listener thread
│   │   │   │   ├── RequestHandler.java    # Điều phối request theo type
│   │   │   │   ├── SettlementHandler.java # Logic kết thúc & thanh toán auction
│   │   │   │   └── ServerSessionManager.java
│   │   │   ├── Service/                   # Business logic layer
│   │   │   │   ├── UserService.java
│   │   │   │   ├── AuctionService.java
│   │   │   │   ├── BidService.java
│   │   │   │   ├── AutoBidService.java
│   │   │   │   ├── ItemService.java
│   │   │   │   ├── AdminService.java
│   │   │   │   └── ServiceFactory.java
│   │   │   ├── Exception/                 # Custom exceptions
│   │   │   └── Util/                      # FileUtils, LocalDateTimeAdapter
│   │   └── resources/
│   │       ├── fxml/                      # Giao diện: login, register, home, bid, sell, admin...
│   │       ├── css/                       # Stylesheet (style.css, admin.css, image.css)
│   │       ├── images/                    # Ảnh nền và UI assets
│   │       └── font/                      # Font PressStart2P (pixel style)
│   └── test/
│       └── java/com/btl/n8/Service/
│           └── AuctionServiceTest.java
├── target/
│   ├── server.jar                         # ← Fat JAR phía Server
│   └── client.jar                         # ← Fat JAR phía Client (JavaFX)
├── pom.xml
└── README.md
```

---

## 📦 Vị trí file JAR

Sau khi build, hai file JAR nằm tại:

```
target/server.jar   ← chạy phía server
target/client.jar   ← chạy phía client (có giao diện JavaFX)
```

---

## 🔨 Hướng dẫn Build

Chạy lệnh sau tại thư mục gốc của project (nơi chứa `pom.xml`):

**Linux / macOS:**
```bash
./mvnw clean package -DskipTests
```

**Windows:**
```cmd
mvnw.cmd clean package -DskipTests
```

Lệnh này sẽ tạo ra hai file trong thư mục `target/`:
- `server.jar` — fat JAR chứa Server + MySQL driver + Gson
- `client.jar` — fat JAR chứa Client + JavaFX + MySQL driver + Gson

---

## ▶️ Hướng dẫn chạy

> **Quan trọng:** Phải khởi động Server trước, sau đó mới chạy Client.

### Bước 1 — Chạy Server

```bash
java -jar target/server.jar
```

Kết quả mong đợi:
```
Server đang chạy trên port 9090
```

Server lắng nghe kết nối TCP tại port **9090** và tự động settle các phiên đấu giá hết giờ mỗi 10 giây.

---

### Bước 2 — Chạy Client

Mở một terminal mới (giữ nguyên terminal Server đang chạy):

```bash
java -jar target/client.jar
```

Giao diện đăng nhập sẽ hiện ra. Client mặc định kết nối đến `localhost:9090`.

Để chạy nhiều client cùng lúc (mô phỏng nhiều người dùng), mở thêm terminal và lặp lại lệnh trên.

---

## 🔧 Troubleshooting

**Lỗi `Error: JavaFX runtime components are missing`:**

Điều này xảy ra khi JDK không nhận native lib của JavaFX từ fat JAR. Dùng lệnh sau thay thế:

```bash
java --add-opens java.base/java.lang=ALL-UNNAMED \
     --add-opens java.base/java.util=ALL-UNNAMED \
     -jar target/client.jar
```

**Client báo "Server chưa mở":**

Đảm bảo `server.jar` đã được khởi động và đang chạy trước khi mở client.

**Lỗi kết nối Database:**

Hệ thống kết nối đến MySQL cloud (Railway). Đảm bảo máy có kết nối Internet. Server sẽ in stack trace nếu DB không phản hồi.

---

## 🏗️ Kiến trúc hệ thống

```
┌─────────────────────────────┐        TCP / JSON        ┌──────────────────────────────┐
│         CLIENT              │ ◄──────────────────────► │           SERVER             │
│                             │       port 9090           │                              │
│  JavaFX UI (FXML)           │                           │  Server1 (ServerSocket)      │
│  Controller (MVC)           │                           │  ClientHandler (per thread)  │
│  ClientSocket (Singleton)   │                           │  RequestHandler (dispatcher) │
│  DTO (Request/Response)     │                           │  Service Layer               │
│                             │                           │  DAO Layer                   │
└─────────────────────────────┘                           │  SettlementHandler           │
                                                          │  ScheduledExecutorService    │
                                                          │         │                    │
                                                          │         ▼                    │
                                                          │   MySQL (Railway Cloud)      │
                                                          └──────────────────────────────┘
```

**Design Patterns sử dụng:**
- **MVC** — Controller / Service / DAO tách biệt rõ ràng
- **Singleton** — `ClientSocket`, `SessionManager`
- **Factory** — `ItemFactory`, `ServiceFactory`
- **DAO Pattern** — interface + implementation cho mỗi entity
- **ThreadLocal** — `DataConnection` đảm bảo mỗi thread có Connection riêng

---

## 👥 Thông tin nhóm

**Nhóm 8 — Bài Tập Lớn Lập Trình Mạng**