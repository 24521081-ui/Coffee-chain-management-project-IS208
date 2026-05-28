package com.phungloccoffee.gui.controller.inventory;

import com.phungloccoffee.bus.WarehouseWorkflowBUS;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.gui.service.SessionManager;
import com.phungloccoffee.model.InventoryItem;
import com.phungloccoffee.model.WarehouseSlip;
import com.phungloccoffee.model.WarehouseSlipLine;
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

    private final WarehouseWorkflowBUS workflowBUS = new WarehouseWorkflowBUS();
    private final ObservableList<AuditRow> auditRows = FXCollections.observableArrayList();

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

        auditDetailTable.setEditable(true);
        auditDetailTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        auditDetailTable.setItems(auditRows);

        loadAuditRows();
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
        } catch (ValidationException | PermissionException | DatabaseException e) {
            AlertUtils.showError(e.getMessage());
        }
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
}
