package com.phungloccoffee.gui.controller.inventory;

import com.phungloccoffee.bus.WarehouseTransferBUS;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.model.InventoryItem;
import com.phungloccoffee.model.Kho;
import com.phungloccoffee.model.WarehouseTransferLine;
import com.phungloccoffee.model.WarehouseTransferSlip;
import com.phungloccoffee.util.AlertUtils;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;

import java.math.BigDecimal;
import java.util.List;

public class TransferFormController {
    @FXML private ComboBox<Kho> sourceComboBox;
    @FXML private ComboBox<Kho> targetComboBox;
    @FXML private TextArea noteArea;
    @FXML private TableView<TransferRow> transferTable;
    @FXML private TableColumn<TransferRow, String> codeColumn;
    @FXML private TableColumn<TransferRow, String> materialColumn;
    @FXML private TableColumn<TransferRow, String> unitColumn;
    @FXML private TableColumn<TransferRow, String> quantityColumn;
    @FXML private TableColumn<TransferRow, Double> requestQuantityColumn;
    @FXML private TableColumn<TransferRow, String> statusColumn;

    private final WarehouseTransferBUS transferBUS = new WarehouseTransferBUS();
    private final ObservableList<TransferRow> transferRows = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        configureWarehouseCombos();

        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        materialColumn.setCellValueFactory(new PropertyValueFactory<>("material"));
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        requestQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("requestQuantity"));
        requestQuantityColumn.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        requestQuantityColumn.setOnEditCommit(event -> {
            TransferRow row = event.getRowValue();
            row.setRequestQuantity(Math.max(0, event.getNewValue()));
            transferTable.refresh();
        });
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(column -> new StatusCell<>());

        transferTable.setEditable(true);
        transferTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        transferTable.setItems(transferRows);

        loadWarehousesAndMaterials();
    }

    private void configureWarehouseCombos() {
        StringConverter<Kho> converter = new StringConverter<>() {
            @Override
            public String toString(Kho kho) {
                return kho == null ? "" : kho.getTenKho();
            }

            @Override
            public Kho fromString(String string) {
                return null;
            }
        };
        sourceComboBox.setConverter(converter);
        targetComboBox.setConverter(converter);
        sourceComboBox.setDisable(true);
    }

    private void loadWarehousesAndMaterials() {
        try {
            Kho sourceWarehouse = transferBUS.resolveCurrentWarehouse();
            sourceComboBox.setItems(FXCollections.observableArrayList(sourceWarehouse));
            sourceComboBox.getSelectionModel().selectFirst();

            List<Kho> targets = transferBUS.loadTargetWarehouses(sourceWarehouse.getKhoId());
            targetComboBox.setItems(FXCollections.observableArrayList(targets));
            if (!targets.isEmpty()) {
                targetComboBox.getSelectionModel().selectFirst();
            }

            List<InventoryItem> materials = transferBUS.loadSourceMaterials();
            transferRows.setAll(materials.stream().map(TransferRow::from).toList());
        } catch (DatabaseException | PermissionException | ValidationException e) {
            AlertUtils.showError(e.getMessage());
            transferRows.clear();
            sourceComboBox.getItems().clear();
            targetComboBox.getItems().clear();
        }
    }

    @FXML
    private void saveTransfer() {
        try {
            Kho source = sourceComboBox.getValue();
            Kho target = targetComboBox.getValue();
            if (source == null) {
                throw new ValidationException("Không xác định được kho nguồn.");
            }
            if (target == null) {
                throw new ValidationException("Bạn cần chọn kho đích.");
            }

            List<WarehouseTransferLine> lines = transferRows.stream()
                    .filter(row -> row.getRequestQuantity() > 0)
                    .map(TransferRow::toLine)
                    .toList();

            WarehouseTransferSlip slip = new WarehouseTransferSlip();
            slip.setSourceWarehouseId(source.getKhoId());
            slip.setSourceWarehouseName(source.getTenKho());
            slip.setTargetWarehouseId(target.getKhoId());
            slip.setTargetWarehouseName(target.getTenKho());
            slip.setLines(lines);

            transferBUS.submitTransfer(slip);
            AlertUtils.showInfo("Đã gửi phiếu điều chuyển kho chờ duyệt.");
            noteArea.clear();
            loadWarehousesAndMaterials();
        } catch (ValidationException | PermissionException | DatabaseException e) {
            AlertUtils.showError(e.getMessage());
        }
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
            badge.getStyleClass().addAll("status-badge",
                    "Sẵn sàng".equals(status) ? "status-success" : "status-warning");
            setGraphic(badge);
            setText(null);
            setAlignment(Pos.CENTER_LEFT);
        }
    }

    public static class TransferRow {
        private final String code;
        private final String material;
        private final String unit;
        private final double availableQuantity;
        private final SimpleDoubleProperty requestQuantity;

        public TransferRow(String code, String material, String unit, double availableQuantity, double requestQuantity) {
            this.code = code;
            this.material = material;
            this.unit = unit;
            this.availableQuantity = availableQuantity;
            this.requestQuantity = new SimpleDoubleProperty(requestQuantity);
        }

        public static TransferRow from(InventoryItem item) {
            double available = item.getQuantityOnHand() == null ? 0 : item.getQuantityOnHand().doubleValue();
            return new TransferRow(item.getItemCode(), item.getItemName(), item.getUnit(), available, 0);
        }

        public String getCode() {
            return code;
        }

        public String getMaterial() {
            return material;
        }

        public String getUnit() {
            return unit;
        }

        public String getQuantity() {
            return trim(availableQuantity) + " " + unit;
        }

        public Double getRequestQuantity() {
            return requestQuantity.get();
        }

        public void setRequestQuantity(double value) {
            requestQuantity.set(value);
        }

        public String getStatus() {
            return requestQuantity.get() > availableQuantity ? "Vượt tồn" : "Sẵn sàng";
        }

        public WarehouseTransferLine toLine() {
            return new WarehouseTransferLine(code, material, unit, BigDecimal.valueOf(requestQuantity.get()));
        }

        private static String trim(double value) {
            return value == Math.rint(value) ? String.valueOf((int) value) : String.valueOf(value);
        }
    }
}
