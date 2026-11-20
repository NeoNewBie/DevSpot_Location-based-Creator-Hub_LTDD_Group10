# DevSpot — Tổng quan ứng dụng và rà soát giao diện

> Tài liệu được tổng hợp từ mã nguồn Android hiện có và báo cáo LTDD Nhóm 10. Các giao diện được liệt kê ở đây là những layout XML đã có trong repository; sự hiện diện của layout **không đồng nghĩa** màn hình đã được nối điều hướng hoặc có chức năng chạy được.

## 1. Giới thiệu ứng dụng

DevSpot (Location-based Creator Hub) là ứng dụng Android hướng đến việc kết nối sinh viên/nhà phát triển (Creator) với doanh nghiệp/người tuyển dụng (Recruiter) thông qua portfolio và bản đồ tương tác. Thay vì chỉ xem hồ sơ dạng danh sách, người dùng có thể khám phá Creator, dự án công nghệ và cơ hội quanh trường học hoặc khu vực quan tâm.

Creator xây dựng hồ sơ năng lực, chia sẻ liên kết repo/demo và định vị dự án trên Radar; Recruiter tìm kiếm ứng viên theo vị trí, công nghệ và bán kính, xem hồ sơ/dự án rồi chủ động kết nối. Sản phẩm tập trung vào hệ sinh thái sinh viên và doanh nghiệp công nghệ địa phương, với Đà Nẵng là khu vực minh họa trong dữ liệu mẫu.

Phạm vi ý tưởng gồm portfolio, ghim dự án trên bản đồ, khám phá theo không gian và kết nối hai vai trò. Like, Comment, Follow, Bookmark và Message là các chức năng xã hội bổ trợ. Talent Matching/AI là định hướng mở rộng, chưa phải chức năng được triển khai trong ứng dụng hiện tại. DevSpot hiện là prototype: ngoài đăng nhập và đăng ký, phần lớn nội dung và tương tác vẫn dùng dữ liệu mẫu hoặc chỉ lưu trên thiết bị.

## 2. Các chức năng chính

**Cách đọc trạng thái:** **Kết nối API** nghĩa là thao tác gọi backend; **Lưu trên thiết bị** nghĩa là lưu cục bộ theo tài khoản; **Xem trước** nghĩa là dùng dữ liệu mẫu hoặc chưa lưu/gửi lên máy chủ.

### Dùng chung

- **Tài khoản:** chọn vai trò, đăng ký và đăng nhập (**Kết nối API**). Quên/đổi mật khẩu mới có giao diện, chưa gửi yêu cầu backend.
- **Radar và vị trí:** xem bản đồ OpenTopoMap cùng ghim mẫu; xác minh GPS mới và xem tọa độ trên thiết bị. Chọn Exact/Area/Hidden (**Lưu trên thiết bị**); vị trí chưa gửi lên server.
- **Điều hướng:** sáu tab Radar, Khám phá/Creator, Tin báo, Đã lưu, Tin nhắn và Hồ sơ/Công ty; nội dung thay đổi theo vai trò.

### Creator

- Xem và chỉnh sửa hồ sơ cá nhân (**Lưu trên thiết bị**); hồ sơ Creator công khai và các dự án hiển thị là **Xem trước**.
- Tạo ghim dự án và nhập thông tin repo/demo (**Xem trước**, chưa lưu database); xem cụm trường/cộng đồng mẫu.
- Lưu dự án (**Lưu trên thiết bị**). Like, Follow, Comment và đánh giá dự án hiện là **Xem trước**.
- Xem hộp thư, chat, tin báo và lời mời phỏng vấn (**Xem trước**); tin nhắn/phản hồi chưa gửi backend.

### Recruiter

