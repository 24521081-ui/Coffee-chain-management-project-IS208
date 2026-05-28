package com.phungloccoffee.gui.controller.inventory;

import com.phungloccoffee.bus.WarehouseWorkflowBUS;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.gui.service.SessionManager;
import com.phungloccoffee.model.InventoryItem;
import com.phungloccoffee.model.WarehouseApprovalItem;
import com.phungloccoffee.model.WarehouseSlip;
import com.phungloccoffee.model.WarehouseSlipLine;
import com.phungloccoffee.model.WarehouseSlipStatus;
import com.phungloccoffee.model.WarehouseSlipType;
import com.phungloccoffee.util.AlertUtils;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.util.converter.DoubleStringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class InventoryAuditController {
    private static final double EXPLANATION_THRESHOLD_PERCENT = 2.0;

    @FXML private TextField auditCodeField;
    @FXML private DatePicker auditDatePicker;
    @FXML private TextField createdByField;
    @FXML private TextArea noteArea;
    @FXML private TableView<AuditRow> auditDetailTable;
    @FXML private TableColumn<AuditRow, String> codeColumn;
    @FXML private TableColumn<AuditRow, String> itemColumn;
    @FXML private TableColumn<AuditRow, String> unitColumn;
    @FXML private TableColumn<AuditRow, String> systemColumn;
    @FXML private TableColumn<AuditRow, Double> actualColumn;
    @FXML private TableColumn<AuditRow, String> diffColumn;
    @FXML private TableColumn<AuditRow, String> noteColumn;
    @FXML private TableColumn<AuditRow, String> actionColumn;
    @FXML private TableView<SlipHistoryRow> historyTable;
    @FXML private TableColumn<SlipHistoryRow, String> historyCodeColumn;
    @FXML private TableColumn<SlipHistoryRow, String> historyDateColumn;
    @FXML private TableColumn<SlipHistoryRow, String> historyStatusColumn;
    @FXML private TableColumn<SlipHistoryRow, String> historyNoteColumn;
    @FXML private DatePicker historyFromDatePicker;
    @FXML private DatePicker historyToDatePicker;

    private final WarehouseWorkflowBUS workflowBUS = new WarehouseWorkflowBUS();
    private final ObservableList<AuditRow> auditRows = FXCollections.observableArrayList();
    private final ObservableList<SlipHistoryRow> historyRows = FXCollections.observableArrayList();
    private final ObservableList<SlipHistoryRow> allHistoryRows = FXCollections.observableArrayList();
    private final DateTimeFormatter historyFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        auditDatePicker.setValue(LocalDate.now());
        createdByField.setText(SessionManager.getCurrentUser() == null ? "" : SessionManager.getCurrentUser().getFullName());
        createdByField.setEditable(false);

        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        itemColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));
        systemColumn.setCellValueFactory(new PropertyValueFactory<>("systemText"));
        actualColumn.setCellValueFactory(new PropertyValueFactory<>("actual"));
        actualColumn.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        actualColumn.setOnEditCommit(event -> {
            AuditRow row = event.getRowValue();
            row.setActual(Math.max(0, event.getNewValue()));
            auditDetailTable.refresh();
        });
        diffColumn.setCellValueFactory(new PropertyValueFactory<>("diffText"));
        noteColumn.setCellValueFactory(new PropertyValueFactory<>("note"));
        noteColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        noteColumn.setOnEditCommit(event -> event.getRowValue().setNote(event.getNewValue() == null ? "" : event.getNewValue().trim()));
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("explanationRequirement"));
        actionColumn.setCellFactory(column -> new ExplanationRequirementCell());

        historyCodeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        historyDateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        historyStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        historyStatusColumn.setCellFactory(column -> new HistoryStatusCell());
        historyNoteColumn.setCellValueFactory(new PropertyValueFactory<>("note"));

        auditDetailTable.setEditable(true);
        auditDetailTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        auditDetailTable.setItems(auditRows);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        historyTable.setItems(historyRows);

        loadAuditRows();
        loadHistory();
    }

    private void loadAuditRows() {
        try {
            List<InventoryItem> materials = workflowBUS.loadMaterialsForCurrentBranch();
            auditRows.setAll(materials.stream().map(AuditRow::from).toList());
        } catch (Exception e) {
            auditRows.setAll(
                    new AuditRow("NL001", "Cà phê rang xay", "KG", 34, 34, ""),
                    new AuditRow("NL002", "Sữa tươi không đường", "L", 12, 12, "")
            );
        }
    }

    @FXML
    private void saveDraft() {
        persist(false);
    }

    @FXML
    private void submitAudit() {
        persist(true);
    }

    @FXML
    private void reloadSystemQuantity() {
        loadAuditRows();
    }

    private void persist(boolean submit) {
        try {
            WarehouseSlip slip = new WarehouseSlip();
            slip.setSlipId(auditCodeField.getText());
            slip.setNote(noteArea.getText());
            slip.setLines(auditRows.stream().map(AuditRow::toLine).toList());
            if (submit) {
                workflowBUS.submitStocktake(slip);
                AlertUtils.showInfo("Phiếu kiểm kê đã gửi duyệt. Tồn kho chưa thay đổi.");
            } else {
                workflowBUS.saveStocktakeDraft(slip);
                AlertUtils.showInfo("Đã lưu nháp phiếu kiểm kê.");
            }
            auditCodeField.clear();
            noteArea.clear();
            loadAuditRows();
            loadHistory();
        } catch (ValidationException | PermissionException | DatabaseException e) {
            AlertUtils.showError(e.getMessage());
        }
    }

    private void loadHistory() {
        try {
            allHistoryRows.setAll(workflowBUS.loadMySlipHistory(WarehouseSlipType.STOCKTAKE).stream()
                    .map(this::toHistoryRow)
                    .toList());
            applyHistoryDateFilter();
        } catch (DatabaseException | PermissionException e) {
            allHistoryRows.clear();
            historyRows.clear();
        }
    }

    @FXML
    private void filterHistoryByDate() {
        applyHistoryDateFilter();
    }

    @FXML
    private void resetHistoryDateFilter() {
        historyFromDatePicker.setValue(null);
        historyToDatePicker.setValue(null);
        applyHistoryDateFilter();
    }

    private void applyHistoryDateFilter() {
        LocalDate from = historyFromDatePicker == null ? null : historyFromDatePicker.getValue();
        LocalDate to = historyToDatePicker == null ? null : historyToDatePicker.getValue();
        historyRows.setAll(allHistoryRows.stream()
                .filter(row -> (from == null || !row.getCreatedDate().isBefore(from))
                        && (to == null || !row.getCreatedDate().isAfter(to)))
                .toList());
    }

    private SlipHistoryRow toHistoryRow(WarehouseApprovalItem item) {
        String note = item.getRejectedReason() != null && !item.getRejectedReason().isBlank()
                ? item.getRejectedReason()
                : (item.getRelatedParty() == null ? "" : item.getRelatedParty());
        return new SlipHistoryRow(
                item.getSlipId(),
                item.getCreatedAt().toLocalDate(),
                item.getCreatedAt().format(historyFormatter),
                WarehouseSlipStatus.toDisplay(item.getStatus()),
                note
        );
    }

    private static class ExplanationRequirementCell extends TableCell<AuditRow, String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label badge = new Label(item);
            badge.getStyleClass().addAll("status-badge",
                    "Bắt buộc".equals(item) ? "status-warning" : "status-success");
            HBox box = new HBox(badge);
            box.setAlignment(Pos.CENTER_LEFT);
            setGraphic(box);
            setText(null);
        }
    }

    private static class HistoryStatusCell extends TableCell<SlipHistoryRow, String> {
        @Override
        protected void updateItem(String status, boolean empty) {
            super.updateItem(status, empty);
            if (empty || status == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            Label badge = new Label(status);
            String styleClass = switch (status) {
                case "ÄÃ£ duyá»‡t" -> "status-success";
                case "Tá»« chá»‘i" -> "status-danger";
                default -> "status-warning";
            };
            badge.getStyleClass().addAll("status-badge", styleClass);

            HBox box = new HBox(badge);
            box.setAlignment(Pos.CENTER_LEFT);
            setGraphic(box);
            setText(null);
        }
    }

    public static class AuditRow {
        private final SimpleStringProperty code;
        private final SimpleStringProperty name;
        private final SimpleStringProperty unit;
        private final double system;
        private final SimpleDoubleProperty actual;
        private final SimpleStringProperty note;

        public AuditRow(String code, String name, String unit, double system, double actual, String note) {
            this.code = new SimpleStringProperty(code);
            this.name = new SimpleStringProperty(name);
            this.unit = new SimpleStringProperty(unit);
            this.system = system;
            this.actual = new SimpleDoubleProperty(actual);
            this.note = new SimpleStringProperty(note);
        }

        public static AuditRow from(InventoryItem item) {
            double systemQty = item.getQuantityOnHand().doubleValue();
            return new AuditRow(item.getItemCode(), item.getItemName(), item.getUnit(), systemQty, systemQty, "");
        }

        public String getCode() { return code.get(); }
        public String getName() { return name.get(); }
        public String getUnit() { return unit.get(); }
        public double getActual() { return actual.get(); }
        public void setActual(double value) { this.actual.set(value); }
        public String getSystemText() { return trim(system) + " " + getUnit(); }
        public String getDiffText() { return trim(getActual() - system) + " " + getUnit(); }
        public String getNote() { return note.get(); }
        public void setNote(String value) { note.set(value); }
        public String getExplanationRequirement() { return requiresExplanation() ? "Bắt buộc" : "Không yêu cầu"; }

        public boolean requiresExplanation() {
            double delta = Math.abs(getActual() - system);
            if (system <= 0) {
                return delta > 0;
            }
            return (delta / system) * 100.0 > EXPLANATION_THRESHOLD_PERCENT;
        }

        public WarehouseSlipLine toLine() {
            WarehouseSlipLine line = new WarehouseSlipLine();
            line.setItemId(getCode());
            line.setItemName(getName());
            line.setUnit(getUnit());
            line.setSystemQuantity(BigDecimal.valueOf(system));
            line.setActualQuantity(BigDecimal.valueOf(getActual()));
            line.setNote(getNote());
            return line;
        }

        private static String trim(double value) {
            return value == Math.rint(value) ? String.valueOf((int) value) : String.valueOf(value);
        }
    }

    public static class SlipHistoryRow {
        private final SimpleStringProperty code;
        private final LocalDate createdDate;
        private final SimpleStringProperty date;
        private final SimpleStringProperty status;
        private final SimpleStringProperty note;

        public SlipHistoryRow(String code, LocalDate createdDate, String date, String status, String note) {
            this.code = new SimpleStringProperty(code);
            this.createdDate = createdDate;
            this.date = new SimpleStringProperty(date);
            this.status = new SimpleStringProperty(status);
            this.note = new SimpleStringProperty(note);
        }

        public String getCode() { return code.get(); }
        public LocalDate getCreatedDate() { return createdDate; }
        public String getDate() { return date.get(); }
        public String getStatus() { return status.get(); }
        public String getNote() { return note.get(); }
    }
}
