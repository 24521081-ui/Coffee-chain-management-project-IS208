# CODEX IMPLEMENTATION TRACE - Inventory Report Dynamic Data

## 1. Mục tiêu thay đổi

Chuyển trang `Báo cáo > Tồn kho` từ dữ liệu tĩnh sang dữ liệu động lấy từ Oracle Database và reload toàn bộ theo filter. Trang thật đang được menu mở là `src/main/resources/com/phungloccoffee/gui/view/report/InventoryReport.fxml`, controller là `src/main/java/com/phungloccoffee/gui/controller/report/InventoryReportController.java`.

## 2. Các file đã đọc

- `src/main/resources/com/phungloccoffee/gui/view/report/InventoryReport.fxml`: giao diện báo cáo tồn kho.
- `src/main/java/com/phungloccoffee/gui/controller/report/InventoryReportController.java`: controller xử lý UI cũ.
- `src/main/java/com/phungloccoffee/gui/service/InventoryReportService.java`: service cũ đang tính summary/chart từ item list.
- `src/main/java/com/phungloccoffee/dao/InventoryReportDAO.java`: DAO tồn kho hiện có.
- `src/main/java/com/phungloccoffee/dao/ReportLookupDAO.java`: logic lấy danh sách chi nhánh và nhóm sản phẩm.
- `src/main/java/com/phungloccoffee/gui/service/ReportFilterUtils.java`: formatter ngày, tiền, số.
- `src/main/java/com/phungloccoffee/model/report/ReportModels.java`: model report cũ, đặc biệt `BranchOption`.
- `src/main/java/com/phungloccoffee/util/DBConnection.java`: kết nối Oracle.
- `src/main/java/com/phungloccoffee/gui/service/MenuConfig.java`: xác nhận `INVENTORY_REPORT` trỏ đến `/com/phungloccoffee/gui/view/report/InventoryReport.fxml`.
- `sql/02_tables.sql`: schema thật của `ton_kho`, `kho`, `chi_nhanh`, `san_pham`, `danh_muc_san_pham`.

## 3. Logic cũ trước khi sửa

- FXML có text/badge tĩnh như `Toàn hệ thống`, `Cần kiểm tra`, `Ưu tiên xử lý`, `+3%`.
- Controller gọi `InventoryReportService`, chưa đi theo tuyến `Controller -> BUS -> DAO`.
- Controller nhận dữ liệu từ service rồi tự dựng `InventoryRow`.
- Service cũ tự tính summary, branch chart, category chart từ list item.
- Trạng thái tồn kho dựa vào text string cũ trong service/controller.
- Query cũ chỉ trả item list, chưa có DAO riêng cho summary, cảnh báo theo chi nhánh, cơ cấu nhóm, ngày dữ liệu mới nhất.
- Controller load query trên JavaFX Application Thread.
- Filter thay đổi chưa auto reload toàn bộ trang.

## 4. Vấn đề phát hiện

- Dữ liệu tĩnh còn nằm trong FXML.
- Filter đổi nhưng chưa reload toàn bộ card/chart/table.
- Chart cảnh báo theo chi nhánh phụ thuộc dữ liệu đã lọc trong service, chưa có query riêng bảo đảm chi nhánh không cảnh báo vẫn có thể hiển thị 0.
- Bảng chưa clear dữ liệu cũ trước mỗi lần render.
- Filter ngày đang mặc định theo ngày máy qua `ReportFilterUtils.defaultFromDate/defaultToDate`; yêu cầu không chặn dữ liệu tương lai.
- Trang chưa lấy ngày dữ liệu mới nhất trong DB để set mặc định.
- Logic nghiệp vụ chưa nằm trong BUS.

## 5. Logic mới sau khi sửa

