package com.phungloccoffee.gui.controller.branch;

import com.phungloccoffee.gui.service.BranchManagementService;
import com.phungloccoffee.model.branch.BranchManagementModels.BranchData;
import com.phungloccoffee.util.AlertUtils;
import com.phungloccoffee.util.AutoCodeGenerator;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class BranchManagementController {
    private static final String STATUS_ACTIVE = "Đang hoạt động";
    private static final String STATUS_PAUSED = "Tạm dừng";
    private static final String ALL_AREAS = "Tất cả khu vực";
    private static final String ALL_STATUSES = "Tất cả trạng thái";
    private static final String PERIOD_DAY = "Theo ngày";
    private static final String PERIOD_MONTH = "Theo tháng";
    private static final String PERIOD_YEAR = "Theo năm";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");

    @FXML private TextField searchField;
    @FXML private ComboBox<String> areaFilterComboBox;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private ComboBox<String> periodTypeComboBox;
    @FXML private DatePicker periodDatePicker;
    @FXML private Label periodScopeLabel;
    @FXML private Label totalBranchLabel;
    @FXML private Label activeBranchLabel;
    @FXML private Label pausedBranchLabel;
    @FXML private TableView<BranchRow> branchTable;
    @FXML private TableColumn<BranchRow, String> branchCodeColumn;
    @FXML private TableColumn<BranchRow, String> branchNameColumn;
    @FXML private TableColumn<BranchRow, String> areaColumn;
    @FXML private TableColumn<BranchRow, String> managerColumn;
    @FXML private TableColumn<BranchRow, String> phoneColumn;
    @FXML private TableColumn<BranchRow, String> statusColumn;
    @FXML private TableColumn<BranchRow, Void> actionColumn;

    private final BranchManagementService branchManagementService = new BranchManagementService();
    private final ObservableList<BranchRow> branches = FXCollections.observableArrayList();
    private FilteredList<BranchRow> filteredBranches;
    private String activePeriodType = PERIOD_MONTH;
    private LocalDate activePeriodDate = LocalDate.now();

    @FXML
    private void initialize() {
        setupTable();
        loadBranches();
        setupFilters();
        updateStats();
    }

    private void setupTable() {
        branchCodeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        branchNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        areaColumn.setCellValueFactory(new PropertyValueFactory<>("area"));
        managerColumn.setCellValueFactory(new PropertyValueFactory<>("manager"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        statusColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(getStatusForPeriod(data.getValue())));
        statusColumn.setCellFactory(column -> new StatusCell<>());
        actionColumn.setCellFactory(column -> new ActionCell());
        filteredBranches = new FilteredList<>(branches, branch -> true);
        branchTable.setItems(filteredBranches);
        branchTable.setPlaceholder(new Label("Không có chi nhánh phù hợp với bộ lọc đã chọn"));
        branchTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupFilters() {
        updateAreaFilterOptions();
        statusFilterComboBox.getItems().setAll(ALL_STATUSES, STATUS_ACTIVE, STATUS_PAUSED);
        statusFilterComboBox.getSelectionModel().selectFirst();
        setupPeriodFilter();
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        areaFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        statusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void setupPeriodFilter() {
        periodTypeComboBox.getItems().setAll(PERIOD_DAY, PERIOD_MONTH, PERIOD_YEAR);
        periodTypeComboBox.getSelectionModel().select(PERIOD_MONTH);
        periodDatePicker.setValue(LocalDate.now());
        activePeriodType = PERIOD_MONTH;
        activePeriodDate = periodDatePicker.getValue();
        updatePeriodScopeLabel();
        periodTypeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyTimeFilter());
        periodDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> applyTimeFilter());
    }

    @FXML
    private void applyTimeFilter() {
        activePeriodType = selectedPeriodType();
        activePeriodDate = selectedPeriodDate();
        updatePeriodScopeLabel();
        loadBranches();
        updateAreaFilterOptions();
        applyFilters();
    }

    private void loadBranches() {
        loadBranchesFromDatabase();
    }

    private boolean loadBranchesFromDatabase() {
        try {
            PeriodRange range = activePeriodRange();
            branches.setAll(branchManagementService.loadBranches(range.start(), range.end()).stream()
                    .map(this::toBranchRow)
                    .toList());
        } catch (Exception e) {
            branches.clear();
            AlertUtils.showError("Không thể tải chi nhánh từ cơ sở dữ liệu. Vui lòng kiểm tra cấu hình database.");
            e.printStackTrace();
        }
        return true;
    }

    private BranchRow toBranchRow(BranchData branch) {
        return new BranchRow(
                branch.code(),
                branch.name(),
                branch.area(),
                branch.address(),
                branch.phone(),
                branch.manager(),
                branch.managerPhone(),
                branch.managerEmail(),
                branch.openingDate(),
                statusLabel(branch.status()),
                branch.statusChangedDate(),
                branch.note(),
                branch.employeeCount(),
                branch.servingProductCount(),
                branch.orderCount(),
                branch.revenue(),
                branch.ingredientCount()
        );
    }

    private void updateAreaFilterOptions() {
        String selectedArea = areaFilterComboBox.getValue();
        LinkedHashSet<String> areas = new LinkedHashSet<>();
        areas.add(ALL_AREAS);
        branches.stream()
                .map(BranchRow::getArea)
                .filter(area -> !safe(area).isEmpty())
                .sorted(Comparator.naturalOrder())
                .forEach(areas::add);
        areaFilterComboBox.getItems().setAll(areas);
        if (selectedArea != null && areas.contains(selectedArea)) {
            areaFilterComboBox.getSelectionModel().select(selectedArea);
        } else {
            areaFilterComboBox.getSelectionModel().selectFirst();
        }
    }

    private void applyFilters() {
        String keyword = normalize(searchField.getText());
        String selectedArea = areaFilterComboBox.getValue();
        String selectedStatus = statusFilterComboBox.getValue();
        PeriodRange periodRange = activePeriodRange();
        filteredBranches.setPredicate(branch -> {
            boolean matchesKeyword = keyword.isBlank()
                    || normalize(branch.getCode()).contains(keyword)
                    || normalize(branch.getName()).contains(keyword)
                    || normalize(branch.getArea()).contains(keyword)
                    || normalize(branch.getManager()).contains(keyword)
                    || normalize(branch.getPhone()).contains(keyword);
            boolean matchesArea = selectedArea == null || ALL_AREAS.equals(selectedArea) || selectedArea.equals(branch.getArea());
            boolean matchesStatus = selectedStatus == null || ALL_STATUSES.equals(selectedStatus) || selectedStatus.equals(getStatusForPeriod(branch));
            boolean matchesPeriod = isBranchVisibleInPeriod(branch, periodRange);
            return matchesKeyword && matchesArea && matchesStatus && matchesPeriod;
        });
        branchTable.refresh();
        updateStats();
    }

    private void updateStats() {
        List<BranchRow> rows = filteredBranches == null ? branches : filteredBranches;
        totalBranchLabel.setText(String.valueOf(rows.size()));
        activeBranchLabel.setText(String.valueOf(rows.stream().filter(branch -> STATUS_ACTIVE.equals(getStatusForPeriod(branch))).count()));
        pausedBranchLabel.setText(String.valueOf(rows.stream().filter(branch -> STATUS_PAUSED.equals(getStatusForPeriod(branch))).count()));
    }

    private String selectedPeriodType() {
        String periodType = periodTypeComboBox.getValue();
        return safe(periodType).isEmpty() ? PERIOD_MONTH : periodType;
    }

    private LocalDate selectedPeriodDate() {
        LocalDate periodDate = periodDatePicker.getValue();
        return periodDate == null ? LocalDate.now() : periodDate;
    }

    private void updatePeriodScopeLabel() {
        periodDatePicker.setPromptText(switch (activePeriodType) {
            case PERIOD_DAY -> "Chọn ngày";
            case PERIOD_YEAR -> "Chọn năm";
            default -> "Chọn tháng/năm";
        });
        periodScopeLabel.setText("Kỳ dữ liệu: " + formatPeriodLabel(activePeriodType, activePeriodDate));
    }

    private String formatPeriodLabel(String periodType, LocalDate periodDate) {
        LocalDate date = periodDate == null ? LocalDate.now() : periodDate;
        return switch (periodType) {
            case PERIOD_DAY -> "Theo ngày " + date.format(DATE_FORMATTER);
            case PERIOD_YEAR -> "Năm " + date.getYear();
            default -> "Tháng " + date.format(MONTH_FORMATTER);
        };
    }

    private PeriodRange activePeriodRange() {
        LocalDate date = activePeriodDate == null ? LocalDate.now() : activePeriodDate;
        return switch (activePeriodType) {
            case PERIOD_DAY -> new PeriodRange(date, date);
            case PERIOD_YEAR -> new PeriodRange(date.withDayOfYear(1), date.withDayOfYear(date.lengthOfYear()));
            default -> new PeriodRange(date.withDayOfMonth(1), date.withDayOfMonth(date.lengthOfMonth()));
        };
    }

    private boolean isBranchVisibleInPeriod(BranchRow branch, PeriodRange periodRange) {
        return branch.getOpeningDate() == null || !branch.getOpeningDate().isAfter(periodRange.end());
    }

    private String getStatusForPeriod(BranchRow branch) {
        return getStatusForPeriod(branch, activePeriodRange().end());
    }

    private String getStatusForPeriod(BranchRow branch, LocalDate periodEnd) {
        if (branch == null) {
            return "";
        }
        LocalDate statusChangedDate = branch.getStatusChangedDate();
        if (STATUS_PAUSED.equals(branch.getStatus())
                && statusChangedDate != null
                && periodEnd.isBefore(statusChangedDate)) {
            return STATUS_ACTIVE;
        }
        return branch.getStatus();
    }

    private BranchOperationStats operationStatsForPeriod(BranchRow branch) {
        BigDecimal revenue = branch.getMonthRevenue() == null ? BigDecimal.ZERO : branch.getMonthRevenue();
        return new BranchOperationStats(
                branch.getOrderCount(),
                revenue,
                branch.getServingProductCount(),
                branch.getIngredientCount()
        );
    }

    @FXML
    private void showAddBranchDialog() {
        BranchFormFields fields = createBranchForm(null);
        Dialog<Boolean> dialog = createBranchFormDialog("Thêm chi nhánh", "Lưu chi nhánh", fields);
        ButtonType saveType = dialog.getDialogPane().getButtonTypes().get(0);
        Node saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            BranchRow branch = readBranchForm(fields, null);
            if (branch == null) {
                event.consume();
                return;
            }
            if (!saveBranchToDatabase(branch)) {
                event.consume();
                return;
            }
            loadBranches();
            updateAreaFilterOptions();
            applyFilters();
            updateStats();
            selectBranchByCode(branch.getCode());
            AlertUtils.showInfo("Thêm chi nhánh thành công");
        });
        dialog.showAndWait();
    }

    private void showEditBranchDialog(BranchRow branch) {
        BranchFormFields fields = createBranchForm(branch);
        Dialog<Boolean> dialog = createBranchFormDialog("Cập nhật chi nhánh", "Lưu cập nhật", fields);
        ButtonType saveType = dialog.getDialogPane().getButtonTypes().get(0);
        Node saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            BranchRow updatedBranch = readBranchForm(fields, branch);
            if (updatedBranch == null) {
                event.consume();
                return;
            }
            if (!saveBranchToDatabase(updatedBranch)) {
                event.consume();
                return;
            }
            loadBranches();
            updateAreaFilterOptions();
            applyFilters();
            updateStats();
            selectBranchByCode(updatedBranch.getCode());
            AlertUtils.showInfo("Cập nhật chi nhánh thành công");
        });
        dialog.showAndWait();
    }

    private boolean saveBranchToDatabase(BranchRow branch) {
        try {
            branchManagementService.saveBranch(toBranchData(branch));
            return true;
        } catch (Exception e) {
            AlertUtils.showError(readableError(e));
            e.printStackTrace();
            return false;
        }
    }

    private BranchData toBranchData(BranchRow branch) {
        return new BranchData(
                branch.getCode(),
                branch.getName(),
                branch.getArea(),
                branch.getAddress(),
                branch.getPhone(),
                branch.getManager(),
                branch.getManagerPhone(),
                branch.getManagerEmail(),
                branch.getOpeningDate(),
                statusToDb(branch.getStatus()),
                branch.getStatusChangedDate(),
                branch.getNote(),
                branch.getEmployeeCount(),
                branch.getServingProductCount(),
                branch.getOrderCount(),
                branch.getMonthRevenue(),
                branch.getIngredientCount()
        );
    }

    private void selectBranchByCode(String code) {
        for (BranchRow row : filteredBranches) {
            if (row.getCode().equalsIgnoreCase(code)) {
                branchTable.getSelectionModel().select(row);
                branchTable.scrollTo(row);
                return;
            }
        }
    }

    private Dialog<Boolean> createBranchFormDialog(String title, String saveText, BranchFormFields fields) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(title);
        ButtonType saveType = new ButtonType(saveText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, cancelType);
        dialog.getDialogPane().setPrefWidth(760);
        attachDialogStyles(dialog);
        dialog.getDialogPane().setContent(fields.container());
        dialog.getDialogPane().lookupButton(saveType).getStyleClass().add("primary-action-button");
        dialog.getDialogPane().lookupButton(cancelType).getStyleClass().add("secondary-action-button");
        dialog.setResultConverter(button -> button != null && button.getButtonData() == ButtonBar.ButtonData.OK_DONE);
        return dialog;
    }

    private BranchFormFields createBranchForm(BranchRow branch) {
        boolean updateMode = branch != null;
        TextField codeField = textField(updateMode ? branch.getCode() : generateNextBranchCode(), "Mã chi nhánh");
        codeField.setEditable(false);
        codeField.getStyleClass().add("readonly-code-field");
        TextField nameField = textField(updateMode ? branch.getName() : "", "Tên chi nhánh");
        TextField areaField = textField(updateMode ? branch.getArea() : "", "Khu vực");
        TextField addressField = textField(updateMode ? branch.getAddress() : "", "Địa chỉ");
        TextField phoneField = textField(updateMode ? branch.getPhone() : "", "Số điện thoại");
        TextField managerField = textField(updateMode ? branch.getManager() : "", "Người quản lý");
        areaField.setText(updateMode ? branch.getArea() : deriveAreaFromAddress(addressField.getText()));
        areaField.setEditable(false);
        areaField.getStyleClass().add("readonly-code-field");
        addressField.textProperty().addListener((observable, oldValue, newValue) ->
                areaField.setText(deriveAreaFromAddress(newValue)));

        DatePicker openingDatePicker = new DatePicker(updateMode ? branch.getOpeningDate() : LocalDate.now());
        openingDatePicker.getStyleClass().add("page-date");
        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList(STATUS_ACTIVE, STATUS_PAUSED));
        statusCombo.getSelectionModel().select(updateMode ? branch.getStatus() : STATUS_ACTIVE);
        statusCombo.setMaxWidth(Double.MAX_VALUE);
        statusCombo.getStyleClass().add("page-combo");
        TextArea noteArea = new TextArea(updateMode ? branch.getNote() : "");
        noteArea.setPromptText("Ghi chú");
        noteArea.setPrefRowCount(3);
        noteArea.setWrapText(true);
        noteArea.getStyleClass().add("page-text-area");

        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(12);
        form.getStyleClass().add("branch-form-grid");
        form.add(formLabel("Mã chi nhánh"), 0, 0);
        form.add(codeField, 1, 0);
        form.add(formLabel("Tên chi nhánh"), 2, 0);
        form.add(nameField, 3, 0);
        form.add(formLabel("Khu vực"), 0, 1);
        form.add(areaField, 1, 1);
        form.add(formLabel("Số điện thoại"), 2, 1);
        form.add(phoneField, 3, 1);
        form.add(formLabel("Người quản lý"), 0, 2);
        form.add(managerField, 1, 2);
        form.add(formLabel("Ngày mở"), 2, 2);
        form.add(openingDatePicker, 3, 2);
        form.add(formLabel("Trạng thái"), 0, 3);
        form.add(statusCombo, 1, 3);
        form.add(formLabel("Địa chỉ"), 0, 4);
        form.add(addressField, 1, 4, 3, 1);
        form.add(formLabel("Ghi chú"), 0, 5);
        form.add(noteArea, 1, 5, 3, 1);
        for (Node node : new Node[]{codeField, nameField, areaField, addressField, phoneField, managerField, openingDatePicker, statusCombo, noteArea}) {
            GridPane.setHgrow(node, Priority.ALWAYS);
        }
        return new BranchFormFields(form, codeField, nameField, areaField, addressField, phoneField,
                managerField, openingDatePicker, statusCombo, noteArea);
    }

    private BranchRow readBranchForm(BranchFormFields fields, BranchRow originalBranch) {
        String code = originalBranch == null ? generateNextBranchCode() : originalBranch.getCode();
        String name = safe(fields.nameField().getText());
        String address = safe(fields.addressField().getText());
        String area = deriveAreaFromAddress(address);
        String phone = safe(fields.phoneField().getText());
        String manager = safe(fields.managerField().getText());
        LocalDate openingDate = fields.openingDatePicker().getValue();
        String status = fields.statusCombo().getValue();
        String note = safe(fields.noteArea().getText());

        while (originalBranch == null && branchCodeExists(code)) {
            code = AutoCodeGenerator.generateNextCode("CN", branchCodesWithExtra(code));
        }
        if (name.isEmpty()) {
            AlertUtils.showWarning("Tên chi nhánh không được rỗng.");
            return null;
        }
        if (address.isEmpty()) {
            AlertUtils.showWarning("Địa chỉ chi nhánh không được rỗng.");
            return null;
        }
        if (!isValidPhone(phone)) {
            AlertUtils.showWarning("Số điện thoại không hợp lệ.");
            return null;
        }
        if (openingDate == null) {
            AlertUtils.showWarning("Ngày mở chi nhánh không được rỗng.");
            return null;
        }
        if (safe(status).isEmpty()) {
            AlertUtils.showWarning("Trạng thái không được rỗng.");
            return null;
        }

        if (originalBranch != null) {
            LocalDate statusChangedDate = resolveStatusChangedDate(originalBranch, status);
            return new BranchRow(code, name, area, address, phone, manager, originalBranch.getManagerPhone(),
                    originalBranch.getManagerEmail(), openingDate, status, statusChangedDate, note, originalBranch.getEmployeeCount(),
                    originalBranch.getServingProductCount(), originalBranch.getOrderCount(),
                    originalBranch.getMonthRevenue(), originalBranch.getIngredientCount());
        }
        LocalDate statusChangedDate = STATUS_PAUSED.equals(status) ? LocalDate.now() : null;
        return new BranchRow(code, name, area, address, phone, manager, "", "", openingDate, status,
                statusChangedDate, note, 0, 0, 0, null, 0);
    }

    private LocalDate resolveStatusChangedDate(BranchRow originalBranch, String status) {
        if (safe(status).equals(originalBranch.getStatus())) {
            return originalBranch.getStatusChangedDate();
        }
        return STATUS_PAUSED.equals(status) ? LocalDate.now() : null;
    }

    private String generateNextBranchCode() {
        return AutoCodeGenerator.generateNextCode("CN", branches.stream().map(BranchRow::getCode).toList());
    }

    private boolean branchCodeExists(String code) {
        return branches.stream().anyMatch(branch -> branch.getCode().equalsIgnoreCase(code));
    }

    private List<String> branchCodesWithExtra(String code) {
        List<String> codes = new java.util.ArrayList<>(branches.stream().map(BranchRow::getCode).toList());
        codes.add(code);
        return codes;
    }

    private void showBranchDetails(BranchRow branch) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Xem chi tiết chi nhánh");
        dialog.setHeaderText(branch.getCode() + " - " + branch.getName());
        ButtonType closeType = new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeType);
        double maxDialogHeight = Screen.getPrimary().getVisualBounds().getHeight() * 0.8;
        double maxContentHeight = Math.max(360, maxDialogHeight - 145);
        dialog.getDialogPane().setPrefWidth(800);
        dialog.getDialogPane().setMaxHeight(maxDialogHeight);
        attachDialogStyles(dialog);
        dialog.getDialogPane().lookupButton(closeType).getStyleClass().add("secondary-action-button");

        BranchOperationStats operationStats = operationStatsForPeriod(branch);
        VBox branchInfoSection = detailSection("Thông tin chi nhánh",
                detailRow("Mã chi nhánh", branch.getCode()),
                detailRow("Tên chi nhánh", branch.getName()),
                detailRow("Khu vực", branch.getArea()),
                detailRow("Địa chỉ", branch.getAddress()),
                detailRow("Số điện thoại", branch.getPhone()),
                detailRow("Trạng thái", statusBadge(getStatusForPeriod(branch))),
                detailRow("Ngày mở", formatDate(branch.getOpeningDate())),
                detailRow("Ghi chú", branch.getNote()));
        VBox managerSection = detailSection("Thông tin quản lý",
                detailRow("Người quản lý", branch.getManager()),
                detailRow("SĐT quản lý", branch.getManagerPhone()),
                detailRow("Email quản lý", branch.getManagerEmail()));
        branchInfoSection.setMaxWidth(Double.MAX_VALUE);
        managerSection.setMaxWidth(Double.MAX_VALUE);
        HBox topSections = new HBox(10, branchInfoSection, managerSection);
        topSections.getStyleClass().add("branch-detail-top-row");
        HBox.setHgrow(branchInfoSection, Priority.ALWAYS);
        HBox.setHgrow(managerSection, Priority.ALWAYS);

        VBox content = new VBox(10,
                topSections,
                detailSection("Thông tin vận hành",
                        detailRow("Kỳ dữ liệu", formatPeriodLabel(activePeriodType, activePeriodDate)),
                        detailRow("Số nhân viên", branch.getEmployeeCount() > 0 ? branch.getEmployeeCount() + " nhân viên" : "Chưa có dữ liệu"),
                        detailRow("SP đang phục vụ", operationStats.servingProductCount() + " sản phẩm"),
                        detailRow("Tổng đơn hàng", formatNumber(operationStats.orderCount())),
                        detailRow("Doanh thu", formatMoney(operationStats.revenue())),
                        detailRow("Nguyên liệu theo dõi", operationStats.ingredientCount() + " nguyên liệu"))
        );
        content.getStyleClass().add("branch-detail-content");
        content.setPadding(new Insets(2, 0, 0, 0));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPrefViewportHeight(Math.min(520, maxContentHeight));
        scrollPane.setMaxHeight(maxContentHeight);
        scrollPane.getStyleClass().add("branch-detail-scroll");
        dialog.getDialogPane().setContent(scrollPane);
        dialog.setOnShown(event -> {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.setMaxHeight(maxDialogHeight);
            if (stage.getHeight() > maxDialogHeight) {
                stage.setHeight(maxDialogHeight);
            }
            stage.centerOnScreen();
        });
        dialog.showAndWait();
    }

    private VBox detailSection(String title, Node... rows) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("branch-detail-section-title");
        VBox table = new VBox(rows);
        table.getStyleClass().add("branch-detail-grid");
        VBox section = new VBox(8, titleLabel, table);
        section.setMaxWidth(Double.MAX_VALUE);
        section.getStyleClass().add("branch-detail-section");
        return section;
    }

    private HBox detailRow(String label, String value) {
        return detailRow(label, detailValue(value));
    }

    private HBox detailRow(String label, Node valueNode) {
        Label labelNode = new Label(label);
        labelNode.setMinWidth(150);
        labelNode.setPrefWidth(150);
        labelNode.setMaxWidth(150);
        labelNode.setTextOverrun(OverrunStyle.CLIP);
        labelNode.getStyleClass().add("branch-detail-label");

        HBox valueCell = new HBox(valueNode);
        valueCell.setAlignment(Pos.CENTER_LEFT);
        valueCell.getStyleClass().add("branch-detail-value-cell");
        HBox.setHgrow(valueCell, Priority.ALWAYS);
        HBox row = new HBox(labelNode, valueCell);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("branch-detail-row");
        return row;
    }

    private Label detailValue(String value) {
        Label label = new Label(safe(value).isEmpty() ? "Chưa có dữ liệu" : value);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.getStyleClass().add("branch-detail-value");
        return label;
    }

    private Label statusBadge(String status) {
        Label badge = new Label(status);
        badge.getStyleClass().addAll("status-badge", STATUS_ACTIVE.equals(status) ? "status-success" : "status-warning");
        return badge;
    }

    private void toggleBranchStatus(BranchRow branch) {
        boolean paused = STATUS_PAUSED.equals(branch.getStatus());
        String action = paused ? "mở lại" : "tạm dừng";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn có chắc muốn " + action + " chi nhánh " + branch.getCode() + "?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        try {
            branchManagementService.updateStatus(branch.getCode(), paused ? 1 : 0);
        } catch (Exception e) {
            AlertUtils.showError(readableError(e));
            e.printStackTrace();
            return;
        }
        loadBranches();
        applyFilters();
    }

    private TextField textField(String value, String promptText) {
        TextField field = new TextField(value);
        field.setPromptText(promptText);
        field.setMaxWidth(Double.MAX_VALUE);
        field.getStyleClass().add("page-input");
        return field;
    }

    private Label formLabel(String text) {
        Label label = new Label(text);
        label.setMinWidth(112);
        label.setPrefWidth(112);
        label.setMaxWidth(112);
        label.setTextOverrun(OverrunStyle.CLIP);
        label.getStyleClass().add("form-label");
        return label;
    }

    private boolean isValidPhone(String phone) {
        return !safe(phone).isEmpty() && phone.matches("\\d{9,11}");
    }

    private String statusLabel(int status) {
        return status == 1 ? STATUS_ACTIVE : STATUS_PAUSED;
    }

    private int statusToDb(String status) {
        return STATUS_ACTIVE.equals(status) ? 1 : 0;
    }

    private void attachDialogStyles(Dialog<?> dialog) {
        Optional.ofNullable(getClass().getResource("/com/phungloccoffee/gui/css/pages.css"))
                .ifPresent(resource -> dialog.getDialogPane().getStylesheets().add(resource.toExternalForm()));
        dialog.getDialogPane().getStyleClass().add("branch-dialog-pane");
    }

    private String formatDate(LocalDate date) {
        return date == null ? "Chưa có dữ liệu" : date.format(DATE_FORMATTER);
    }

    private String formatMoney(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return "Chưa có dữ liệu";
        }
        long roundedValue = value.setScale(0, RoundingMode.HALF_UP).longValue();
        return String.format(Locale.GERMANY, "%,d", roundedValue) + " đ";
    }

    private String formatNumber(int value) {
        return String.format(Locale.GERMANY, "%,d", value);
    }

    private String normalize(String value) {
        String lower = safe(value).toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.replace("đ", "d");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String deriveAreaFromAddress(String address) {
        String value = safe(address);
        if (value.isEmpty()) {
            return "Chưa xác định";
        }
        String[] parts = value.split(",");
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = safe(parts[i]);
            if (!part.isEmpty()) {
                return part;
            }
        }
        return value;
    }

    private String readableError(Exception exception) {
        String message = exception.getMessage();
        return safe(message).isEmpty() ? "Không thể thực hiện thao tác với cơ sở dữ liệu." : message;
    }

    private record BranchFormFields(Node container, TextField codeField, TextField nameField,
                                    TextField areaField, TextField addressField, TextField phoneField,
                                    TextField managerField, DatePicker openingDatePicker,
                                    ComboBox<String> statusCombo, TextArea noteArea) {
    }

    private record PeriodRange(LocalDate start, LocalDate end) {
    }

    private record BranchOperationStats(int orderCount, BigDecimal revenue, int servingProductCount, int ingredientCount) {
    }

    private static class StatusCell<T> extends TableCell<T, String> {
        @Override
        protected void updateItem(String status, boolean empty) {
            super.updateItem(status, empty);
            if (empty || status == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label badge = new Label(status);
            badge.getStyleClass().addAll("status-badge", STATUS_ACTIVE.equals(status) ? "status-success" : "status-warning");
            setGraphic(badge);
            setText(null);
            setAlignment(Pos.CENTER_LEFT);
        }
    }

    private class ActionCell extends TableCell<BranchRow, Void> {
        private final HBox box = new HBox(10);
        private final Button viewButton = new Button("Xem");
        private final Button editButton = new Button("Sửa");
        private final Button toggleButton = new Button();

        ActionCell() {
            viewButton.getStyleClass().addAll("action-button", "action-btn-view", "branch-table-action-button");
            editButton.getStyleClass().addAll("action-button", "action-btn-edit", "branch-table-action-button");
            toggleButton.getStyleClass().addAll("action-button", "action-btn-pause", "branch-table-action-button");
            viewButton.setOnAction(event -> showBranchDetails(getTableRow().getItem()));
            editButton.setOnAction(event -> showEditBranchDialog(getTableRow().getItem()));
            toggleButton.setOnAction(event -> toggleBranchStatus(getTableRow().getItem()));
            box.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().addAll(viewButton, editButton, toggleButton);
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setGraphic(null);
                return;
            }
            BranchRow row = getTableRow().getItem();
            toggleButton.setText(STATUS_PAUSED.equals(row.getStatus()) ? "Mở lại" : "Tạm dừng");
            toggleButton.getStyleClass().removeAll("action-btn-pause", "action-btn-resume");
            toggleButton.getStyleClass().add(STATUS_PAUSED.equals(row.getStatus()) ? "action-btn-resume" : "action-btn-pause");
            setGraphic(box);
        }
    }

    public static class BranchRow {
        private String code;
        private String name;
        private String area;
        private String address;
        private String phone;
        private String manager;
        private String managerPhone;
        private String managerEmail;
        private LocalDate openingDate;
        private String status;
        private LocalDate statusChangedDate;
        private String note;
        private int employeeCount;
        private int servingProductCount;
        private int orderCount;
        private BigDecimal monthRevenue;
        private int ingredientCount;

        public BranchRow(String code, String name, String area, String address, String phone,
                         String manager, String managerPhone, String managerEmail, LocalDate openingDate,
                         String status, String note, int employeeCount, int servingProductCount,
                         int orderCount, BigDecimal monthRevenue, int ingredientCount) {
            this(code, name, area, address, phone, manager, managerPhone, managerEmail, openingDate, status,
                    null, note, employeeCount, servingProductCount, orderCount, monthRevenue, ingredientCount);
        }

        public BranchRow(String code, String name, String area, String address, String phone,
                         String manager, String managerPhone, String managerEmail, LocalDate openingDate,
                         String status, LocalDate statusChangedDate, String note, int employeeCount,
                         int servingProductCount, int orderCount, BigDecimal monthRevenue, int ingredientCount) {
            this.code = code;
            this.name = name;
            this.area = area;
            this.address = address;
            this.phone = phone;
            this.manager = manager;
            this.managerPhone = managerPhone;
            this.managerEmail = managerEmail;
            this.openingDate = openingDate;
            this.status = status;
            this.statusChangedDate = statusChangedDate;
            this.note = note;
            this.employeeCount = employeeCount;
            this.servingProductCount = servingProductCount;
            this.orderCount = orderCount;
            this.monthRevenue = monthRevenue;
            this.ingredientCount = ingredientCount;
        }

        public void copyFrom(BranchRow other) {
            this.name = other.name;
            this.area = other.area;
            this.address = other.address;
            this.phone = other.phone;
            this.manager = other.manager;
            this.managerPhone = other.managerPhone;
            this.managerEmail = other.managerEmail;
            this.openingDate = other.openingDate;
            this.status = other.status;
            this.statusChangedDate = other.statusChangedDate;
            this.note = other.note;
            this.employeeCount = other.employeeCount;
            this.servingProductCount = other.servingProductCount;
            this.orderCount = other.orderCount;
            this.monthRevenue = other.monthRevenue;
            this.ingredientCount = other.ingredientCount;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getArea() {
            return area;
        }

        public String getAddress() {
            return address;
        }

        public String getPhone() {
            return phone;
        }

        public String getManager() {
            return manager;
        }

        public String getManagerPhone() {
            return managerPhone;
        }

        public String getManagerEmail() {
            return managerEmail;
        }

        public LocalDate getOpeningDate() {
            return openingDate;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDate getStatusChangedDate() {
            return statusChangedDate;
        }

        public void setStatusChangedDate(LocalDate statusChangedDate) {
            this.statusChangedDate = statusChangedDate;
        }

        public String getNote() {
            return note;
        }

        public int getEmployeeCount() {
            return employeeCount;
        }

        public int getServingProductCount() {
            return servingProductCount;
        }

        public int getOrderCount() {
            return orderCount;
        }

        public BigDecimal getMonthRevenue() {
            return monthRevenue;
        }

        public int getIngredientCount() {
            return ingredientCount;
        }
    }
}
