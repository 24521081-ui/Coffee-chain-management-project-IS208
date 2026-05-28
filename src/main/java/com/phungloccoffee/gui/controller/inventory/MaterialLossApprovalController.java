package com.phungloccoffee.gui.controller.inventory;

import com.phungloccoffee.bus.MaterialLossBUS;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.gui.util.IconFactory;
import com.phungloccoffee.model.MaterialLossRecord;
import com.phungloccoffee.util.AlertUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class MaterialLossApprovalController {
    private static final String STATUS_PENDING = "Chờ duyệt";
    private static final String STATUS_APPROVED = "Đã duyệt";
    private static final String STATUS_REJECTED = "Từ chối";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private TableView<Row> lossTable;
    @FXML private TableColumn<Row, String> codeColumn;
    @FXML private TableColumn<Row, String> creatorColumn;
    @FXML private TableColumn<Row, String> materialColumn;
    @FXML private TableColumn<Row, String> quantityColumn;
    @FXML private TableColumn<Row, String> reasonColumn;
    @FXML private TableColumn<Row, String> dateColumn;
    @FXML private TableColumn<Row, String> statusColumn;
    @FXML private TableColumn<Row, Void> actionColumn;
    @FXML private StackPane lossTabIconContainer;
    @FXML private StackPane detailTabIconContainer;

    private final MaterialLossBUS materialLossBUS = new MaterialLossBUS();
    private final ObservableList<Row> rows = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        lossTabIconContainer.getChildren().setAll(IconFactory.createPageTabIcon("alert"));
        detailTabIconContainer.getChildren().setAll(IconFactory.createPageTabIcon("clipboard"));

        codeColumn.setCellValueFactory(data -> data.getValue().codeProperty());
        creatorColumn.setCellValueFactory(data -> data.getValue().creatorProperty());
        materialColumn.setCellValueFactory(data -> data.getValue().materialProperty());
        quantityColumn.setCellValueFactory(data -> data.getValue().quantityProperty());
        reasonColumn.setCellValueFactory(data -> data.getValue().reasonProperty());
        dateColumn.setCellValueFactory(data -> data.getValue().dateProperty());
        statusColumn.setCellValueFactory(data -> data.getValue().statusProperty());

        codeColumn.setCellFactory(column -> new LabelCell<>("cell-code"));
        creatorColumn.setCellFactory(column -> new LabelCell<>("cell-nv"));
        materialColumn.setCellFactory(column -> new WrapTextCell<>(180));
        quantityColumn.setCellFactory(column -> new QuantityCell());
        reasonColumn.setCellFactory(column -> new WrapTextCell<>(240));
        statusColumn.setCellFactory(column -> new StatusCell());
        actionColumn.setCellFactory(column -> new ActionCell());

        lossTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        lossTable.setItems(rows);
        loadRows();
    }

    private void loadRows() {
        try {
            rows.setAll(materialLossBUS.loadLossHistory().stream().map(Row::from).toList());
        } catch (DatabaseException | PermissionException e) {
            AlertUtils.showError(e.getMessage());
            rows.clear();
        }
    }

    private void approve(Row row) {
        try {
            materialLossBUS.approveLoss(row.getCode());
            AlertUtils.showInfo("Đã duyệt hao hụt và cập nhật tồn kho.");
            loadRows();
        } catch (ValidationException | PermissionException | DatabaseException e) {
            AlertUtils.showError(e.getMessage());
        }
    }

    private void reject(Row row) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Từ chối hao hụt");
        dialog.setHeaderText("Nhập lý do từ chối cho " + row.getCode());
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().isBlank()) {
            AlertUtils.showWarning("Bắt buộc nhập lý do từ chối.");
            return;
        }
        try {
            materialLossBUS.rejectLoss(row.getCode(), result.get());
            AlertUtils.showInfo("Đã từ chối phiếu hao hụt.");
            loadRows();
        } catch (ValidationException | PermissionException | DatabaseException e) {
            AlertUtils.showError(e.getMessage());
        }
    }

    private void showDetails(Row row) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Chi tiết hao hụt nguyên liệu");
        alert.setHeaderText(row.getCode() + " - " + row.getStatus());
        alert.setContentText("Nguồn dữ liệu: Cơ sở dữ liệu thật"
                + "\nNgười lập: " + row.getCreator()
                + "\nNguyên liệu: " + row.getMaterial()
                + "\nSố lượng: " + row.getQuantity()
                + "\nLý do: " + row.getReason()
                + "\nNgày ghi nhận: " + row.getDate()
                + "\nTrạng thái: " + row.getStatus());
        alert.showAndWait();
    }

    private static String statusStyle(String status) {
        return switch (status) {
            case STATUS_APPROVED -> "badge-approved";
            case STATUS_REJECTED -> "badge-rejected";
            default -> "badge-pending";
        };
    }

    private class LabelCell<T> extends TableCell<Row, T> {
        private final String styleClass;

        private LabelCell(String styleClass) {
            this.styleClass = styleClass;
        }

        @Override
        protected void updateItem(T value, boolean empty) {
            super.updateItem(value, empty);
            if (empty || value == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label label = new Label(String.valueOf(value));
            label.getStyleClass().add(styleClass);
            label.setWrapText(true);
            setGraphic(label);
            setText(null);
            setAlignment(Pos.CENTER);
        }
    }

    private class WrapTextCell<T> extends TableCell<Row, T> {
        private final double maxWidth;

        private WrapTextCell(double maxWidth) {
            this.maxWidth = maxWidth;
        }

        @Override
        protected void updateItem(T value, boolean empty) {
            super.updateItem(value, empty);
            if (empty || value == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label label = new Label(String.valueOf(value));
            label.setWrapText(true);
            label.setMaxWidth(maxWidth);
            label.setAlignment(Pos.CENTER);
            setGraphic(label);
            setText(null);
            setAlignment(Pos.CENTER);
        }
    }

    private class QuantityCell extends TableCell<Row, String> {
        @Override
        protected void updateItem(String value, boolean empty) {
            super.updateItem(value, empty);
            if (empty || value == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label label = new Label(value);
            label.getStyleClass().add("cell-qty");
            setGraphic(label);
            setText(null);
            setAlignment(Pos.CENTER);
        }
    }

    private class StatusCell extends TableCell<Row, String> {
        @Override
        protected void updateItem(String status, boolean empty) {
            super.updateItem(status, empty);
            if (empty || status == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label badge = new Label(status);
            badge.getStyleClass().addAll("badge", statusStyle(status));
            setGraphic(badge);
            setText(null);
            setAlignment(Pos.CENTER);
        }
    }

    private class ActionCell extends TableCell<Row, Void> {
        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Row row = getTableRow().getItem();
            VBox actionStack = new VBox(6);
            actionStack.setAlignment(Pos.CENTER);
            Button detail = new Button("Xem");
            detail.setOnAction(event -> showDetails(row));

            if (STATUS_PENDING.equals(row.getStatus())) {
                detail.getStyleClass().add("btn-text-blue");
                Button approve = new Button("Duyệt");
                approve.getStyleClass().add("btn-approve");
                approve.setOnAction(event -> approve(row));
                Button reject = new Button("Từ chối");
                reject.getStyleClass().add("btn-reject");
                reject.setOnAction(event -> reject(row));
                HBox actionRow = new HBox(6, approve, reject);
                actionRow.setAlignment(Pos.CENTER);
                actionStack.getChildren().addAll(detail, actionRow);
            } else {
                detail.getStyleClass().add("btn-view-bg");
                actionStack.getChildren().add(detail);
            }
            setGraphic(actionStack);
            setText(null);
            setAlignment(Pos.CENTER);
        }
    }

    public static class Row {
        private final SimpleStringProperty code;
        private final SimpleStringProperty creator;
        private final SimpleStringProperty material;
        private final SimpleStringProperty quantity;
        private final SimpleStringProperty reason;
        private final SimpleStringProperty date;
        private final SimpleStringProperty status;
        private final LocalDate createdDate;

        public Row(String code, String creator, String material, String quantity, String reason, String date, String status,
                   LocalDate createdDate) {
            this.code = new SimpleStringProperty(code);
            this.creator = new SimpleStringProperty(creator);
            this.material = new SimpleStringProperty(material);
            this.quantity = new SimpleStringProperty(quantity);
            this.reason = new SimpleStringProperty(reason);
            this.date = new SimpleStringProperty(date);
            this.status = new SimpleStringProperty(status);
            this.createdDate = createdDate;
        }

        public static Row from(MaterialLossRecord record) {
            BigDecimal quantity = record.getQuantity() == null ? BigDecimal.ZERO : record.getQuantity().stripTrailingZeros();
            return new Row(
                    record.getLossId(),
                    record.getEmployeeId() == null || record.getEmployeeId().isBlank() ? "N/A" : record.getEmployeeId(),
                    record.getMaterialId() + " - " + record.getMaterialName(),
                    quantity.toPlainString() + " " + (record.getUnit() == null ? "" : record.getUnit()),
                    record.getReason(),
                    record.getCreatedAt() == null ? "" : DATE_FORMATTER.format(record.getCreatedAt()),
                    toDisplayStatus(record.getStatus()),
                    record.getCreatedAt() == null ? null : record.getCreatedAt().toLocalDate()
            );
        }

        private static String toDisplayStatus(String status) {
            return switch (status) {
                case "DA_DUYET" -> STATUS_APPROVED;
                case "TU_CHOI" -> STATUS_REJECTED;
                default -> STATUS_PENDING;
            };
        }

        public SimpleStringProperty codeProperty() { return code; }
        public SimpleStringProperty creatorProperty() { return creator; }
        public SimpleStringProperty materialProperty() { return material; }
        public SimpleStringProperty quantityProperty() { return quantity; }
        public SimpleStringProperty reasonProperty() { return reason; }
        public SimpleStringProperty dateProperty() { return date; }
        public SimpleStringProperty statusProperty() { return status; }
        public String getCode() { return code.get(); }
        public String getMaterial() { return material.get(); }
        public String getQuantity() { return quantity.get(); }
        public String getReason() { return reason.get(); }
        public String getDate() { return date.get(); }
        public String getStatus() { return status.get(); }
        public String getCreator() { return creator.get(); }
        public LocalDate getCreatedDate() { return createdDate; }
    }
}