1. User mở trang `Báo cáo > Tồn kho`.
2. `InventoryReportController.initialize()` load filter mặc định.
3. Controller gọi `loadReport()`.
4. Controller tạo `InventoryReportFilter`.
5. Controller gọi `InventoryReportBUS.getInventoryReport(filter)` trong JavaFX `Task`.
6. BUS validate filter.
7. BUS gọi `InventoryReportDAO`.
8. DAO query Oracle.
9. BUS tính summary, trạng thái, biểu đồ, bảng.
10. Controller clear dữ liệu cũ.
11. Controller render lại toàn bộ UI: 4 card, bar chart, pie chart, danh sách cảnh báo, bảng.

## 6. Danh sách file đã sửa

### File: `src/main/resources/com/phungloccoffee/gui/view/report/InventoryReport.fxml`
- Lý do sửa: bỏ badge phần trăm tĩnh và chuẩn hóa text giao diện.
- Nội dung đã sửa: thêm `fx:id="inventoryGrowthBadge"` cho phần trăm tăng/giảm giá trị tồn kho; thêm `fx:id="categoryChartTitleLabel"` để đổi tiêu đề khi chọn nhóm cụ thể; giữ các `fx:id` cũ của card/chart/table.
- Class/hàm liên quan: `InventoryReportController.updateKpis`, `renderReport`.
- Ảnh hưởng đến module khác: chỉ ảnh hưởng trang `InventoryReport`.
- Ghi chú: không đổi route/menu.

### File: `src/main/java/com/phungloccoffee/gui/controller/report/InventoryReportController.java`
- Lý do sửa: đưa Controller về đúng vai trò UI.
- Nội dung đã sửa: thay `InventoryReportService` bằng `InventoryReportBUS`; tạo `InventoryReportFilter`; auto reload khi đổi từ ngày/đến ngày/chi nhánh/nhóm/trạng thái; reload thủ công qua nút `Xem báo cáo`; dùng JavaFX `Task`; clear dữ liệu cũ trước render.
- Class/hàm liên quan: `initialize`, `setupDefaultDates`, `setupBranchFilter`, `setupCategoryFilter`, `setupStatusFilter`, `loadReport`, `readFilter`, `renderReport`, `updateKpis`, `seedCharts`, `updateWarningList`.
- Ảnh hưởng đến module khác: không đổi controller khác.
- Ghi chú: biến `updatingControls` tránh reload lặp khi set filter mặc định.

### File: `src/main/java/com/phungloccoffee/bus/InventoryReportBUS.java`
- Lý do sửa: thêm lớp nghiệp vụ đúng kiến trúc.
- Nội dung đã sửa: load option chi nhánh/nhóm, lấy ngày dữ liệu mới nhất, validate filter, tính kỳ trước, tính summary, growth giá trị tồn kho, trạng thái tồn kho, map chart/table data.
- Class/hàm liên quan: `getInventoryReport`, `getDefaultReportDate`, `previousFilter`, `buildSummary`, `toItemReport`, `statusFromQuantity`, `growthPercent`.
- Ảnh hưởng đến module khác: lớp mới.
- Ghi chú: không dùng `LocalDate.now()` để chặn dữ liệu; default date lấy từ `MAX(ton_kho.last_updated)`.

### File: `src/main/java/com/phungloccoffee/dao/InventoryReportDAO.java`
- Lý do sửa: bổ sung query Oracle động cho InventoryReport.
- Nội dung đã sửa: thêm `findBranchOptions`, `findCategoryOptions`, `findLatestInventoryDate`, `findInventoryItems(InventoryReportFilter)`, `findBranchAlerts`, `findCategoryValues`; giữ method cũ để service cũ vẫn compile.
- Class/hàm liên quan: các method trên và record raw `InventoryItemRaw`, `InventoryBranchAlertRaw`, `InventoryCategoryValueRaw`.
- Ảnh hưởng đến module khác: method cũ `findInventoryItems(LocalDate, LocalDate, String, String, String)` vẫn được giữ.
- Ghi chú: dùng schema thật `ton_kho`, `kho`, `chi_nhanh`, `san_pham`, `danh_muc_san_pham`.

