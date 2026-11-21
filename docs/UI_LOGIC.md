# Luồng giao diện DevSpot

Tài liệu này mô tả điều hướng hiện tại của ứng dụng Android và phân biệt chức năng đã nối API với hành vi chỉ chạy trên thiết bị.

## 1. Khởi động và xác thực

1. Ứng dụng hiển thị Splash trong thời gian ngắn, sau đó mở màn Đăng nhập.
2. Đăng nhập gửi email và mật khẩu tới `POST /api/auth/login`. Nếu thành công, vai trò trả về từ API quyết định mở Creator Radar hay Recruiter Radar.
3. Nếu thông tin sai hoặc API không sẵn sàng, thông báo lỗi xuất hiện ngay trên màn đăng nhập; người dùng vẫn ở màn này để thử lại.
4. Chọn “Đăng ký ngay” sẽ mở màn chọn vai trò. Không vai trò nào được chọn sẵn; thẻ được chọn có viền, nền, chữ và dấu tích màu xanh. Nút tiếp tục chỉ bật sau khi chọn Creator hoặc Recruiter.
5. Màn tiếp theo là biểu mẫu Creator hoặc Recruiter. Nút “Đổi vai trò” quay lại bước chọn vai trò; liên kết “Đăng nhập ngay” mở đăng nhập.
6. Gửi biểu mẫu hợp lệ gọi `POST /api/auth/register`. API kiểm tra dữ liệu, băm mật khẩu PBKDF2-SHA256 và tạo tài khoản cùng hồ sơ vai trò trong một SQL transaction. Recruiter liên kết với công ty hiện có hoặc tạo công ty mới. Đăng ký thành công mở Home theo vai trò vừa tạo; email trùng hoặc lỗi kết nối được hiển thị trên form.
7. Quên mật khẩu, gửi lời mời, nhắn tin và các thao tác chưa có endpoint hiển thị thông báo xem trước thay vì xác nhận đã lưu/gửi lên máy chủ.

## 2. Điều hướng theo vai trò

### Creator

- **Radar**: xem bản đồ gồm dự án và Creator mẫu; chạm ghim dự án mở chi tiết dự án hoặc CV tác giả. Chạm ghim Creator mở hồ sơ với các thao tác kết bạn và nhắn tin. Chạm trạng thái vị trí sẽ xin quyền nếu cần và lấy một vị trí mới để xác minh, không chỉ dùng vị trí cuối cùng.
- Trên Radar, hai nút Tin nhắn và Thông báo ở thanh trên mở trực tiếp Hộp thư và Dòng thông báo.
- **Khám phá**: xem cụm trường học/cộng đồng mẫu.
- **Hồ sơ**: xem và chỉnh sửa hồ sơ Creator ở chế độ xem trước.
- **Đã lưu**: hiển thị các dự án đã đánh dấu. Trong chi tiết dự án, chạm biểu tượng lưu để thêm/bỏ dự án.
- Hộp thư có các cuộc trò chuyện mẫu với nhà tuyển dụng; có thể mở cuộc trò chuyện và nhập/gửi tin trong phiên xem trước. Creator xem hồ sơ công khai của người khác có thể theo dõi hoặc nhắn tin; không hiển thị thao tác mời phỏng vấn dành riêng cho Recruiter.
- Dòng thông báo minh họa lời mời phỏng vấn, lượt xem dự án và kết nối mới.

### Recruiter

- **Radar**: xem các ghim Creator mẫu; chạm ghim để mở CV hoặc bắt đầu lời mời tuyển dụng. Có các lối tắt Tin nhắn và Thông báo ở thanh trên; chạm trạng thái vị trí để xác minh vị trí mới từ thiết bị.
- **Đã lưu**: chỉ hiển thị hồ sơ Creator đã lưu, không trộn với dự án của Creator. Từ hồ sơ công khai, dùng “Lưu hồ sơ Creator” để thêm hoặc bỏ hồ sơ; chọn một dòng trong danh sách để mở lại hồ sơ.
- **Công ty**: xem hồ sơ công ty và các thao tác chỉnh sửa đang ở chế độ xem trước.
- Trang hồ sơ Creator của Recruiter cung cấp lưu hồ sơ, nhắn tin và mời phỏng vấn. Creator không nhìn thấy nút lưu ứng viên hay nút mời phỏng vấn.

### Thanh điều hướng chính

- Radar, Khám phá/Creator, Tin báo, Đã lưu, Tin nhắn và Hồ sơ/Công ty dùng chung một layout trên cả hai vai trò.
- Tab đang mở được tô màu xanh; tab Hồ sơ chuyển đến hồ sơ Creator hoặc hồ sơ công ty theo vai trò đăng nhập.
- Các màn hình chi tiết và biểu mẫu là màn con; nút quay lại trở về màn trước.

## 3. Vị trí và quyền riêng tư

- Màn Quyền riêng tư vị trí có thao tác “Xác minh vị trí hiện tại”; app xin quyền Android khi cần và yêu cầu một vị trí mới từ LocationManager. Khi thành công, tọa độ và độ chính xác chỉ được hiển thị trên thiết bị của người dùng.
- Trạng thái GPS tắt, không có quyền hoặc không nhận được vị trí mới được thông báo riêng; người dùng có thể mở cài đặt vị trí và thử lại.
- Ba lựa chọn Exact/Area/Hidden là loại trừ lẫn nhau. Chạm lại lựa chọn đang chọn hoặc nhấn “Bỏ chọn / Ẩn vị trí” sẽ bỏ dấu chọn; khi áp dụng trạng thái không chọn được lưu an toàn thành Hidden.
- Chế độ vị trí được lưu cục bộ theo email trong SharedPreferences; hiện chưa đồng bộ sang `CreatorProfiles.LocationPrivacy` trên SQL Server. Việc xác minh GPS không tự chia sẻ tọa độ lên API.

## 4. Lưu dữ liệu và giới hạn đồng bộ

- Danh sách Creator đã lưu, dự án đã lưu và kết nối bạn bè được ghi vào `SharedPreferences`, tách theo email của tài khoản đang đăng nhập; dữ liệu tồn tại sau khi đóng/mở lại ứng dụng trên cùng thiết bị.
- Hai danh sách này chưa được đồng bộ lên SQL Server/API và không tự chia sẻ giữa các thiết bị.
- Bản đồ dùng hồ sơ/dự án mẫu. Creator xem một số dự án và Creator khác; Recruiter xem các hồ sơ Creator. Vị trí ghim nằm gần vị trí hiện tại nếu có, nếu không thì quanh tọa độ mặc định tại Đà Nẵng.
- Like, follow, bình luận, chat, đánh giá, ghim dự án, lọc và các thay đổi hồ sơ khác vẫn là dữ liệu mẫu/phiên xem trước, không nên hiểu là đã được lưu trên máy chủ.
- Đăng ký tài khoản đã được lưu vào SQL Server; chế độ quyền riêng tư vị trí và các dữ liệu tương tác khác vẫn lưu cục bộ hoặc chỉ là xem trước như mô tả ở trên.
- Đăng xuất đưa người dùng về màn đăng nhập. Danh sách cục bộ theo email không bị xóa khi đăng xuất.

## 5. Quy tắc quay lại

- Nút quay lại dùng lịch sử điều hướng trong ứng dụng.
- Từ màn đăng nhập gốc, nút Back của thiết bị đóng Activity; từ Home đã đăng nhập, Back cũng không đưa người dùng trở lại màn chọn vai trò.
- Đăng xuất là thao tác riêng, thực hiện từ Cài đặt và xác nhận trước khi quay về đăng nhập.