- Xem Radar với Creator mẫu; tìm kiếm và lọc theo từ khóa/công nghệ/bán kính (**Xem trước**, chưa truy vấn API).
- Xem hồ sơ Creator và lưu/bỏ lưu hồ sơ (**Lưu trên thiết bị**); thông tin công ty hiển thị là dữ liệu mẫu, chỉnh sửa chỉ lưu cục bộ.
- Soạn lời mời phỏng vấn và xem màn hình xác nhận (**Xem trước**, chưa gửi ứng viên); hộp thư và tin báo cũng dùng dữ liệu mẫu.

## 3. Danh mục giao diện hiện có

| Nhóm | Layout | Nội dung thể hiện trong giao diện |
|---|---|---|
| Khởi động/đăng nhập | `activity_splash.xml`, `activity_login.xml` | Splash DevSpot hiển thị ngắn trước khi mở đăng nhập; có liên kết “Quên mật khẩu?” và đăng ký. |
| Đăng ký/khôi phục | `activity_role_selection.xml`, `activity_register_creator.xml`, `activity_register_recruiter.xml`, `activity_password_reset.xml` | Chọn Creator/Recruiter; form đăng ký hai vai trò và giao diện khôi phục mật khẩu. |
| Creator | `fragment_creator_home.xml`, `fragment_creator_profile.xml`, `fragment_edit_profile.xml`, `fragment_drop_project_pin.xml` | Home/Radar, hồ sơ và kỹ năng, chỉnh sửa hồ sơ, form ghim dự án với tọa độ GPS và liên kết repo/demo. |
| Recruiter/khám phá | `fragment_recruiter_home.xml`, `activity_creator_public_profile.xml`, `activity_company_profile.xml`, `bottom_sheet_filter_radar.xml`, `fragment_cluster_nodes.xml` | Radar khu vực, hồ sơ Creator công khai, trang công ty, lọc bán kính/công nghệ và xem cụm node. |
| Dự án/đánh giá | `activity_project_detail.xml`, `activity_project_review.xml`, `item_pinned_project_card.xml` | Chi tiết dự án, liên kết GitHub/demo, thao tác thích/theo dõi/bình luận/lưu, gửi đánh giá repo và thẻ dự án. |
| Tín hiệu/kết nối | `fragment_signal_stream.xml`, `fragment_inbox.xml`, `activity_chat_detail.xml`, `activity_interview_response.xml`, `item_signal_card.xml`, `item_inbox_chat.xml`, `item_chat_message_received.xml` | Dòng tín hiệu/lời mời, phản hồi lời mời, danh sách hộp thư, chat và các thẻ tin nhắn/tín hiệu. |
| Lưu và điều hướng | `fragment_saved_bookmarks.xml`, `layout_bottom_navigation.xml` | Danh sách mục đã lưu; thanh điều hướng dùng chung gồm Radar, Khám phá/Creator, Tin báo, Đã lưu, Tin nhắn và Hồ sơ/Công ty. |
| Phỏng vấn | `activity_interview_dispatch.xml`, `activity_interview_success.xml` | Form lời mời có lịch/địa điểm/đề xuất lương; màn hình xác nhận và trạng thái chờ phản hồi trong luồng xem trước. |
| Hồ sơ vị trí/tài khoản | `fragment_geo_privacy.xml`, `fragment_system_settings.xml`, `activity_logout_confirm.xml` | Chế độ vị trí chính xác/khu vực/ẩn, cài đặt bảo mật-ngôn ngữ-thông báo và xác nhận đăng xuất. |
| Trạng thái dùng chung | `activity_ui_state.xml` | Layout có vùng biểu thị tiến trình/thông báo và hai nút; hiện luồng dùng chung chỉ gán tiêu đề/nội dung, cả hai nút đều quay lại, chưa thực hiện thử lại tải dữ liệu. |
| Placeholder | `activity_main.xml` | Layout mẫu “Hello World”; không phải màn hình chính theo sản phẩm. |

## 4. Cập nhật sau khi hoàn thiện các giao diện

### Luồng đã được nối ở mức giao diện

