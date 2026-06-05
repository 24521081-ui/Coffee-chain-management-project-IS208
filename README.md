<p align="center">
  <a href="https://www.uit.edu.vn/" title="Trường Đại học Công nghệ Thông tin" style="border: 5;">
    <img src="https://i.imgur.com/WmMnSRt.png" alt="Trường Đại học Công nghệ Thông tin | University of Information Technology">
  </a>
</p>

<!-- Title -->
<h1 align="center"><b>IS208 - QUẢN LÝ DỰ ÁN CNTT</b></h1>

## BẢNG MỤC LỤC
* [ Giới thiệu môn học](#gioithieumonhoc)
* [ Giảng viên hướng dẫn](#giangvien)
* [ Thành viên nhóm](#thanhvien)
* [ Đồ án môn học](#doan)
* [ Liên kết Repository](#repository)
* [ Yêu cầu hệ thống](#yeucau)
* [ Hướng dẫn cài đặt & Cấu hình](#caidat)
* [ Cấu hình Cơ sở dữ liệu (Oracle Database)](#database)
* [ Hướng dẫn chạy ứng dụng](#khoidong)

## GIỚI THIỆU MÔN HỌC
<a name="gioithieumonhoc"></a>
* **Tên môn học**: Quản lý dự án CNTT - Information Technology Project Management
* **Mã môn học**: IS208
* **Lớp học**: IS208.Q21
* **Năm học**: 2025-2026

## GIẢNG VIÊN HƯỚNG DẪN
<a name="giangvien"></a>
* ThS. **Tạ Việt Phương** - *phuongtv@uit.edu.vn*

## THÀNH VIÊN NHÓM
<a name="thanhvien"></a>
| STT    | MSSV          | Họ và Tên              | Github                                               | Email                   |
| ------ |:-------------:| ----------------------:|-----------------------------------------------------:|-------------------------:|
| 1      | 24520978      | Dương Đức Lộc     |[DucLoc](https://github.com/DucLoc0802)                 |24520978@gm.uit.edu.vn   |
| 2      | 24521034      | Châu Gia Lương   |[GiaLuong](https://github.com/24520134GiaLuong)         |24521034@gm.uit.edu.vn   |
| 3      | 24521045      | Trần Đức Mạnh     |[DucManh](https://github.com/0814174177)                 |24521045@gm.uit.edu.vn   |
| 4      | 24521081      | Nguyễn Văn Minh           |[VanMinh](https://github.com/24521081-ui)   |24521081@gm.uit.edu.vn   |
| 5      | 24521093      | Nguyễn Thế Mỹ           |[TheMy](https://github.com/themy130806-eng)   |24521093@gm.uit.edu.vn   |

## ĐỒ ÁN MÔN HỌC
<a name="doan"></a>
* **Đồ án Nhóm**: Hệ thống quản lý chuỗi cửa hàng Phụng Lộc Coffee (PhungLocCoffeeApp)
* **Công nghệ sử dụng**:
  - Ngôn ngữ: Java 21 (JDK 21)
  - Framework giao diện: JavaFX 21 (với FXML)
  - Hệ quản trị cơ sở dữ liệu: Oracle Database (OJDB11)
  - Quản lý thư viện & Build: Maven

## LIÊN KẾT REPOSITORY
<a name="repository"></a>
* **Repository URL**: [https://github.com/24521081-ui/Coffee-chain-management-project-IS208](https://github.com/24521081-ui/Coffee-chain-management-project-IS208)
* **Git Clone Command**:
  ```bash
  git clone https://github.com/24521081-ui/Coffee-chain-management-project-IS208.git
  ```

## YÊU CẦU HỆ THỐNG
<a name="yeucau"></a>
Trước khi cài đặt và chạy dự án, máy tính cần đáp ứng các yêu cầu tối thiểu sau:
- **Hệ điều hành**: Windows 10/11, macOS, hoặc Linux (Khuyên dùng Windows để tương thích tốt nhất với các script tự động đi kèm).
- **Java Development Kit (JDK)**: Phiên bản **21** trở lên.
- **Cơ sở dữ liệu**: Oracle Database 19c/21c/23ai (hoặc phiên bản Oracle Database Free/Express Edition cài đặt local).
- **Công cụ hỗ trợ khác**: Git, SQL Developer (hoặc các tool quản trị DB tương tự như DBeaver).

## HƯỚNG DẪN CÀI ĐẶT & CẤU HÌNH
<a name="caidat"></a>
### Bước 1: Clone dự án về máy
Mở Terminal/Command Prompt và chạy lệnh sau:
```bash
git clone https://github.com/24521081-ui/Coffee-chain-management-project-IS208.git
cd Coffee-chain-management-project-IS208
```

### Bước 2: Cấu hình môi trường ứng dụng
Trong thư mục gốc của dự án, chúng em đã chuẩn bị sẵn tệp cấu hình mẫu là `config.example.properties` và tệp cấu hình thực tế `config.properties`.
Để cấu hình các thông số kết nối cơ sở dữ liệu Oracle phù hợp với hệ thống, thầy có thể mở tệp `config.properties` và kiểm tra hoặc chỉnh sửa thông số:
```properties
db.username=PL_COFFEE
db.password=123456

# Các thông số dưới đây mặc định, chỉ thay đổi nếu sử dụng thông số khác:
db.host=localhost
db.port=1521
db.service=freepdb1
```

## CẤU HÌNH CƠ SỞ DỮ LIỆU (ORACLE DATABASE)
<a name="database"></a>
Ứng dụng sử dụng Oracle Database để lưu trữ dữ liệu. Thầy có thể thực hiện theo các bước dưới đây để khởi tạo và nạp dữ liệu mẫu.

### Bước 1: Tạo User/Schema mới (Nếu chưa có)
Kết nối vào cơ sở dữ liệu Oracle bằng tài khoản quản trị cao cấp (như `SYSTEM` hoặc `SYSDBA`) và chạy lệnh tạo user `PL_COFFEE` cùng cấp các quyền cần thiết:
```sql
-- Đăng nhập bằng tài khoản SYSTEM / SYSDBA và chạy:
CREATE USER PL_COFFEE IDENTIFIED BY 123456;
GRANT CONNECT, RESOURCE, CREATE VIEW, UNLIMITED TABLESPACE TO PL_COFFEE;
```
*(Mật khẩu mặc định được cấu hình sẵn là `123456` để khớp hoàn toàn với cấu hình mặc định trong tệp `config.properties`).*

### Bước 2: Chạy các tập lệnh SQL tạo bảng và nạp dữ liệu
Kết nối vào cơ sở dữ liệu Oracle bằng tài khoản `PL_COFFEE` vừa tạo (thông qua SQL Developer, SQLPlus hoặc DBeaver). Mở thư mục [sql/](./sql) và chạy các script SQL theo đúng thứ tự sau:

1. [sql/01_drop_objects.sql](./sql/01_drop_objects.sql) - *Xóa các bảng cũ tránh xung đột nếu có.*
2. [sql/02_tables.sql](./sql/02_tables.sql) - *Tạo cấu trúc bảng mới.*
3. [sql/06_sample_data.sql](./sql/06_sample_data.sql) - *Nạp dữ liệu mẫu phục vụ kiểm thử.*

> **Lưu ý:** Các tệp `03_functions.sql`, `04_triggers.sql`, `05_procedures.sql` hiện tại còn trống, chúng em sẽ bổ sung sau.

### Bước 3: Kiểm tra kết nối cơ sở dữ liệu từ ứng dụng
Để đảm bảo thông tin cấu hình trong `config.properties` và cơ sở dữ liệu hoạt động chính xác, thầy có thể chạy thử tập lệnh kiểm tra kết nối:
- Trên Windows: Nháy đúp vào tệp [test-db.bat](./test-db.bat) trong thư mục gốc.
- Chương trình sẽ biên dịch một tệp Java nhỏ và thực hiện kết nối tới database để kiểm tra. Nếu thành công, màn hình console sẽ hiển thị thông báo kết nối thành công.

---

## HƯỚNG DẪN CHẠY ỨNG DỤNG
<a name="khoidong"></a>
Thầy có thể khởi động ứng dụng JavaFX theo hai cách sau:

### Cách 1: Sử dụng Script tự động (dùng cho Windows)
Dự án đi kèm tệp [run.bat](./run.bat) để hỗ trợ thiết lập môi trường nhanh chóng:
1. Nháy đúp vào tệp [run.bat](./run.bat) tại thư mục gốc của dự án.
2. Script sẽ tự động thực hiện các thao tác:
   - Kiểm tra xem máy đã cài Java chưa. Nếu chưa cài, hệ thống sẽ tự động tải và cài đặt **Java JDK 21** một cách an toàn thông qua Windows Package Manager (`winget`).
   - Tự động cấu hình biến môi trường `JAVA_HOME` tạm thời cho phiên làm việc hiện tại để tránh các lỗi biên dịch của Maven.
   - Biên dịch dự án và chạy ứng dụng JavaFX.

### Cách 2: Sử dụng dòng lệnh 
Nếu sử dụng hệ điều hành khác hoặc muốn chạy trực tiếp bằng Terminal, thầy có thể dùng lệnh Maven Wrapper được đi kèm dự án:
```bash
# Trên Windows:
.\mvnw.cmd clean javafx:run

# Trên macOS/Linux:
chmod +x mvnw
./mvnw clean javafx:run
```

### Tài khoản đăng nhập kiểm thử (Mặc định)
Sau khi ứng dụng khởi động thành công, thầy có thể đăng nhập vào hệ thống quản lý bằng tài khoản mẫu đã được chuẩn bị sẵn trong cơ sở dữ liệu:
* **Tài khoản**: `admin`
* **Mật khẩu**: `123456`
