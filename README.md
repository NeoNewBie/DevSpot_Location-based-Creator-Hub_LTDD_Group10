# DevSpot_Location-based-Creator-Hub_LTDD_Group10
Mạng xã hội Bản đồ Portfolio (Location-based Creator Hub). Sinh viên thả ghim (pin) các dự án cá nhân, game demo hoặc mã nguồn của mình tại vị trí hiện tại. Các công ty công nghệ tại Đà Nẵng có thể lướt bản đồ khu vực quanh các trường đại học để trực tiếp bấm vào xem và "săn" thực tập sinh. 

## Tài liệu

- [Tổng quan ứng dụng và rà soát giao diện](docs/APP_OVERVIEW.md)
- [Luồng giao diện Creator/Recruiter](docs/UI_LOGIC.md)
- [Schema SQL Server và dữ liệu mẫu Creator/Project/Map](database/DevSpot_CreatorHub.sql)

## Chạy API kết nối SQL Server

1. Tạo database mẫu bằng script trên nếu chưa có: `database/DevSpot_CreatorHub.sql`.
2. Cần .NET 10 SDK và SQL Server đang chạy. Cấu hình chuỗi kết nối trong `backend/DevSpot.Api/appsettings.Development.json` hoặc đặt biến môi trường `ConnectionStrings__DevSpot`; mặc định dùng Windows Integrated Security tới instance `localhost\MSSQLSERVER01`.
3. Chạy API từ thư mục gốc repo: ` --project backend/DevSpot.Api --launch-profile http`. Kiểm tra SQL Server tại `http://localhost:5080/api/health/db`.
4. Với Android Emulator, tạo đường hầm về máy tính bằng `adb reverse tcp:5080 tcp:5080`; app mặc định gọi `http://127.0.0.1:5080/`. Thiết bị thật cần build với URL API trên máy tính, ví dụ `-PdevspotApiBaseUrl=http://192.168.1.10:5080/`, đồng thời chạy API trên interface LAN (`dotnet run --project backend/DevSpot.Api --no-launch-profile -- --urls http://0.0.0.0:5080`) và cho phép cổng 5080 qua firewall.
5. Đăng nhập bằng một email trong `docs/APP_ACCOUNT.MD` và mật khẩu demo `DevSpotDemo@2026!`.

Android chỉ gọi API; chuỗi kết nối SQL Server được giữ ở backend, không đóng gói vào APK. Endpoint hiện được nối là đăng nhập; các luồng dữ liệu khác vẫn là bản xem trước. HTTP và Windows Integrated Security chỉ dùng cho môi trường phát triển cục bộ; triển khai thật cần HTTPS và cơ chế phiên/token được bảo vệ.
