# DevSpot — Tổng quan ứng dụng và rà soát giao diện

> Tài liệu được tổng hợp từ mã nguồn Android hiện có và báo cáo LTDD Nhóm 10. Các giao diện được liệt kê ở đây là những layout XML đã có trong repository; sự hiện diện của layout **không đồng nghĩa** màn hình đã được nối điều hướng hoặc có chức năng chạy được.

## 1. DevSpot là gì?

DevSpot là ý tưởng nền tảng portfolio kết hợp mạng xã hội và bản đồ tương tác, kết nối sinh viên/nhà phát triển (Creator) với doanh nghiệp/người tuyển dụng (Recruiter). Creator đưa dự án, repo hoặc bản demo lên bản đồ dưới dạng ghim; Recruiter khám phá dự án và ứng viên theo vị trí, công nghệ và bán kính.

Theo báo cáo, phạm vi cốt lõi gồm tạo portfolio, ghim dự án lên bản đồ, khám phá theo không gian và kết nối giữa hai vai trò. Các thao tác xã hội được định hướng gồm Like, Comment, Follow, Bookmark và Message. Talent Matching/AI được mô tả là định hướng mở rộng, không phải phần bắt buộc của phạm vi hiện tại.

## 2. Vai trò và luồng nghiệp vụ dự kiến

### Creator

1. Chọn vai trò và đăng ký tài khoản Creator.
2. Tạo/cập nhật hồ sơ: trường học, ngành học, giới thiệu, liên kết và kỹ năng.
3. Tạo dự án, nhập mô tả và liên kết repo/demo, chọn vị trí để thả ghim.
4. Khám phá bản đồ, nhận tín hiệu/lời mời, xem tin nhắn và quản lý quyền riêng tư vị trí.

### Recruiter

1. Chọn vai trò và đăng ký tài khoản Recruiter/doanh nghiệp.
2. Tìm kiếm ứng viên/dự án theo từ khóa, công nghệ và bán kính trên Radar.
3. Lưu ứng viên/dự án, xem chi tiết dự án, gửi đánh giá hoặc lời mời phỏng vấn.
4. Theo dõi tín hiệu, trao đổi với ứng viên và quản lý thông tin công ty/cài đặt.

## 3. Danh mục giao diện hiện có

| Nhóm | Layout | Nội dung thể hiện trong giao diện |
|---|---|---|
| Khởi động/đăng nhập | `activity_splash.xml`, `activity_login.xml` | Splash DevSpot hiển thị khoảng 1,5 giây trước khi vào luồng chọn vai trò; form đăng nhập, duy trì phiên, liên kết “Quên mật khẩu?” và đăng ký. |
| Đăng ký/khôi phục | `activity_role_selection.xml`, `activity_register_creator.xml`, `activity_register_recruiter.xml`, `activity_password_reset.xml` | Chọn Creator/Recruiter; form đăng ký hai vai trò và giao diện khôi phục mật khẩu. |
| Creator | `fragment_creator_home.xml`, `fragment_creator_profile.xml`, `fragment_edit_profile.xml`, `fragment_drop_project_pin.xml` | Home/Radar, hồ sơ và kỹ năng, chỉnh sửa hồ sơ, form ghim dự án với tọa độ GPS và liên kết repo/demo. |
| Recruiter/khám phá | `fragment_recruiter_home.xml`, `activity_creator_public_profile.xml`, `activity_company_profile.xml`, `bottom_sheet_filter_radar.xml`, `fragment_cluster_nodes.xml` | Radar khu vực, hồ sơ Creator công khai, trang công ty, lọc bán kính/công nghệ và xem cụm node. |
| Dự án/đánh giá | `activity_project_detail.xml`, `activity_project_review.xml`, `item_pinned_project_card.xml` | Chi tiết dự án, liên kết GitHub/demo, thao tác thích/theo dõi/bình luận/lưu, gửi đánh giá repo và thẻ dự án. |
| Tín hiệu/kết nối | `fragment_signal_stream.xml`, `fragment_inbox.xml`, `activity_chat_detail.xml`, `activity_interview_response.xml`, `item_signal_card.xml`, `item_inbox_chat.xml`, `item_chat_message_received.xml` | Dòng tín hiệu/lời mời, phản hồi lời mời, danh sách hộp thư, chat và các thẻ tin nhắn/tín hiệu. |
| Lưu và điều hướng | `fragment_saved_bookmarks.xml`, `layout_bottom_navigation.xml` | Danh sách mục đã lưu; thanh điều hướng với Bản đồ, Cụm node, Tín hiệu, Đã lưu và Tin nhắn. |
| Phỏng vấn | `activity_interview_dispatch.xml`, `activity_interview_success.xml` | Soạn lời mời, chọn lịch/địa điểm/đề xuất lương; màn hình xác nhận đã gửi và trạng thái chờ phản hồi. |
| Hồ sơ vị trí/tài khoản | `fragment_geo_privacy.xml`, `fragment_system_settings.xml`, `activity_logout_confirm.xml` | Chế độ vị trí chính xác/khu vực/ẩn, cài đặt bảo mật-ngôn ngữ-thông báo và xác nhận đăng xuất. |
| Trạng thái dùng chung | `activity_ui_state.xml` | Giao diện trạng thái tải/rỗng/lỗi và hành động thử lại/quay lại; form có kiểm tra trường bắt buộc. |
| Placeholder | `activity_main.xml` | Layout mẫu “Hello World”; không phải màn hình chính theo sản phẩm. |

