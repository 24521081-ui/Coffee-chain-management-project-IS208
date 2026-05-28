package com.phungloccoffee.gui.controller.inventory;

import com.phungloccoffee.bus.WarehouseWorkflowBUS;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.model.WarehouseApprovalItem;
import com.phungloccoffee.model.WarehouseSlipLine;
import com.phungloccoffee.model.WarehouseSlipStatus;
import com.phungloccoffee.model.WarehouseSlipType;
import com.phungloccoffee.util.AlertUtils;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class StocktakeApprovalController {
    @FXML private TableView<Row> stocktakeTable;
    @FXML private TableColumn<Row, String> codeColumn;
    @FXML private TableColumn<Row, String> creatorColumn;
    @FXML private TableColumn<Row, String> dateColumn;
    @FXML private TableColumn<Row, String> noteColumn;
    @FXML private TableColumn<Row, Number> itemCountColumn;
    @FXML private TableColumn<Row, String> statusColumn;
    @FXML private TableColumn<Row, Void> actionColumn;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;

    private final WarehouseWorkflowBUS workflowBUS = new WarehouseWorkflowBUS();
    private final ObservableList<Row> rows = FXCollections.observableArrayList();
    private final ObservableList<Row> allRows = FXCollections.observableArrayList();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final DecimalFormat quantityFormat = new DecimalFormat("#,##0.##");

    @FXML
    private void initialize() {
        codeColumn.setCellValueFactory(data -> data.getValue().codeProperty());
        creatorColumn.setCellValueFactory(data -> data.getValue().creatorProperty());
        dateColumn.setCellValueFactory(data -> data.getValue().dateProperty());
        noteColumn.setCellValueFactory(data -> data.getValue().noteProperty());
        itemCountColumn.setCellValueFactory(data -> data.getValue().itemCountProperty());
        statusColumn.setCellValueFactory(data -> data.getValue().statusProperty());
        statusColumn.setCellFactory(column -> new StatusCell());
        actionColumn.setCellFactory(column -> new ActionCell());
        stocktakeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        stocktakeTable.setItems(rows);
        loadRows();
    }

    private void loadRows() {
        try {
            List<WarehouseApprovalItem> items = workflowBUS.loadApprovalHistory(WarehouseSlipType.STOCKTAKE);
            allRows.setAll(items.stream().map(item -> new Row(
                    item.getSlipId(),
                    item.getCreatedBy(),
                    item.getCreatedAt().toLocalDate(),
                    item.getCreatedAt().format(formatter),
                    safeText(item.getRelatedParty()),
                    item.getItemCount(),
                    WarehouseSlipStatus.toDisplay(item.getStatus())
            )).toList());
            applyDateFilter();
        } catch (Exception e) {
            allRows.clear();
            rows.clear();
            AlertUtils.showError("Không thể tải danh sách phiếu kiểm kê chờ duyệt.");
        }
    }

    @FXML
    private void filterByDate() {
        applyDateFilter();
    }

    @FXML
    private void resetDateFilter() {
        if (fromDatePicker != null) {
            fromDatePicker.setValue(null);
        }
        if (toDatePicker != null) {
            toDatePicker.setValue(null);
        }
        applyDateFilter();
    }

    private void applyDateFilter() {
        LocalDate from = fromDatePicker == null ? null : fromDatePicker.getValue();
        LocalDate to = toDatePicker == null ? null : toDatePicker.getValue();
        rows.setAll(allRows.stream()
                .filter(row -> (from == null || !row.getCreatedDate().isBefore(from))
                        && (to == null || !row.getCreatedDate().isAfter(to)))
                .toList());
    }

    private void showDetails(Row row) {
        try {
            List<WarehouseSlipLine> details = workflowBUS.loadStocktakeDetails(row.getCode());

            TableView<DetailRow> detailTable = new TableView<>();
            detailTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            detailTable.setPrefHeight(420);

            TableColumn<DetailRow, String> itemCodeColumn = new TableColumn<>("Mã nguyên liệu");
            itemCodeColumn.setCellValueFactory(data -> data.getValue().itemCodeProperty());

            TableColumn<DetailRow, String> itemNameColumn = new TableColumn<>("Tên nguyên liệu");
            itemNameColumn.setCellValueFactory(data -> data.getValue().itemNameProperty());

            TableColumn<DetailRow, String> unitColumn = new TableColumn<>("Đơn vị");
            unitColumn.setCellValueFactory(data -> data.getValue().unitProperty());

            TableColumn<DetailRow, String> systemQtyColumn = new TableColumn<>("SL tồn kho");
            systemQtyColumn.setCellValueFactory(data -> data.getValue().systemQuantityProperty());

            TableColumn<DetailRow, String> actualQtyColumn = new TableColumn<>("SL thực tế");
            actualQtyColumn.setCellValueFactory(data -> data.getValue().actualQuantityProperty());

            TableColumn<DetailRow, String> varianceColumn = new TableColumn<>("Chênh lệch");
            varianceColumn.setCellValueFactory(data -> data.getValue().varianceProperty());

            detailTable.getColumns().setAll(
                    itemCodeColumn,
                    itemNameColumn,
                    unitColumn,
                    systemQtyColumn,
                    actualQtyColumn,
                    varianceColumn
            );

            ObservableList<DetailRow> detailRows = FXCollections.observableArrayList(
                    details.stream().map(this::toDetailRow).toList()
            );
            detailTable.setItems(detailRows);

            Label noteLabel = new Label("Giải trình / ghi chú của nhân viên");
            noteLabel.getStyleClass().add("section-title");

            TextArea explanationArea = new TextArea();
            explanationArea.setWrapText(true);
            explanationArea.setEditable(false);
            explanationArea.setPrefRowCount(4);
            explanationArea.setText(resolveExplanation(row));

            Label helperLabel = new Label(
                    "Lưu ý: schema hiện tại chỉ lưu được ghi chú/giải trình chung của phiếu kiểm kê. " +
                    "Chưa có cột lưu giải trình riêng cho từng nguyên liệu."
            );
            helperLabel.setWrapText(true);
            helperLabel.getStyleClass().add("section-subtitle");

            VBox content = new VBox(12);
            content.setPadding(new Insets(12));
            Label titleLabel = new Label("Chi tiết phiếu kiểm kê " + row.getCode());
            titleLabel.getStyleClass().add("section-title");
            Label metaLabel = new Label(
                    "Người lập: " + row.getCreator() +
                    " | Ngày lập: " + row.getDate() +
                    " | Số mặt hàng: " + row.getItemCount()
            );
            metaLabel.getStyleClass().add("section-subtitle");

            VBox.setVgrow(detailTable, Priority.ALWAYS);
            content.getChildren().addAll(titleLabel, metaLabel, detailTable, noteLabel, explanationArea, helperLabel);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Chi tiết phiếu kiểm kê");
            alert.setHeaderText(null);
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setContent(content);
            dialogPane.setPrefWidth(980);
            dialogPane.setPrefHeight(720);
            alert.showAndWait();
        } catch (ValidationException | PermissionException | DatabaseException e) {
            AlertUtils.showError(e.getMessage());
        }
    }

    private void approve(Row row) {
        try {
            workflowBUS.approve(WarehouseSlipType.STOCKTAKE, row.getCode());
            AlertUtils.showInfo("Đã duyệt phiếu kiểm kê và điều chỉnh tồn kho.");
            loadRows();
        } catch (ValidationException | PermissionException | DatabaseException e) {
            AlertUtils.showError(e.getMessage());
        }
    }

    private void reject(Row row) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Từ chối phiếu kiểm kê");
        dialog.setHeaderText("Nhập lý do từ chối cho " + row.getCode());
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().isBlank()) {
            AlertUtils.showWarning("Bắt buộc nhập lý do từ chối.");
            return;
        }
        try {
            workflowBUS.reject(WarehouseSlipType.STOCKTAKE, row.getCode(), result.get());
            AlertUtils.showInfo("Đã từ chối phiếu kiểm kê.");
            loadRows();
        } catch (ValidationException | PermissionException | DatabaseException e) {
            AlertUtils.showError(e.getMessage());
        }
    }

    private DetailRow toDetailRow(WarehouseSlipLine line) {
        BigDecimal systemQty = safeDecimal(line.getSystemQuantity());
        BigDecimal actualQty = safeDecimal(line.getActualQuantity());
        BigDecimal variance = actualQty.subtract(systemQty);
        return new DetailRow(
                safeText(line.getItemId()),
                safeText(line.getItemName()),
                safeText(line.getUnit()),
                formatQuantity(systemQty),
                formatQuantity(actualQty),
                formatQuantity(variance)
        );
    }

    private String resolveExplanation(Row row) {
        String note = safeText(row.getNote());
        return note.isBlank() ? "Nhân viên chưa nhập ghi chú hoặc giải trình chung cho phiếu này." : note;
    }

    private String formatQuantity(BigDecimal value) {
        return quantityFormat.format(safeDecimal(value));
    }

    private BigDecimal safeDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private class StatusCell extends TableCell<Row, String> {
        @Override
        protected void updateItem(String status, boolean empty) {
            super.updateItem(status, empty);
            if (empty || status == null) {
                setGraphic(null);
                return;
            }
            Label badge = new Label(status);
            String style = switch (status) {
                case "Đã duyệt" -> "badge-approved";
                case "Từ chối" -> "badge-rejected";
                default -> "badge-pending";
            };
            badge.getStyleClass().addAll("badge", style);
            setGraphic(badge);
            setAlignment(Pos.CENTER);
        }
    }

    private class ActionCell extends TableCell<Row, Void> {
        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setGraphic(null);
                return;
            }

            Row row = getTableRow().getItem();
            Button view = new Button("Xem chi tiết");
            view.getStyleClass().addAll("action-button", "action-btn-view");
            view.setOnAction(event -> showDetails(row));

            HBox box = new HBox(6);
            box.setAlignment(Pos.CENTER);
            box.getChildren().add(view);

            if ("Chờ duyệt".equals(row.getStatus())) {
                Button approve = new Button("Duyệt");
                approve.getStyleClass().add("btn-approve");
                approve.setOnAction(event -> approve(row));

                Button reject = new Button("Từ chối");
                reject.getStyleClass().add("btn-reject");
                reject.setOnAction(event -> reject(row));

                box.getChildren().addAll(approve, reject);
            }

            setGraphic(box);
        }
    }

    public static class Row {
        private final SimpleStringProperty code;
        private final SimpleStringProperty creator;
        private final LocalDate createdDate;
        private final SimpleStringProperty date;
        private final SimpleStringProperty note;
        private final SimpleIntegerProperty itemCount;
        private final SimpleStringProperty status;

        public Row(String code, String creator, LocalDate createdDate, String date, String note, int itemCount, String status) {
            this.code = new SimpleStringProperty(code);
            this.creator = new SimpleStringProperty(creator);
            this.createdDate = createdDate;
            this.date = new SimpleStringProperty(date);
            this.note = new SimpleStringProperty(note);
            this.itemCount = new SimpleIntegerProperty(itemCount);
            this.status = new SimpleStringProperty(status);
        }

        public SimpleStringProperty codeProperty() { return code; }
        public SimpleStringProperty creatorProperty() { return creator; }
        public SimpleStringProperty dateProperty() { return date; }
        public SimpleStringProperty noteProperty() { return note; }
        public SimpleIntegerProperty itemCountProperty() { return itemCount; }
        public SimpleStringProperty statusProperty() { return status; }
        public String getCode() { return code.get(); }
        public String getCreator() { return creator.get(); }
        public LocalDate getCreatedDate() { return createdDate; }
        public String getDate() { return date.get(); }
        public String getNote() { return note.get(); }
        public int getItemCount() { return itemCount.get(); }
        public String getStatus() { return status.get(); }
    }

    public static class DetailRow {
        private final SimpleStringProperty itemCode;
        private final SimpleStringProperty itemName;
        private final SimpleStringProperty unit;
        private final SimpleStringProperty systemQuantity;
        private final SimpleStringProperty actualQuantity;
        private final SimpleStringProperty variance;

        public DetailRow(String itemCode, String itemName, String unit,
                         String systemQuantity, String actualQuantity, String variance) {
            this.itemCode = new SimpleStringProperty(itemCode);
            this.itemName = new SimpleStringProperty(itemName);
            this.unit = new SimpleStringProperty(unit);
            this.systemQuantity = new SimpleStringProperty(systemQuantity);
            this.actualQuantity = new SimpleStringProperty(actualQuantity);
            this.variance = new SimpleStringProperty(variance);
        }

        public SimpleStringProperty itemCodeProperty() { return itemCode; }
        public SimpleStringProperty itemNameProperty() { return itemName; }
        public SimpleStringProperty unitProperty() { return unit; }
        public SimpleStringProperty systemQuantityProperty() { return systemQuantity; }
        public SimpleStringProperty actualQuantityProperty() { return actualQuantity; }
        public SimpleStringProperty varianceProperty() { return variance; }
    }
}