### File: `src/main/java/com/phungloccoffee/model/report/InventoryReportFilter.java`
- Lý do sửa: model hóa filter.
- Nội dung đã sửa: chứa `fromDate`, `toDate`, `branchId`, `categoryId`, `status`.
- Class/hàm liên quan: `InventoryReportFilter`.
- Ảnh hưởng đến module khác: lớp mới.
- Ghi chú: project dùng `String branchId/categoryId` vì schema dùng `VARCHAR2(20)`.

### File: `src/main/java/com/phungloccoffee/model/report/InventoryReportData.java`
- Lý do sửa: chứa toàn bộ dữ liệu Controller cần render.
- Nội dung đã sửa: summary, branch alert points, category value points, item reports, category chart title.
- Class/hàm liên quan: `InventoryReportData`.
- Ảnh hưởng đến module khác: lớp mới.

### File: `src/main/java/com/phungloccoffee/model/report/InventorySummary.java`
- Lý do sửa: chứa 4 card đầu trang và growth.
- Nội dung đã sửa: `totalTrackedProducts`, `alertBranchCount`, `lowStockItemCount`, `totalInventoryValue`, `growthPercent`, `growthLabel`.
- Class/hàm liên quan: `InventorySummary`.
- Ảnh hưởng đến module khác: lớp mới, không thay nested `ReportModels.InventorySummary`.

### File: `src/main/java/com/phungloccoffee/model/report/InventoryBranchAlertPoint.java`
- Lý do sửa: dữ liệu biểu đồ cảnh báo theo chi nhánh.
- Nội dung đã sửa: `branchId`, `branchName`, `chartLabel`, `alertCount`.
- Class/hàm liên quan: `InventoryBranchAlertPoint`.
- Ảnh hưởng đến module khác: lớp mới.

### File: `src/main/java/com/phungloccoffee/model/report/InventoryCategoryValuePoint.java`
- Lý do sửa: dữ liệu biểu đồ cơ cấu tồn kho theo nhóm.
- Nội dung đã sửa: `categoryId`, `categoryName`, `inventoryValue`.
- Class/hàm liên quan: `InventoryCategoryValuePoint`.
- Ảnh hưởng đến module khác: lớp mới.

### File: `src/main/java/com/phungloccoffee/model/report/InventoryItemReport.java`
- Lý do sửa: dữ liệu từng dòng bảng tồn kho nguyên liệu.
- Nội dung đã sửa: product, branch, category, quantity, min quantity, inventory value, status.
- Class/hàm liên quan: `InventoryItemReport`.
- Ảnh hưởng đến module khác: lớp mới.

### File: `src/main/java/com/phungloccoffee/model/report/InventoryStatus.java`
- Lý do sửa: enum trạng thái tồn kho.
- Nội dung đã sửa: `OUT_OF_STOCK`, `LOW_STOCK`, `STABLE`, map ra `Hết hàng`, `Tồn thấp`, `Ổn định`.
- Class/hàm liên quan: `InventoryStatus`.
- Ảnh hưởng đến module khác: lớp mới.

### File: `src/main/java/com/phungloccoffee/model/report/InventoryCategoryOption.java`
- Lý do sửa: option nhóm nguyên liệu có id thật.
- Nội dung đã sửa: `id`, `displayName`, `isAll`.
- Class/hàm liên quan: `InventoryCategoryOption`.
- Ảnh hưởng đến module khác: lớp mới.

## 7. Database/Table/Column liên quan

- `ton_kho.kho_id`: khóa kho, join sang `kho`.
- `ton_kho.san_pham_id`: sản phẩm/nguyên liệu tồn kho.
- `ton_kho.so_luong_ton`: quantity on hand.
- `ton_kho.muc_ton_toi_thieu`: min quantity/reorder point.
- `ton_kho.last_updated`: ngày dữ liệu tồn kho.
- `kho.kho_id`: khóa kho.
- `kho.chi_nhanh_id`: map kho về chi nhánh.
- `chi_nhanh.chi_nhanh_id`: mã chi nhánh.
- `chi_nhanh.ten_chi_nhanh`: tên chi nhánh.
- `chi_nhanh.trang_thai`: chỉ lấy chi nhánh hoạt động ở chart cảnh báo.
- `san_pham.san_pham_id`: mã sản phẩm/nguyên liệu.
- `san_pham.ten_san_pham`: tên nguyên liệu/sản phẩm.
- `san_pham.danh_muc_id`: nhóm nguyên liệu/sản phẩm.
- `san_pham.gia_von`: giá vốn dùng tính giá trị tồn.
- `danh_muc_san_pham.danh_muc_id`: mã nhóm.
- `danh_muc_san_pham.ten_danh_muc`: tên nhóm.

