# CODEX IMPLEMENTATION TRACE - Git Conflict Resolution

## 1. Mục tiêu
Resolve Git conflict để branch có thể merge/push.

## 2. Nguyên nhân conflict
- Có nhiều file conflict trong `target/classes`.
- `target/classes` là build output của Maven/Java, không nên được Git tracking.
- File source/script thật được người dùng nêu là `sql/erd_drc_objects.sql`; trong checkout local hiện tại file này không tồn tại và không xuất hiện trong `git status`, nên không có conflict local để merge thủ công.

## 3. Các lệnh Git đã chạy
- `git status --short --branch`
- `git status`
- `git branch --list backup-before-resolve-conflict*`
- `Test-Path sql\erd_drc_objects.sql`
- `git branch backup-before-resolve-conflict`
- `git ls-files target | Measure-Object`
- `git ls-files sql`
- `git rm -r --cached target`
- `git status --short --branch`
- `rg -n "^<<<<<<< |^=======$|^>>>>>>> " --glob "!target/**" --glob "!.git/**" .`
- `git diff --name-only -- src\main\resources\com\phungloccoffee\gui\view\PaymentScreen.fxml src\main\resources\com\phungloccoffee\gui\css\topbar.css src\main\java\com\phungloccoffee\bus\CustomerBUS.java src\main\java\com\phungloccoffee\dao\OrderDAO.java`
- `git diff --cached --stat`
- `mvn clean compile`
- `git add .gitignore`
- `git add CODEX_IMPLEMENTATION_TRACE.md`
- `git add -u`
- `git status`
- `git commit -m "Resolve merge conflicts and stop tracking build artifacts"`
- `git push`

## 4. File đã sửa

### .gitignore
- Thêm `target/`
- Thêm `*.class`
- Thêm `.idea/`, `*.iml`, `.DS_Store`

### sql/erd_drc_objects.sql
- File này không tồn tại trong working tree local hiện tại.
- Không có conflict marker local để resolve.
- Không tự tạo file SQL giả để tránh phá schema hoặc thêm object không kiểm chứng.

## 5. File không sửa trực tiếp
- Không sửa tay các file `.class` trong `target/classes`.
- Đã bỏ tracking `target/` vì đây là build output.
- Không sửa trực tiếp bản copy resource trong `target/classes`.
- Đã kiểm tra source tương ứng như `src/main/resources/com/phungloccoffee/gui/view/PaymentScreen.fxml`, `src/main/resources/com/phungloccoffee/gui/css/topbar.css`, `src/main/java/com/phungloccoffee/bus/CustomerBUS.java`, `src/main/java/com/phungloccoffee/dao/OrderDAO.java`; không có diff/conflict ở các file này tại thời điểm xử lý.

## 6. Kết quả kiểm tra
- `git status` ban đầu: không ở trạng thái merge conflict, branch `Manh` sạch và ngang `origin/Manh`.
- Sau `git rm -r --cached target`: các file trong `target/` được staged dạng delete khỏi index.
- Kiểm tra conflict marker thật bằng `rg`: không tìm thấy marker `<<<<<<<`, `=======`, `>>>>>>>` trong source ngoài `target/`.
- `mvn clean compile`: thành công (`BUILD SUCCESS`).

## 7. Hướng dẫn cho Codex khác
- Không commit lại `target/`.
- Nếu conflict quay lại ở `target/classes` thì xử lý bằng cách bỏ tracking `target/`, không merge `.class`.
- Nếu conflict nằm ở source thật thì phải merge thủ công sau khi đọc nội dung file.
- Không được force push nếu chưa được người dùng cho phép.