## 4. Cập nhật sau khi hoàn thiện các giao diện

### Luồng đã được nối ở mức giao diện

- Splash → chọn vai trò → đăng ký hoặc đăng nhập → Home Creator/Recruiter.
- Hai Home có bản đồ OSMDroid, ghim vị trí gần đây khi có quyền hoặc dùng tọa độ Đà Nẵng tham khảo; chạm trạng thái vị trí để xin quyền/cài đặt GPS.
- Điều hướng qua lại giữa các tab; trạng thái tab hiện tại được nhấn màu đồng nhất. Có lối tắt cài đặt từ hai Home và điều hướng tới cụm node, tín hiệu, đã lưu, hộp thư, hồ sơ cá nhân/công khai và công ty.
- Các luồng tạo ghim, lọc Radar, chi tiết dự án, thao tác Like/Follow/Comment/Bookmark, đánh giá repo, lời mời phỏng vấn và phản hồi lời mời.
- Có giao diện khôi phục mật khẩu, trạng thái dùng chung, chat mẫu, xác nhận đăng xuất và kiểm tra một số trường bắt buộc.

### Còn lại

- **Xử lý lỗi tải bản đồ:** hiện đã có trạng thái quyền/GPS và vị trí dự phòng; chưa có nhận biết lỗi tải tile/mất mạng để tự hiển thị và thử lại trạng thái lỗi.
- **Trạng thái danh sách theo dữ liệu thật:** mẫu hộp thư/tín hiệu/đã lưu đang dùng dữ liệu minh họa; cần nối trạng thái rỗng/lỗi của từng danh sách khi tích hợp API.
- **Backend và lưu trữ:** đăng nhập/đăng ký, reset mật khẩu, tạo ghim, lọc, đánh giá, bookmark/follow/like/comment, lời mời và tin nhắn chưa được gửi/lưu từ máy chủ. Các thao tác ghi dữ liệu hiện chỉ xác thực hoặc phản hồi ở mức giao diện demo.

## 5. Công nghệ và cấu trúc dự án

- **Nền tảng/UI:** Android, Kotlin, layout XML, ViewBinding được bật trong `app/build.gradle.kts`.
- **Thành phần hiện có:** AndroidX Activity/AppCompat/Core, ConstraintLayout và Material Components.
- **Thư viện khai báo:** Retrofit + Gson, OkHttp logging interceptor, Coroutines, Glide, ViewModel, LiveData và RecyclerView.
- **Bản đồ:** OSMDroid; hai màn hình Radar khai báo MapView và cấu hình điểm gần nhất/dự phòng.
- **Điểm vào:** `app/src/main/java/com/example/devspot/view/MainActivity.kt`.
- **Giao diện:** `app/src/main/res/layout/`.
- **Manifest:** `app/src/main/AndroidManifest.xml`.
- **Tài liệu sản phẩm tham chiếu:** Báo cáo LTDD Nhóm 10 được cung cấp cùng yêu cầu.

## 6. Phạm vi và xác minh

Các màn hình/điều hướng đã được nối trong `MainActivity` như một bản xem trước UI; chưa có xác thực hay lưu dữ liệu thật. Đã kiểm tra bằng `:app:assembleDebug`, `:app:testDebugUnitTest` và xác nhận Gradle resolve OSMDroid `6.1.20`. Trên AVD Android 37.1, app không crash và UI hierarchy có nội dung nhưng screenshot đen; ứng dụng Cài đặt vẫn hiển thị bình thường. Cùng APK trên AVD `DevSpot_37_2_Beta` (Android 37.2-beta3, Google APIs PS16K, software renderer) hiển thị đúng màn chọn Creator/Recruiter, nên lỗi màn đen thuộc image Android 37.1 của AVD chứ không phải luồng khởi chạy trong app. `MapView` và marker cũng hiển thị trên beta AVD; tile OpenStreetMap không tải được vì môi trường hiện tại không kết nối được tới `tile.openstreetmap.org`. Dùng AVD beta để kiểm thử giao diện; cần kết nối mạng có thể truy cập dịch vụ tile để thấy bản đồ nền.
