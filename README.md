# Pokemon Auction System — BTL Nhóm 8

Hệ thống đấu giá trực tuyến theo chủ đề Pokémon, xây dựng theo mô hình **Client–Server** với giao tiếp qua **TCP Socket**. Người dùng có thể đăng ký, đăng nhập, đăng vật phẩm đấu giá, đặt giá thủ công hoặc tự động (Auto-Bid), và xem kết quả thanh toán khi phiên đấu giá kết thúc.

---

## Mô tả bài toán & phạm vi hệ thống

| Tính năng | Mô tả |
|---|---|
| Đăng ký / Đăng nhập | Xác thực người dùng, phân quyền Seller / Bidder / Admin |
| Đăng vật phẩm | Seller tạo phiên đấu giá với giá khởi điểm và thời gian kết thúc |
| Đặt giá (Bid) | Bidder đặt giá thủ công, server kiểm tra tính hợp lệ |
| Auto-Bid | Bidder đặt giá trần, hệ thống tự động đặt giá tăng dần |
| Anti-sniping | Tự động gia hạn thêm 30 giây nếu có bid đặt trong 30 giây cuối phiên |
| Kết thúc phiên | Server tự động settle auction khi hết giờ (mỗi 10 giây), trừ số dư người thắng |
| Quản trị | Admin duyệt / từ chối vật phẩm, quản lý người dùng |
| Giao tiếp | Client ↔ Server qua TCP Socket, truyền JSON (Gson) |
| CSDL | MySQL cloud (Railway), dùng ThreadLocal Connection cho thread-safety |

---

## ️ Công nghệ & Môi trường

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
- Máy cần có kết nối Internet để truy cập database Railway

> ️ **Lưu ý:** `client.jar` chứa JavaFX native libraries đóng gói sẵn. Nếu gặp lỗi JavaFX không tìm thấy, xem mục **Troubleshooting** bên dưới.

---

## Cấu trúc thư mục

```
BTL_Nhom8/
├── src/
│   ├── main/
│   │   ├── java/com/btl/n8/
│   │   │   ├── Main.java                  # Entry point (JavaFX Application)
│   │   │   ├── Launcher.java              # Wrapper để fat JAR bypass JavaFX module check
│   │   │   ├── Connection/                # DAO interfaces & implementations
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
│   │   │   │   ├── Entity/                # User, Auction, Bid, Item, Seller, Bidder, Admin
│   │   │   │   ├── Enums/                 # AuctionStatus, BidStatus, ItemType, Role
│   │   │   │   ├── Factory/               # ItemFactory (Factory Pattern)
│   │   │   │   └── Mapper/                # UserMapper, ItemMapper
│   │   │   ├── Network/
│   │   │   │   ├── Server1.java           # Entry point phía Server, TCP port 9090
│   │   │   │   ├── ClientHandler.java     # Xử lý mỗi client trên thread riêng
│   │   │   │   ├── ClientSocket.java      # Singleton TCP client + listener thread
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
│   │   │   └── Util/                      # FileUtils, Adapters (JSON/Type)
│   │   └── resources/
│   │       ├── fxml/                      # login, register, home, bid, sell, admin, account...
│   │       ├── css/                       # style.css, admin.css, image.css
│   │       ├── images/                    # UI assets & backgrounds
│   │       └── font/                      # PressStart2P (pixel font)
│   └── test/
│       └── java/com/btl/n8/Service/       # Unit tests (JUnit 5 + Mockito)
├── target/
│   ├── server.jar                         # ← Fat JAR phía Server   sau khi build
│   └── client.jar                         # ← Fat JAR phía Client   sau khi build
├── pom.xml                                # Maven build config + maven-shade-plugin
└── README.md
```

---

## Vị trí file JAR

Sau khi build và copy (xem mục Build bên dưới), hai file JAR nằm tại **thư mục gốc**:

```
server.jar   ← chạy phía Server (không cần JavaFX)
client.jar   ← chạy phía Client (có giao diện JavaFX)
```

---

## Hướng dẫn Build Fat JAR (Uber JAR)

Project dùng **maven-shade-plugin** để đóng gói toàn bộ dependencies (JavaFX, Gson, MySQL driver) vào một file JAR duy nhất, giúp chạy trực tiếp bằng `java -jar` mà không cần cài thêm gì.

### Cách build

Chạy lệnh sau tại **thư mục gốc của project** (nơi chứa `pom.xml`):