Không thay đổi cấu trúc Database.

## 8. SQL/DAO chính đã thêm hoặc sửa

- Query tổng sản phẩm theo dõi: DAO trả item list theo filter; BUS đếm `COUNT(DISTINCT san_pham_id)` nếu tất cả chi nhánh, hoặc số dòng tồn kho nếu một chi nhánh.
- Query số chi nhánh có cảnh báo: `findBranchAlerts`, BUS đếm branch có `alertCount > 0`.
- Query số mặt hàng tồn thấp: BUS đếm item có `so_luong_ton <= muc_ton_toi_thieu`.
- Query tổng giá trị tồn kho: DAO tính từng dòng `so_luong_ton * san_pham.gia_von`, BUS cộng tổng.
- Query giá trị tồn kho kỳ trước: BUS tạo filter kỳ trước và gọi lại `findInventoryItems`.
- Query cảnh báo tồn kho theo chi nhánh: `findBranchAlerts`, `chi_nhanh LEFT JOIN kho LEFT JOIN ton_kho`, `SUM(CASE WHEN so_luong_ton <= muc_ton_toi_thieu THEN 1 ELSE 0 END)`.
- Query cơ cấu tồn kho theo nhóm: `findCategoryValues`, `SUM(ton_kho.so_luong_ton * san_pham.gia_von) GROUP BY danh_muc`.
- Query bảng tồn kho nguyên liệu: `findInventoryItems(InventoryReportFilter)`.
- Query ngày dữ liệu mới nhất: `SELECT MAX(last_updated) FROM ton_kho`.

## 9. Logic filter

- `branchId = null`: tất cả chi nhánh.
- `categoryId = null`: tất cả nhóm.
- `status = null`: tất cả trạng thái.
- `fromDate/toDate` không bị chặn bởi ngày hiện tại của máy.
- Nếu DB có dữ liệu tương lai trong `ton_kho.last_updated`, controller vẫn thống kê được khi filter bao phủ ngày đó.
- Default date lấy theo `MAX(ton_kho.last_updated)`, không lấy trực tiếp `LocalDate.now()` trừ fallback khi DB lỗi.
- Filter ngày áp dụng vào `ton_kho.last_updated >= fromDate` và `< toDate + 1`.

## 10. Logic trạng thái tồn kho

- `so_luong_ton <= 0`: `Hết hàng`.
- `so_luong_ton > 0 AND so_luong_ton <= muc_ton_toi_thieu`: `Tồn thấp`.
- `so_luong_ton > muc_ton_toi_thieu`: `Ổn định`.

Filter trạng thái dùng enum `InventoryStatus`, DAO lọc theo điều kiện số lượng thật, Controller chỉ render enum.

## 11. Logic biểu đồ

Biểu đồ cảnh báo theo chi nhánh:
- X = Chi nhánh.
- Y = Số cảnh báo tồn kho.
- Mỗi cột là một chi nhánh.
- Nếu tất cả chi nhánh, query dùng LEFT JOIN để chi nhánh không cảnh báo có thể hiển thị 0.
- Nếu chọn một chi nhánh, chỉ còn một cột của chi nhánh đó.

Biểu đồ cơ cấu tồn kho theo nhóm:
- Group theo `danh_muc_san_pham`.
- Giá trị = `SUM(ton_kho.so_luong_ton * san_pham.gia_von)`.
- Nếu chọn một nhóm cụ thể, title đổi thành `Giá trị tồn kho nhóm đang chọn`.

