package com.phungloccoffee.gui.controller.inventory;

import com.phungloccoffee.bus.WarehouseTransferBUS;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.gui.util.IconFactory;
import com.phungloccoffee.model.WarehouseTransferLine;
import com.phungloccoffee.model.WarehouseTransferSlip;
import com.phungloccoffee.util.AlertUtils;
import javafx.beans.property.SimpleIntegerProperty;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class TransferApprovalController {
    private static final String STATUS_PENDING = "Chờ duyệt";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private TableView<Row> transferTable;
    @FXML private TableColumn<Row, String> codeColumn;
    @FXML private TableColumn<Row, String> creatorColumn;
    @FXML private TableColumn<Row, String> sourceColumn;
    @FXML private TableColumn<Row, String> targetColumn;
    @FXML private TableColumn<Row, String> dateColumn;
    @FXML private TableColumn<Row, Number> itemCountColumn;
    @FXML private TableColumn<Row, String> statusColumn;
    @FXML private TableColumn<Row, Void> actionColumn;
    @FXML private StackPane transferTabIconContainer;
    @FXML private StackPane detailTabIconContainer;

    private final WarehouseTransferBUS transferBUS = new WarehouseTransferBUS();
    private final ObservableList<Row> rows = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        transferTabIconContainer.getChildren().setAll(IconFactory.createPageTabIcon("repeat"));
        detailTabIconContainer.getChildren().setAll(IconFactory.createPageTabIcon("clipboard"));

        codeColumn.setCellValueFactory(data -> data.getValue().codeProperty());
        creatorColumn.setCellValueFactory(data -> data.getValue().creatorProperty());
        sourceColumn.setCellValueFactory(data -> data.getValue().sourceProperty());
        targetColumn.setCellValueFactory(data -> data.getValue().targetProperty());
        dateColumn.setCellValueFactory(data -> data.getValue().dateProperty());
        itemCountColumn.setCellValueFactory(data -> data.getValue().itemCountProperty());
        statusColumn.setCellValueFactory(data -> data.getValue().statusProperty());

        codeColumn.setCellFactory(column -> new CodeCell());
        creatorColumn.setCellFactory(column -> new CreatorCell());
        sourceColumn.setCellFactory(column -> new WrapTextCell<>());
        targetColumn.setCellFactory(column -> new WrapTextCell<>());
        itemCountColumn.setCellFactory(column -> new QuantityCell());
        statusColumn.setCellFactory(column -> new StatusCell());
        actionColumn.setCellFactory(column -> new ActionCell());

        transferTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        transferTable.setItems(rows);

        loadTransfers();
    }

    private void loadTransfers() {
        try {
            rows.setAll(transferBUS.loadPendingTransfers().stream().map(Row::from).toList());
        } catch (DatabaseException | PermissionException e) {
            AlertUtils.showError(e.getMessage());
            rows.clear();
        }
    }

    private void approve(Row row) {
        try {
            transferBUS.approveTransfer(row.getCode());
            AlertUtils.showInfo("Đã duyệt phiếu điều chuyển kho.");
            loadTransfers();
        } catch (ValidationException | PermissionException | DatabaseException e) {
            AlertUtils.showError(e.getMessage());
        }
    }

    private void reject(Row row) {
        try {
            transferBUS.rejectTransfer(row.getCode());
            AlertUtils.showInfo("Đã từ chối phiếu điều chuyển kho.");
            loadTransfers();
        } catch (ValidationException | PermissionException | DatabaseException e) {
            AlertUtils.showError(e.getMessage());
        }
    }

    private void showDetails(Row row) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Chi tiết phiếu điều chuyển");
        alert.setHeaderText(row.getCode() + " - " + row.getStatus());
        alert.setContentText("Người lập: " + row.getCreator()
                + "\nKho nguồn: " + row.getSource()
                + "\nKho đích: " + row.getTarget()
                + "\nNgày lập: " + row.getDate()
                + "\nSố lượng mặt hàng: " + row.getItemCount()
                + "\n\nChi tiết:\n" + row.getDetails());
        alert.showAndWait();
    }

    private static String splitCode(String code) {
        int dashIndex = code.indexOf('-');
        if (dashIndex < 0 || dashIndex == code.length() - 1) {
            return code;
        }
        return code.substring(0, dashIndex + 1) + "\n" + code.substring(dashIndex + 1);
    }

    private class CodeCell extends TableCell<Row, String> {
        @Override
        protected void updateItem(String code, boolean empty) {
            super.updateItem(code, empty);
            if (empty || code == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label label = new Label(splitCode(code));
            label.getStyleClass().add("cell-code");
            label.setAlignment(Pos.CENTER);
            setGraphic(label);
            setText(null);
            setAlignment(Pos.CENTER);
        }
    }

    private class CreatorCell extends TableCell<Row, String> {
        @Override
        protected void updateItem(String creator, boolean empty) {
            super.updateItem(creator, empty);
            if (empty || creator == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label label = new Label(creator);
            label.getStyleClass().add("cell-nv");
            setGraphic(label);
            setText(null);
            setAlignment(Pos.CENTER);
        }
    }

    private class WrapTextCell<T> extends TableCell<Row, T> {
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
            label.setMaxWidth(150);
            label.setAlignment(Pos.CENTER);
            setGraphic(label);
            setText(null);
            setAlignment(Pos.CENTER);
        }
    }

    private class QuantityCell extends TableCell<Row, Number> {
        @Override
        protected void updateItem(Number count, boolean empty) {
            super.updateItem(count, empty);
            if (empty || count == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label label = new Label(String.valueOf(count.intValue()));
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
            badge.getStyleClass().addAll("badge", "badge-pending");
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
            detail.getStyleClass().add("btn-text-blue");
            detail.setOnAction(event -> showDetails(row));

            Button approve = new Button("Duyệt");
            approve.getStyleClass().add("btn-approve");
            approve.setOnAction(event -> approve(row));

            Button reject = new Button("Từ chối");
            reject.getStyleClass().add("btn-reject");
            reject.setOnAction(event -> reject(row));

            HBox actionRow = new HBox(6, approve, reject);
            actionRow.setAlignment(Pos.CENTER);
            actionStack.getChildren().addAll(detail, actionRow);

            setGraphic(actionStack);
            setText(null);
            setAlignment(Pos.CENTER);
        }
    }

    public static class Row {
        private final SimpleStringProperty code;
        private final SimpleStringProperty creator;
        private final SimpleStringProperty source;
        private final SimpleStringProperty target;
        private final SimpleStringProperty date;
        private final SimpleIntegerProperty itemCount;
        private final SimpleStringProperty status;
        private final String details;

        public Row(String code, String creator, String source, String target, String date, int itemCount, String status, String details) {
            this.code = new SimpleStringProperty(code);
            this.creator = new SimpleStringProperty(creator);
            this.source = new SimpleStringProperty(source);
            this.target = new SimpleStringProperty(target);
            this.date = new SimpleStringProperty(date);
            this.itemCount = new SimpleIntegerProperty(itemCount);
            this.status = new SimpleStringProperty(status);
            this.details = details;
        }

        public static Row from(WarehouseTransferSlip slip) {
            String detailText = slip.getLines().stream()
                    .map(Row::formatLine)
                    .collect(Collectors.joining("\n"));
            return new Row(
                    slip.getSlipId(),
                    slip.getCreatedBy(),
                    slip.getSourceWarehouseName(),
                    slip.getTargetWarehouseName(),
                    slip.getCreatedAt() == null ? "" : DATE_FORMATTER.format(slip.getCreatedAt()),
                    slip.getLines().size(),
                    STATUS_PENDING,
                    detailText
            );
        }

        private static String formatLine(WarehouseTransferLine line) {
            BigDecimal quantity = line.getQuantity() == null ? BigDecimal.ZERO : line.getQuantity().stripTrailingZeros();
            return line.getItemId() + " - " + line.getItemName() + ": " + quantity.toPlainString() + " " + line.getUnit();
        }

        public SimpleStringProperty codeProperty() { return code; }
        public SimpleStringProperty creatorProperty() { return creator; }
        public SimpleStringProperty sourceProperty() { return source; }
        public SimpleStringProperty targetProperty() { return target; }
        public SimpleStringProperty dateProperty() { return date; }
        public SimpleIntegerProperty itemCountProperty() { return itemCount; }
        public SimpleStringProperty statusProperty() { return status; }
        public String getCode() { return code.get(); }
        public String getCreator() { return creator.get(); }
        public String getSource() { return source.get(); }
        public String getTarget() { return target.get(); }
        public String getDate() { return date.get(); }
        public int getItemCount() { return itemCount.get(); }
        public String getStatus() { return status.get(); }
        public String getDetails() { return details; }
    }
}