**Linux / macOS:**
```bash
./mvnw clean package -DskipTests
```

**Windows (Command Prompt):**
```cmd
mvnw.cmd clean package -DskipTests
```

**Windows (PowerShell):**
```powershell
.\mvnw.cmd clean package -DskipTests
```

> Cờ `-DskipTests` bỏ qua unit test để build nhanh hơn. Nếu muốn chạy test, bỏ cờ này.

Sau khi build xong, copy JAR ra thư mục gốc để chạy cho tiện:

**Linux / macOS:**
```bash
cp target/server.jar server.jar
cp target/client.jar client.jar
```

**Windows:**
```cmd
copy target\server.jar server.jar
copy target\client.jar client.jar
```

### Quá trình build làm gì?

Lệnh `mvn clean package` thực hiện theo thứ tự:
1. `clean` — Xóa thư mục `target/` cũ
2. `compile` — Biên dịch toàn bộ source code Java
3. `package` — Kích hoạt maven-shade-plugin, tạo 2 fat JAR:
   - `target/server.jar` với main class `com.btl.n8.Network.Server1`
   - `target/client.jar` với main class `com.btl.n8.Launcher`

### Cấu hình maven-shade-plugin trong pom.xml

Plugin được cấu hình với **2 execution** — mỗi execution tạo một JAR:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.5.1</version>
    <executions>

        <!-- Tạo client.jar -->
        <execution>
            <id>client-jar</id>
            <phase>package</phase>
            <goals><goal>shade</goal></goals>
            <configuration>
                <outputFile>${project.build.directory}/client.jar</outputFile>
                <transformers>
                    <transformer implementation="...ManifestResourceTransformer">
                        <mainClass>com.btl.n8.Launcher</mainClass>
                    </transformer>
                    <transformer implementation="...ServicesResourceTransformer"/>
                </transformers>
                <filters>
                    <filter>
                        <artifact>*:*</artifact>
                        <excludes>
                            <exclude>module-info.class</exclude>
                            <exclude>META-INF/*.SF</exclude>
                            ...
                        </excludes>
                    </filter>
                </filters>
            </configuration>
        </execution>

        <!-- Tạo server.jar -->
        <execution>
            <id>server-jar</id>
            <phase>package</phase>
            <goals><goal>shade</goal></goals>
            <configuration>
                <outputFile>${project.build.directory}/server.jar</outputFile>
                <transformers>
                    <transformer implementation="...ManifestResourceTransformer">
                        <mainClass>com.btl.n8.Network.Server1</mainClass>
                    </transformer>
                    ...
                </transformers>
            </configuration>
        </execution>

    </executions>
</plugin>
```

**Giải thích các tham số quan trọng:**

| Tham số | Ý nghĩa |
|---|---|
| `ManifestResourceTransformer` | Ghi `Main-Class` vào `MANIFEST.MF` để `java -jar` biết class nào cần chạy |
| `ServicesResourceTransformer` | Gộp các file `META-INF/services/` từ nhiều JAR, tránh mất SPI providers |
| `module-info.class` (exclude) | Loại bỏ để tránh xung đột module system trong fat JAR |
| `META-INF/*.SF/.DSA/.RSA` (exclude) | Loại bỏ chữ ký số của các thư viện có ký số, tránh lỗi security |

**Tại sao dùng `Launcher` thay vì `Main` cho client?**

`Main` extends `Application` (JavaFX). Khi fat JAR không có module JavaFX đúng cách, JVM sẽ từ chối chạy thẳng `Main`. Cách bypass chuẩn là tạo một class wrapper `Launcher` **không** extends `Application`, class này gọi `Main.main()`. JVM khởi động `Launcher` bình thường, sau đó JavaFX runtime load theo.

---

## ▶️ Hướng dẫn chạy

> **Quan trọng:** Phải khởi động **Server trước**, sau đó mới chạy Client.

### Bước 1 — Chạy Server

```bash
java -jar server.jar
```

Kết quả mong đợi:
```
Server đang chạy trên port 9090
```

Server lắng nghe kết nối TCP tại port **9090** và tự động settle các phiên đấu giá hết giờ mỗi 10 giây.

---

### Bước 2 — Chạy Client

Mở **terminal mới** (giữ nguyên terminal Server đang chạy):

```bash
java -jar client.jar
```

Giao diện đăng nhập sẽ hiện ra. Client mặc định kết nối đến `localhost:9090`.

Để mô phỏng nhiều người dùng, mở thêm terminal và lặp lại lệnh trên:

```bash
java -jar client.jar
```

---

## Danh sách chức năng đã hoàn thành

### Phía Client (JavaFX GUI)
- [x] Đăng ký tài khoản người dùng
- [x] Đăng nhập / Đăng xuất
- [x] Xem danh sách phiên đấu giá đang mở
- [x] Xem chi tiết phiên đấu giá (giá hiện tại, thời gian còn lại, lịch sử bid)
- [x] Đặt giá thủ công (Manual Bid)
- [x] Đặt giá tự động (Auto-Bid) với giá trần
- [x] Đăng vật phẩm lên đấu giá (Seller)
- [x] Xem danh sách vật phẩm đã đăng (Seller)
- [x] Xem lịch sử các lần đặt giá của tài khoản
- [x] Nạp tiền vào tài khoản (Deposit)
- [x] Nâng cấp lên quyền Seller
- [x] Giao diện Admin: duyệt / từ chối vật phẩm, quản lý người dùng
- [x] Nhận thông báo real-time từ server (auction settled, bid outbid...)

### Phía Server
- [x] Xử lý đa kết nối đồng thời (ThreadPool — 10 threads)
- [x] Xác thực người dùng qua token UUID
- [x] Định tuyến request theo `type` field trong JSON
- [x] Kiểm tra hợp lệ bid (giá phải cao hơn giá hiện tại, số dư đủ...)
- [x] Auto-Bid engine (tự động tăng giá khi bị vượt qua)
- [x] Scheduled settlement: kiểm tra và kết thúc auction hết giờ mỗi 10 giây
- [x] Anti-sniping: tự động gia hạn thêm 30 giây nếu có bid đặt trong vòng 30 giây cuối
- [x] Trừ số dư người thắng, hoàn tiền người thua sau khi settle
- [x] Push notification đến tất cả client liên quan sau khi settle
- [x] Kết nối MySQL cloud với ThreadLocal Connection (thread-safe)

### Design Patterns & Kiến trúc
- [x] MVC — Controller / Service / DAO tách biệt
- [x] Singleton — `ClientSocket`, `SessionManager`, `AppEventBus`
- [x] Factory — `ItemFactory`, `ServiceFactory`
- [x] DAO Pattern — interface + implementation
- [x] Observer / Event Bus — `AppEventBus` cho giao tiếp trong UI
- [x] ThreadLocal — `DataConnection` thread-safe connection pool

---

## Troubleshooting

**Lỗi `Error: JavaFX runtime components are missing`:**

```bash
java --add-opens java.base/java.lang=ALL-UNNAMED \
     --add-opens java.base/java.util=ALL-UNNAMED \
     -jar client.jar
```

**Client báo không kết nối được Server:**

Đảm bảo `server.jar` đã được khởi động và terminal Server hiển thị `Server đang chạy trên port 9090` trước khi mở client.

**Lỗi kết nối Database:**

Hệ thống kết nối đến MySQL cloud (Railway). Kiểm tra máy có kết nối Internet. Server sẽ in stack trace nếu DB không phản hồi.

**Build lỗi `Permission denied` (Linux/macOS):**

```bash
chmod +x mvnw
./mvnw clean package -DskipTests
```

---

## ️ Kiến trúc hệ thống

```
┌─────────────────────────────┐        TCP / JSON        ┌──────────────────────────────┐
│         CLIENT              │ ◄──────────────────────► │           SERVER             │
│                             │       port 9090           │                              │
│  JavaFX UI (FXML)           │                           │  Server1 (ServerSocket)      │
│  Controller (MVC)           │                           │  ClientHandler (per thread)  │
│  ClientSocket (Singleton)   │                           │  RequestHandler (dispatcher) │
│  DTO (Request/Response)     │                           │  Service Layer               │
│  AppEventBus (Observer)     │                           │  DAO Layer                   │
└─────────────────────────────┘                           │  SettlementHandler           │
                                                          │  ScheduledExecutorService    │
                                                          │         │                    │
                                                          │         ▼                    │
                                                          │   MySQL (Railway Cloud)      │
                                                          └──────────────────────────────┘
```

---

## Thông tin nhóm

**Nhóm 8 — Bài Tập Lớn Lập Trình Mạng**