## 12. Cách test thủ công

1. Mở ứng dụng.
2. Đăng nhập bằng tài khoản Ban giám đốc.
3. Vào trang `Báo cáo > Tồn kho`.
4. Kiểm tra dữ liệu mặc định được load từ Database.
5. Đổi từ ngày/đến ngày.
6. Đổi chi nhánh.
7. Đổi nhóm nguyên liệu.
8. Đổi trạng thái.
9. Bấm `Xem báo cáo`.
10. Kiểm tra 4 card đầu trang thay đổi.
11. Kiểm tra biểu đồ cảnh báo theo chi nhánh đúng trục X = chi nhánh, Y = số cảnh báo.
12. Kiểm tra biểu đồ cơ cấu tồn kho theo nhóm thay đổi.
13. Kiểm tra bảng nguyên liệu reload đúng và không giữ dữ liệu cũ.
14. Kiểm tra trường hợp filter không có dữ liệu.
15. Kiểm tra trường hợp Database có dữ liệu tháng 6 dù hôm nay là tháng 5: chọn filter tháng 6 vẫn thống kê được nếu `ton_kho.last_updated` nằm trong tháng 6.

## 13. Kết quả mong đợi

- Trang không còn hard-code số liệu tồn kho.
- Tất cả card, chart, table reload theo cùng `InventoryReportFilter`.
- Không bị giới hạn bởi ngày hiện tại của máy.
- Biểu đồ cảnh báo tồn kho đúng X = chi nhánh, Y = số cảnh báo.
- Biểu đồ cơ cấu nhóm lấy từ `SUM(so_luong_ton * gia_von)`.
- Bảng nguyên liệu không giữ dữ liệu cũ.
- Không crash khi không có dữ liệu.

## 14. Rủi ro còn lại

- Schema hiện tại chỉ có bảng tồn kho hiện tại `ton_kho`, không có bảng snapshot/lịch sử tồn kho theo ngày. Vì vậy filter ngày dùng `ton_kho.last_updated`; đây không phải lịch sử tồn kho đầy đủ.
- Giá trị tồn kho dùng `san_pham.gia_von`; cần xác nhận đây là giá vốn mong muốn cho báo cáo.
- Nếu muốn báo cáo tồn kho đúng theo từng ngày quá khứ, cần thêm bảng lịch sử/snapshot tồn kho hoặc bảng giao dịch tồn kho và cập nhật DAO.
- Method cũ trong `InventoryReportDAO` vẫn tồn tại cho `InventoryReportService`; nếu sau này bỏ service cũ có thể dọn sau.

## 15. Hướng dẫn cho Codex khác

- Muốn hiểu trang này thì đọc `InventoryReportController.java` trước.
- Logic chính nằm ở `InventoryReportBUS.java`.
- Query nằm ở `InventoryReportDAO.java`, tập trung các method `findInventoryItems(InventoryReportFilter)`, `findBranchAlerts`, `findCategoryValues`, `findLatestInventoryDate`.
- Model dữ liệu nằm ở `src/main/java/com/phungloccoffee/model/report`: `InventoryReportFilter`, `InventoryReportData`, `InventorySummary`, `InventoryBranchAlertPoint`, `InventoryCategoryValuePoint`, `InventoryItemReport`, `InventoryStatus`, `InventoryCategoryOption`.
- Không đưa SQL vào Controller.
- Không xóa `updatingControls`, vì nó tránh reload lặp khi set filter mặc định.
- Nếu muốn thêm xuất PDF/Excel sau này, mở rộng controller hoặc thêm exporter/service riêng.
- Nếu muốn thêm trạng thái mới như `Tồn dư`, sửa `InventoryStatus`, `InventoryReportBUS.statusFromQuantity`, filter SQL trong `InventoryReportDAO.bindFilter`/query condition, và `StatusCell`.

## 16. Kiểm tra đã chạy

- Đã chạy `mvn -q -DskipTests compile`.
- Kết quả: compile thành công.