- Splash → đăng nhập; đăng ký đi qua bước chọn vai trò rồi mở form Creator/Recruiter. Hai form gọi `POST /api/auth/register`; đăng ký thành công mở Home theo vai trò, lỗi được hiển thị ngay trên form.
- Hai Home có bản đồ OSMDroid và dữ liệu ghim mẫu. App có thể dùng vị trí tham khảo gần đây hoặc Đà Nẵng; thao tác xác minh yêu cầu vị trí mới từ thiết bị, hiển thị tọa độ/độ chính xác cục bộ.
- Sáu tab chính dùng cùng một thanh điều hướng cho cả hai vai trò; tab Hồ sơ mở Creator Profile hoặc Company Profile theo vai trò. Tab đang mở được tô màu nhất quán.
- Màn Quyền riêng tư hỗ trợ Exact/Area/Hidden, nút bỏ chọn và mặc định ẩn nếu áp dụng khi không có lựa chọn. Cấu hình lưu trong SharedPreferences theo email, chưa đồng bộ với SQL Server.
- Chỉnh sửa hồ sơ Creator/Company và các mục đã lưu được ghi cục bộ vào SharedPreferences theo email; chúng tồn tại trên thiết bị nhưng không đồng bộ lên API. Tương tác Like/Follow trên chi tiết dự án và nội dung chat/tín hiệu khác chỉ là trạng thái mẫu trong giao diện/phiên chạy.
- Các luồng tạo ghim, lọc Radar, chi tiết dự án, thao tác Like/Follow/Comment/Bookmark, đánh giá repo, lời mời phỏng vấn và phản hồi lời mời.
- Có giao diện khôi phục mật khẩu, trạng thái dùng chung, chat mẫu, xác nhận đăng xuất và kiểm tra một số trường bắt buộc. Giao diện trạng thái dùng chung chỉ hiển thị nội dung được truyền vào và quay lại; chưa có cơ chế thử lại tải dữ liệu.

### Còn lại

- **Xử lý lỗi tải bản đồ:** hiện đã có trạng thái quyền/GPS và vị trí dự phòng; chưa có nhận biết lỗi tải tile/mất mạng để tự hiển thị và thử lại trạng thái lỗi.
- **Trạng thái danh sách theo dữ liệu thật:** mẫu hộp thư/tín hiệu/đã lưu đang dùng dữ liệu minh họa; cần nối trạng thái rỗng/lỗi của từng danh sách khi tích hợp API.
- **Backend và lưu trữ:** Android đăng nhập và đăng ký qua API ASP.NET Core; API xác thực bằng PBKDF2 và tạo Users cùng CreatorProfiles hoặc Companies/RecruiterProfiles trong SQL transaction. Reset mật khẩu, tạo ghim, lọc, đánh giá, bookmark/follow/like/comment, lời mời và tin nhắn chưa được lưu vào database.
- **SQL Server:** [`database/DevSpot_CreatorHub.sql`](../database/DevSpot_CreatorHub.sql) tạo schema Creator/Project/Map và dữ liệu mẫu. Tài khoản demo dùng chung mật khẩu `DevSpotDemo@2026!`, lưu dạng PBKDF2-SHA256. API và cách chạy được hướng dẫn trong [`backend/DevSpot.Api`](../backend/DevSpot.Api) và README.

## 5. Công nghệ và cấu trúc dự án

- **Nền tảng/UI:** Android, Kotlin, layout XML, ViewBinding được bật trong `app/build.gradle.kts`.
- **Thành phần hiện có:** AndroidX Activity/AppCompat/Core, ConstraintLayout và Material Components.
- **Thư viện khai báo:** Retrofit + Gson, OkHttp logging interceptor, Coroutines, Glide, ViewModel, LiveData và RecyclerView.
- **Bản đồ:** OSMDroid; hai màn hình Radar khai báo MapView và cấu hình điểm gần nhất/dự phòng.
- **Cấu trúc Android:** `app/src/main/java/com/example/devspot/view/MainActivity.kt` là Activity launcher, giữ điều hướng, xử lý biểu mẫu, trạng thái và dữ liệu mẫu. Các màn được đổi bằng cách nạp layout qua `setContentView`; tên `fragment_*.xml` chỉ là quy ước tên file, không phải Android Fragment. `app/src/main/res/layout/` chứa layout và `app/src/main/AndroidManifest.xml` khai báo quyền mạng/vị trí cùng cấu hình ứng dụng.
- **API client:** `app/src/main/java/com/example/devspot/data/DevSpotApi.kt` khai báo đăng nhập/đăng ký; `DevSpotApiClient.kt` khởi tạo Retrofit. URL lấy từ Gradle property `devspotApiBaseUrl`, mặc định `http://127.0.0.1:5080/`.
- **Backend:** `backend/DevSpot.Api/Program.cs` là ASP.NET Core minimal API, dùng .NET 10 và `Microsoft.Data.SqlClient`; cấu hình kết nối SQL Server chỉ nằm ở backend. Hướng dẫn chạy và cấu hình môi trường phát triển nằm trong `README.md`.
- **Dữ liệu:** `database/DevSpot_CreatorHub.sql` tạo cơ sở dữ liệu `DevSpotCreatorHub`, schema và dữ liệu demo. Script có các bảng tài khoản/hồ sơ, công ty, dự án/kỹ năng, tương tác xã hội, tin nhắn, lời mời phỏng vấn, đánh giá và thông báo.
- **Yêu cầu build Android hiện tại:** `compileSdk`, `targetSdk` và `minSdk` đều là API 37; Gradle daemon được cấu hình dùng JDK 21. `gradle.properties` còn chứa đường dẫn JDK tuyệt đối của một máy Windows, nên cần điều chỉnh nếu build trên máy khác.
- **Tài liệu sản phẩm tham chiếu:** Báo cáo LTDD Nhóm 10 được cung cấp cùng yêu cầu.

## 6. Phạm vi tích hợp và xác minh

Backend hiện có ba endpoint: `GET /api/health/db`, `POST /api/auth/login` và `POST /api/auth/register`. Đăng nhập kiểm tra mật khẩu PBKDF2-SHA256; đăng ký tạo tài khoản và hồ sơ Creator hoặc Recruiter trong cùng SQL transaction. Login và register lần lượt giới hạn 5 và 3 yêu cầu mỗi phút. Client không nhận token/session từ backend; trạng thái đăng nhập chỉ giữ trong bộ nhớ của Activity và bị mất khi tiến trình ứng dụng kết thúc.

Schema SQL đã có các bảng cho nhiều luồng sản phẩm, nhưng hiện chưa có API đọc/ghi các dự án, ghim, hồ sơ, like, follow, comment, bookmark, chat, đánh giá, lời mời, thông báo hoặc quyền riêng tư. Bản đồ, hồ sơ công khai, hộp thư và tín hiệu trong app vì vậy dùng dữ liệu mẫu; không được hiểu là dữ liệu từ database demo. Đăng ký tài khoản mới và đăng nhập là các luồng hiện kết nối backend.

Build cần API 37 SDK/platform và JDK 21 theo cấu hình hiện tại; backend cần .NET 10 SDK cùng SQL Server. Android mặc định gọi loopback API trên cổng 5080, nên emulator cần cấu hình kết nối phù hợp (xem README). Manifest cho phép HTTP cleartext để phát triển cục bộ; không dùng cấu hình này cho triển khai production. Tile OpenTopoMap cần mạng; app chưa có trạng thái lỗi tile hoặc tự thử tải lại.

Các test hiện có trong `app/src/test/` và `app/src/androidTest/` vẫn là test mẫu (phép cộng và kiểm tra package name); chúng không kiểm chứng luồng đăng nhập/đăng ký, điều hướng hay tính năng sản phẩm. Build/test từng được ghi nhận chạy thành công sau các thay đổi trước đây, nhưng kết quả đó không chứng minh các luồng xem trước đã hoàn chỉnh hoặc được kiểm thử tự động.
