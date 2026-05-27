package com.phungloccoffee.gui.controller.product;

import com.phungloccoffee.gui.service.ProductCatalogService;
import com.phungloccoffee.model.Product;
import com.phungloccoffee.model.ProductCategory;
import com.phungloccoffee.model.product.ProductCatalogModels.Ingredient;
import com.phungloccoffee.model.product.ProductCatalogModels.ProductMetadata;
import com.phungloccoffee.model.product.ProductCatalogModels.ProductRecipe;
import com.phungloccoffee.util.AlertUtils;
import com.phungloccoffee.util.AutoCodeGenerator;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class ProductManagementController {
    private static final String STATUS_ACTIVE = com.phungloccoffee.model.product.ProductCatalogModels.STATUS_ACTIVE;
    private static final String STATUS_INACTIVE = com.phungloccoffee.model.product.ProductCatalogModels.STATUS_INACTIVE;
    private static final String DEFAULT_PRODUCT_TYPE = com.phungloccoffee.model.product.ProductCatalogModels.DEFAULT_PRODUCT_TYPE;
    private static final String DEFAULT_PRODUCT_UNIT = com.phungloccoffee.model.product.ProductCatalogModels.DEFAULT_PRODUCT_UNIT;
    private static final String DEFAULT_RECIPE_STATUS = "Đang áp dụng";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private TextField searchField;
    @FXML private Label resultCountLabel;
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, String> codeColumn;
    @FXML private TableColumn<Product, String> nameColumn;
    @FXML private TableColumn<Product, BigDecimal> priceColumn;
    @FXML private TableColumn<Product, String> statusColumn;
    @FXML private TableColumn<Product, String> recipeStatusColumn;
    @FXML private Button detailButton;
    @FXML private Button editButton;
    @FXML private Button recipeButton;

    private final ProductCatalogService productCatalogService = new ProductCatalogService();
    private final ObservableList<Product> products = FXCollections.observableArrayList();
    private final ObservableList<ProductCategory> categories = FXCollections.observableArrayList();
    private FilteredList<Product> filteredProducts;

    @FXML
    private void initialize() {
        setupTable();
        setupSelectionActions();
        loadCategories();
        loadProducts(null);
    }

    private void setupTable() {
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceColumn.setCellFactory(column -> new PriceCell<>());
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(column -> new StatusCell<>());
        recipeStatusColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(recipeStatusFor(data.getValue())));
        recipeStatusColumn.setCellFactory(column -> new RecipeStatusCell<>());

        filteredProducts = new FilteredList<>(products, product -> true);
        productTable.setItems(filteredProducts);
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        productTable.setPlaceholder(new Label("Không tìm thấy sản phẩm phù hợp"));
        productTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldProduct, selectedProduct) -> updateSelectionActions());
        searchField.setOnAction(event -> applySearchFilter());
    }

    private void setupSelectionActions() {
        updateSelectionActions();
    }

    private void updateSelectionActions() {
        boolean hasSelection = productTable != null && productTable.getSelectionModel().getSelectedItem() != null;
        detailButton.setDisable(!hasSelection);
        editButton.setDisable(!hasSelection);
        recipeButton.setDisable(!hasSelection);
    }

    private void loadProducts(String productCodeToSelect) {
        try {
            List<Product> loadedProducts = productCatalogService.loadProducts();
            products.setAll(loadedProducts == null ? List.of() : loadedProducts);
        } catch (Exception e) {
            products.clear();
            AlertUtils.showError("Không thể tải sản phẩm từ cơ sở dữ liệu. Vui lòng kiểm tra cấu hình database.");
            e.printStackTrace();
        }
        applyCurrentFilter(false);
        selectProductByCode(productCodeToSelect);
        updateSelectionActions();
    }

    private void loadCategories() {
        try {
            List<ProductCategory> loadedCategories = productCatalogService.loadCategories();
            categories.setAll(loadedCategories == null ? List.of() : loadedCategories);
        } catch (Exception e) {
            categories.clear();
            AlertUtils.showError("Không thể tải danh mục sản phẩm từ cơ sở dữ liệu.");
            e.printStackTrace();
        }
    }

    @FXML
    private void applySearchFilter() {
        applyCurrentFilter(true);
    }

    private void applyCurrentFilter(boolean showEmptyMessage) {
        String keyword = normalize(searchField.getText());
        filteredProducts.setPredicate(product -> keyword.isBlank()
                || normalize(product.getCode()).contains(keyword)
                || normalize(product.getName()).contains(keyword)
                || normalize(product.getStatus()).contains(keyword));
        updateResultCount();
        if (showEmptyMessage && !keyword.isBlank() && filteredProducts.isEmpty()) {
            AlertUtils.showWarning("Không tìm thấy sản phẩm phù hợp");
        }
    }

    @FXML
    private void clearFilter() {
        searchField.clear();
        applyCurrentFilter(false);
    }

    @FXML
    private void showAddProductDialog() {
        ProductFormFields fields = createProductForm(null, false);
        Dialog<Boolean> dialog = createProductFormDialog("Thêm sản phẩm", "Lưu sản phẩm", fields);
        ButtonType saveType = dialog.getDialogPane().getButtonTypes().get(0);
        Node saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            ProductFormData data = readProductForm(fields, null);
            if (data == null || !saveNewProduct(data)) {
                event.consume();
            }
        });
        dialog.showAndWait();
    }

    @FXML
    private void showUpdateProductDialog() {
        Product selectedProduct = requireSelectedProduct();
        if (selectedProduct == null) {
            return;
        }

        ProductFormFields fields = createProductForm(selectedProduct, true);
        Dialog<Boolean> dialog = createProductFormDialog("Cập nhật thông tin sản phẩm", "Lưu cập nhật", fields);
        ButtonType saveType = dialog.getDialogPane().getButtonTypes().get(0);
        Node saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            ProductFormData data = readProductForm(fields, selectedProduct);
            if (data == null || !saveUpdatedProduct(data)) {
                event.consume();
            }
        });
        dialog.showAndWait();
    }

    @FXML
    private void showProductDetails() {
        Product selectedProduct = requireSelectedProduct();
        if (selectedProduct == null) {
            return;
        }

        ProductMetadata metadata = metadataFor(selectedProduct);
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Xem chi tiết sản phẩm");
        dialog.setHeaderText(selectedProduct.getCode() + " - " + selectedProduct.getName());
        ButtonType closeType = new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeType);
        dialog.getDialogPane().setPrefWidth(720);
        attachDialogStyles(dialog);
        dialog.getDialogPane().lookupButton(closeType).getStyleClass().add("secondary-action-button");

        VBox detailGrid = createProductDetailGrid(selectedProduct, metadata);
        VBox infoSection = createDialogSection("Thông tin chung sản phẩm", detailGrid);

        TableView<ProductRecipe> recipeTable = createDetailRecipeTable();
        try {
            recipeTable.setItems(FXCollections.observableArrayList(productCatalogService.loadRecipeRows(selectedProduct.getCode())));
        } catch (Exception e) {
            recipeTable.setItems(FXCollections.observableArrayList());
            AlertUtils.showError("Không thể tải định mức sản phẩm từ cơ sở dữ liệu.");
            e.printStackTrace();
        }
        recipeTable.setPrefHeight(190);
        VBox recipeSection = createDialogSection("Danh sách định mức nguyên liệu", recipeTable);

        VBox content = new VBox(12, infoSection, recipeSection);
        content.setPadding(new Insets(4, 0, 0, 0));
        content.getStyleClass().add("product-detail-dialog-content");
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    @FXML
    private void showRecipeDialog() {
        Product selectedProduct = requireSelectedProduct();
        if (selectedProduct == null) {
            return;
        }

        ObservableList<RecipeEditorRow> workingRows = FXCollections.observableArrayList();
        try {
            workingRows.setAll(productCatalogService.loadRecipeRows(selectedProduct.getCode()).stream()
                    .map(RecipeEditorRow::fromRecipe)
                    .toList());
        } catch (Exception e) {
            AlertUtils.showError("Không thể tải định mức sản phẩm từ cơ sở dữ liệu.");
            e.printStackTrace();
            return;
        }
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Cập nhật định mức");
        dialog.setHeaderText(selectedProduct.getName());
        ButtonType saveType = new ButtonType("Lưu định mức", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, cancelType);
        dialog.getDialogPane().setPrefWidth(840);
        attachDialogStyles(dialog);

        TableView<RecipeEditorRow> recipeTable = createEditableRecipeTable(workingRows);

        IngredientSearchControl ingredientSearch = createIngredientSearchControl();

        TextField quantityField = new TextField();
        quantityField.setPromptText("Định mức");
        quantityField.getStyleClass().add("page-input");

        ComboBox<String> unitCombo = new ComboBox<>(FXCollections.observableArrayList("g", "ml", "kg", "l"));
        unitCombo.getSelectionModel().select("g");
        unitCombo.setMaxWidth(Double.MAX_VALUE);
        unitCombo.getStyleClass().add("page-combo");

        Button addIngredientButton = new Button("Thêm nguyên liệu");
        addIngredientButton.getStyleClass().add("primary-action-button");
        addIngredientButton.setOnAction(event -> addIngredientToRecipe(workingRows, recipeTable, ingredientSearch, quantityField, unitCombo));

        Button deleteIngredientButton = new Button("Xóa nguyên liệu");
        deleteIngredientButton.getStyleClass().add("danger-button");
        deleteIngredientButton.setOnAction(event -> deleteSelectedRecipeRow(workingRows, recipeTable));

        GridPane addForm = new GridPane();
        addForm.setHgap(12);
        addForm.setVgap(10);
        addForm.getStyleClass().add("product-recipe-form");
        addForm.add(label("Nguyên liệu"), 0, 0);
        addForm.add(label("Định mức"), 1, 0);
        addForm.add(label("Đơn vị tính"), 2, 0);
        addForm.add(ingredientSearch.field(), 0, 1);
        addForm.add(quantityField, 1, 1);
        addForm.add(unitCombo, 2, 1);
        GridPane.setHgrow(ingredientSearch.field(), Priority.ALWAYS);
        GridPane.setHgrow(quantityField, Priority.ALWAYS);
        GridPane.setHgrow(unitCombo, Priority.ALWAYS);

        HBox rowActions = new HBox(10, addIngredientButton, deleteIngredientButton);
        rowActions.setAlignment(Pos.CENTER_LEFT);
        VBox content = new VBox(14, recipeTable, addForm, rowActions);
        content.setPadding(new Insets(8, 0, 0, 0));
        dialog.getDialogPane().setContent(content);

        Node saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.getStyleClass().add("primary-action-button");
        dialog.getDialogPane().lookupButton(cancelType).getStyleClass().add("secondary-action-button");
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!validatePendingIngredientSearch(ingredientSearch)) {
                event.consume();
                return;
            }
            if (!saveRecipeRows(selectedProduct, workingRows)) {
                event.consume();
            }
        });
        dialog.setOnHidden(event -> ingredientSearch.hideSuggestions());
        dialog.showAndWait();
    }

    private Dialog<Boolean> createProductFormDialog(String title, String saveText, ProductFormFields fields) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(title);
        ButtonType saveType = new ButtonType(saveText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, cancelType);
        dialog.getDialogPane().setPrefWidth(590);
        attachDialogStyles(dialog);
        dialog.getDialogPane().setContent(fields.container());
        dialog.getDialogPane().lookupButton(saveType).getStyleClass().add("primary-action-button");
        dialog.getDialogPane().lookupButton(cancelType).getStyleClass().add("secondary-action-button");
        dialog.setResultConverter(button -> button != null && button.getButtonData() == ButtonBar.ButtonData.OK_DONE);
        return dialog;
    }

    private ProductFormFields createProductForm(Product product, boolean updateMode) {
        ProductMetadata metadata = product == null
                ? new ProductMetadata("", LocalDateTime.now(), null)
                : metadataFor(product);

        TextField codeField = new TextField(product == null ? generateNextProductCode() : product.getCode());
        codeField.setPromptText("Mã sản phẩm");
        codeField.setEditable(false);
        codeField.getStyleClass().addAll("page-input", "readonly-code-field");

        TextField nameField = new TextField(product == null ? "" : product.getName());
        nameField.setPromptText("Tên sản phẩm");
        nameField.getStyleClass().add("page-input");

        TextField priceField = new TextField(product == null ? "" : formatQuantity(product.getPrice()));
        priceField.setPromptText("Giá bán");
        priceField.getStyleClass().add("page-input");

        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList(STATUS_ACTIVE, STATUS_INACTIVE));
        statusCombo.getSelectionModel().select(product == null || product.getTrangThai() == 1 ? STATUS_ACTIVE : STATUS_INACTIVE);
        statusCombo.setMaxWidth(Double.MAX_VALUE);
        statusCombo.getStyleClass().add("page-combo");

        ComboBox<ProductCategory> categoryCombo = new ComboBox<>(categories);
        categoryCombo.setConverter(categoryConverter());
        categoryCombo.setMaxWidth(Double.MAX_VALUE);
        categoryCombo.getStyleClass().add("page-combo");
        selectCategory(categoryCombo, product == null ? null : product.getDanhMucId());

        TextArea noteArea = new TextArea(metadata.note());
        noteArea.setPromptText("Ghi chú nội bộ cho sản phẩm");
        noteArea.setPrefRowCount(3);
        noteArea.setWrapText(true);
        noteArea.getStyleClass().add("page-text-area");

        DatePicker publishedDatePicker = new DatePicker(toLocalDate(metadata.publishedAt(), product));
        publishedDatePicker.getStyleClass().add("page-date");

        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(12);
        form.getStyleClass().add("product-dialog-form");
        form.add(label("Mã sản phẩm"), 0, 0);
        form.add(codeField, 1, 0);
        form.add(label("Tên sản phẩm"), 0, 1);
        form.add(nameField, 1, 1);
        form.add(label("Giá bán"), 0, 2);
        form.add(priceField, 1, 2);
        form.add(label("Trạng thái KD"), 0, 3);
        form.add(statusCombo, 1, 3);
        form.add(label("Danh mục SP"), 0, 4);
        form.add(categoryCombo, 1, 4);
        form.add(label("Ngày phát hành"), 0, 5);
        form.add(publishedDatePicker, 1, 5);
        form.add(label("Ghi chú"), 0, 6);
        form.add(noteArea, 1, 6);
        GridPane.setHgrow(codeField, Priority.ALWAYS);
        GridPane.setHgrow(nameField, Priority.ALWAYS);
        GridPane.setHgrow(priceField, Priority.ALWAYS);

        return new ProductFormFields(form, codeField, nameField, priceField, statusCombo, categoryCombo, noteArea, publishedDatePicker);
    }

    private ProductFormData readProductForm(ProductFormFields fields, Product originalProduct) {
        String code = originalProduct == null ? generateNextProductCode() : originalProduct.getCode();
        String name = safe(fields.nameField().getText());
        String status = fields.statusCombo().getValue();
        BigDecimal price = parsePositiveNumber(fields.priceField().getText());

        if (name.isEmpty()) {
            AlertUtils.showWarning("Tên sản phẩm không được rỗng.");
            return null;
        }
        if (price == null) {
            AlertUtils.showWarning("Giá bán không hợp lệ.");
            return null;
        }
        if (safe(status).isEmpty()) {
            AlertUtils.showWarning("Trạng thái không được rỗng.");
            return null;
        }

        ProductCategory category = fields.categoryCombo().getValue();
        LocalDate publishedDate = fields.publishedDatePicker().getValue();
        LocalDateTime publishedAt = publishedDate == null ? null : publishedDate.atStartOfDay();
        ProductMetadata oldMetadata = originalProduct == null ? null : metadataFor(originalProduct);
        LocalDateTime stoppedAt = oldMetadata == null ? null : oldMetadata.stoppedAt();
        if (STATUS_INACTIVE.equals(status) && stoppedAt == null) {
            stoppedAt = LocalDateTime.now();
        }

        Product product = new Product(
                code,
                category == null ? null : category.getDanhMucId(),
                name,
                originalProduct == null ? DEFAULT_PRODUCT_TYPE : originalProduct.getLoaiSanPham(),
                originalProduct == null ? DEFAULT_PRODUCT_UNIT : originalProduct.getDonViTinh(),
                price,
                originalProduct == null ? BigDecimal.ZERO : originalProduct.getGiaVon(),
                statusToInt(status),
                publishedAt,
                LocalDateTime.now()
        );
        return new ProductFormData(product, new ProductMetadata(safe(fields.noteArea().getText()), publishedAt, stoppedAt));
    }

    private boolean saveNewProduct(ProductFormData data) {
        if (productCodeExists(data.product().getCode())) {
            AlertUtils.showWarning("Không thể sinh mã sản phẩm hợp lệ. Vui lòng thử lại.");
            return false;
        }

        try {
            productCatalogService.saveProduct(data.product());
            loadProducts(data.product().getCode());
            AlertUtils.showInfo("Thêm sản phẩm thành công");
            return true;
        } catch (Exception e) {
            AlertUtils.showError(readableError(e));
            return false;
        }
    }

    private boolean saveUpdatedProduct(ProductFormData data) {
        try {
            productCatalogService.saveProduct(data.product());
            loadProducts(data.product().getCode());
            AlertUtils.showInfo("Cập nhật sản phẩm thành công");
            return true;
        } catch (Exception e) {
            AlertUtils.showError(readableError(e));
            return false;
        }
    }

    private boolean saveRecipeRows(Product selectedProduct, ObservableList<RecipeEditorRow> workingRows) {
        Set<String> materialCodes = new HashSet<>();
        for (RecipeEditorRow row : workingRows) {
            if (!materialCodes.add(row.getMaterialCode())) {
                AlertUtils.showWarning("Nguyên liệu này đã tồn tại trong công thức");
                return false;
            }
            if (parsePositiveNumber(row.getQuantity()) == null) {
                AlertUtils.showWarning("Định mức không hợp lệ.");
                return false;
            }
        }

        try {
            productCatalogService.replaceRecipeRows(selectedProduct.getCode(), workingRows.stream()
                .map(row -> new ProductRecipe(
                        row.getMaterialCode(),
                        row.getMaterialName(),
                        parsePositiveNumber(row.getQuantity()),
                        row.getUnit(),
                        row.getStatus()
                ))
                .toList());
        productTable.refresh();
        AlertUtils.showInfo("Cập nhật định mức thành công");
        return true;
        } catch (Exception e) {
            AlertUtils.showError(readableError(e));
            e.printStackTrace();
            return false;
        }
    }

    private IngredientSearchControl createIngredientSearchControl() {
        TextField ingredientField = new TextField();
        ingredientField.setPromptText("Nhập mã hoặc tên nguyên liệu...");
        ingredientField.setMaxWidth(Double.MAX_VALUE);
        ingredientField.getStyleClass().addAll("page-input", "product-ingredient-search");

        ObjectProperty<Ingredient> selectedIngredient = new SimpleObjectProperty<>();
        ObservableList<Ingredient> suggestions = FXCollections.observableArrayList();
        ListView<Ingredient> suggestionList = new ListView<>(suggestions);
        suggestionList.setFocusTraversable(false);
        suggestionList.setPlaceholder(new Label("Không tìm thấy nguyên liệu phù hợp"));
        suggestionList.getStyleClass().add("ingredient-suggestion-list");
        Optional.ofNullable(getClass().getResource("/com/phungloccoffee/gui/css/pages.css"))
                .ifPresent(resource -> suggestionList.getStylesheets().add(resource.toExternalForm()));
        suggestionList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Ingredient ingredient, boolean empty) {
                super.updateItem(ingredient, empty);
                setText(empty || ingredient == null ? null : ingredientDisplay(ingredient));
            }
        });

        Popup suggestionPopup = new Popup();
        suggestionPopup.setAutoHide(false);
        suggestionPopup.getContent().add(suggestionList);
        PauseTransition hideDelay = new PauseTransition(Duration.millis(160));
        IngredientSearchControl control = new IngredientSearchControl(ingredientField, selectedIngredient, suggestionPopup);

        ingredientField.textProperty().addListener((observable, oldText, newText) -> {
            Ingredient selected = selectedIngredient.get();
            if (selected != null && !ingredientDisplay(selected).equals(newText)) {
                selectedIngredient.set(null);
            }
            updateIngredientSuggestions(ingredientField, suggestionList, suggestionPopup, newText);
        });
        ingredientField.focusedProperty().addListener((observable, wasFocused, focused) -> {
            if (focused) {
                hideDelay.stop();
                updateIngredientSuggestions(ingredientField, suggestionList, suggestionPopup, ingredientField.getText());
            } else {
                hideDelay.setOnFinished(event -> suggestionPopup.hide());
                hideDelay.playFromStart();
            }
        });
        suggestionList.setOnMousePressed(event -> hideDelay.stop());
        suggestionList.setOnMouseClicked(event -> {
            Ingredient ingredient = suggestionList.getSelectionModel().getSelectedItem();
            if (ingredient == null) {
                return;
            }
            selectedIngredient.set(ingredient);
            ingredientField.setText(ingredientDisplay(ingredient));
            ingredientField.positionCaret(ingredientField.getText().length());
            suggestionPopup.hide();
        });

        return control;
    }

    private void updateIngredientSuggestions(TextField ingredientField,
                                             ListView<Ingredient> suggestionList,
                                             Popup suggestionPopup,
                                             String keyword) {
        String normalizedKeyword = normalize(keyword);
        if (!ingredientField.isFocused() || normalizedKeyword.isBlank()) {
            suggestionPopup.hide();
            return;
        }

        List<Ingredient> matches;
        try {
            matches = productCatalogService.loadIngredients().stream()
                .filter(ingredient -> normalize(ingredient.code()).contains(normalizedKeyword)
                        || normalize(ingredient.name()).contains(normalizedKeyword)
                        || normalize(ingredientDisplay(ingredient)).contains(normalizedKeyword))
                .limit(8)
                .toList();
        } catch (Exception e) {
            suggestionList.getItems().clear();
            suggestionPopup.hide();
            e.printStackTrace();
            return;
        }
        suggestionList.getItems().setAll(matches);
        suggestionList.setPrefWidth(Math.max(ingredientField.getWidth(), 280));
        suggestionList.setPrefHeight(matches.isEmpty() ? 44 : Math.min(matches.size(), 8) * 36 + 2);

        Bounds screenBounds = ingredientField.localToScreen(ingredientField.getBoundsInLocal());
        if (screenBounds == null) {
            return;
        }
        if (suggestionPopup.isShowing()) {
            suggestionPopup.setX(screenBounds.getMinX());
            suggestionPopup.setY(screenBounds.getMaxY() + 4);
        } else {
            suggestionPopup.show(ingredientField, screenBounds.getMinX(), screenBounds.getMaxY() + 4);
        }
    }

    private boolean validatePendingIngredientSearch(IngredientSearchControl ingredientSearch) {
        if (!safe(ingredientSearch.field().getText()).isEmpty()
                && ingredientSearch.selectedIngredient().get() == null) {
            AlertUtils.showWarning("Vui lòng chọn nguyên liệu hợp lệ");
            return false;
        }
        return true;
    }

    private void addIngredientToRecipe(ObservableList<RecipeEditorRow> workingRows,
                                       TableView<RecipeEditorRow> recipeTable,
                                       IngredientSearchControl ingredientSearch,
                                       TextField quantityField,
                                       ComboBox<String> unitCombo) {
        Ingredient ingredient = ingredientSearch.selectedIngredient().get();
        BigDecimal quantity = parsePositiveNumber(quantityField.getText());
        String unit = unitCombo.getValue();
        if (ingredient == null) {
            AlertUtils.showWarning("Vui lòng chọn nguyên liệu hợp lệ");
            return;
        }
        if (quantity == null) {
            AlertUtils.showWarning("Định mức không hợp lệ.");
            return;
        }
        if (safe(unit).isEmpty()) {
            AlertUtils.showWarning("Đơn vị tính không được rỗng.");
            return;
        }
        boolean exists = workingRows.stream().anyMatch(row -> row.getMaterialCode().equals(ingredient.code()));
        if (exists) {
            AlertUtils.showWarning("Nguyên liệu này đã tồn tại trong công thức");
            return;
        }

        RecipeEditorRow row = new RecipeEditorRow(ingredient.code(), ingredient.name(),
                quantity.stripTrailingZeros().toPlainString(), unit, DEFAULT_RECIPE_STATUS);
        workingRows.add(row);
        recipeTable.getSelectionModel().select(row);
        recipeTable.scrollTo(row);
        quantityField.clear();
        ingredientSearch.clear();
    }

    private void deleteSelectedRecipeRow(ObservableList<RecipeEditorRow> workingRows, TableView<RecipeEditorRow> recipeTable) {
        RecipeEditorRow selectedRow = recipeTable.getSelectionModel().getSelectedItem();
        if (selectedRow == null) {
            AlertUtils.showWarning("Vui lòng chọn nguyên liệu trong công thức.");
            return;
        }
        workingRows.remove(selectedRow);
    }

    private TableView<ProductRecipe> createDetailRecipeTable() {
        TableView<ProductRecipe> table = new TableView<>();
        table.getStyleClass().addAll("page-table", "product-dialog-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Sản phẩm chưa có định mức nguyên liệu"));

        TableColumn<ProductRecipe, String> materialColumn = new TableColumn<>("Tên nguyên liệu");
        materialColumn.setPrefWidth(280);
        materialColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().materialName()));

        TableColumn<ProductRecipe, BigDecimal> quantityColumn = new TableColumn<>("Định mức");
        quantityColumn.setPrefWidth(120);
        quantityColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().quantity()));
        quantityColumn.setCellFactory(column -> new QuantityCell<>());

        TableColumn<ProductRecipe, String> unitColumn = new TableColumn<>("Đơn vị tính");
        unitColumn.setPrefWidth(120);
        unitColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().unit()));

        table.getColumns().setAll(materialColumn, quantityColumn, unitColumn);
        return table;
    }

    private TableView<RecipeEditorRow> createEditableRecipeTable(ObservableList<RecipeEditorRow> rows) {
        TableView<RecipeEditorRow> table = new TableView<>(rows);
        table.setEditable(true);
        table.setPrefHeight(280);
        table.getStyleClass().addAll("page-table", "product-dialog-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Sản phẩm chưa có định mức nguyên liệu"));

        TableColumn<RecipeEditorRow, String> materialColumn = new TableColumn<>("Tên nguyên liệu");
        materialColumn.setPrefWidth(260);
        materialColumn.setCellValueFactory(data -> data.getValue().materialNameProperty());

        TableColumn<RecipeEditorRow, String> quantityColumn = new TableColumn<>("Định mức");
        quantityColumn.setPrefWidth(120);
        quantityColumn.setCellValueFactory(data -> data.getValue().quantityProperty());
        quantityColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        quantityColumn.setOnEditCommit(event -> event.getRowValue().setQuantity(event.getNewValue()));

        TableColumn<RecipeEditorRow, String> unitColumn = new TableColumn<>("Đơn vị tính");
        unitColumn.setPrefWidth(110);
        unitColumn.setCellValueFactory(data -> data.getValue().unitProperty());
        unitColumn.setCellFactory(ComboBoxTableCell.forTableColumn("g", "ml", "kg", "l"));
        unitColumn.setOnEditCommit(event -> event.getRowValue().setUnit(event.getNewValue()));

        TableColumn<RecipeEditorRow, String> statusColumn = new TableColumn<>("Trạng thái");
        statusColumn.setPrefWidth(150);
        statusColumn.setCellValueFactory(data -> data.getValue().statusProperty());
        statusColumn.setCellFactory(ComboBoxTableCell.forTableColumn("Đang áp dụng", "Cần rà soát"));
        statusColumn.setOnEditCommit(event -> event.getRowValue().setStatus(event.getNewValue()));

        table.getColumns().setAll(materialColumn, quantityColumn, unitColumn, statusColumn);
        return table;
    }

    private Product requireSelectedProduct() {
        Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            AlertUtils.showWarning("Vui lòng chọn sản phẩm trước");
        }
        return selectedProduct;
    }

    private void selectProductByCode(String productCode) {
        if (safe(productCode).isEmpty()) {
            return;
        }
        for (Product product : filteredProducts) {
            if (product.getCode().equalsIgnoreCase(productCode)) {
                productTable.getSelectionModel().select(product);
                productTable.scrollTo(product);
                return;
            }
        }
    }

    private void updateResultCount() {
        resultCountLabel.setText("Hiển thị " + filteredProducts.size() + "/" + products.size() + " sản phẩm");
    }

    private ProductMetadata metadataFor(Product product) {
        ProductMetadata metadata = productCatalogService.metadataFor(product);
        LocalDateTime publishedAt = metadata.publishedAt() == null ? product.getCreatedAt() : metadata.publishedAt();
        LocalDateTime stoppedAt = metadata.stoppedAt();
        if (product.getTrangThai() == 0 && stoppedAt == null) {
            stoppedAt = product.getUpdatedAt();
        }
        return new ProductMetadata(metadata.note(), publishedAt, stoppedAt);
    }

    private String categoryNameFor(Product product) {
        return categories.stream()
                .filter(category -> safe(product.getDanhMucId()).equals(category.getDanhMucId()))
                .map(ProductCategory::getTenDanhMuc)
                .findFirst()
                .orElse(productCatalogService.categoryName(product.getDanhMucId(), categories));
    }

    private String recipeStatusFor(Product product) {
        try {
            return productCatalogService.hasRecipe(product.getCode()) ? "Đã có" : "Chưa có";
        } catch (Exception e) {
            e.printStackTrace();
            return "Chưa có";
        }
    }

    private String generateNextProductCode() {
        return AutoCodeGenerator.generateNextCode("SP", products.stream().map(Product::getCode).toList());
    }

    private boolean productCodeExists(String productCode) {
        return products.stream().anyMatch(product -> product.getCode().equalsIgnoreCase(productCode));
    }

    private void selectCategory(ComboBox<ProductCategory> comboBox, String categoryId) {
        Optional<ProductCategory> category = categories.stream()
                .filter(item -> safe(categoryId).equals(item.getDanhMucId()))
                .findFirst();
        if (category.isPresent()) {
            comboBox.getSelectionModel().select(category.get());
        } else if (!categories.isEmpty()) {
            comboBox.getSelectionModel().selectFirst();
        }
    }

    private StringConverter<ProductCategory> categoryConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(ProductCategory category) {
                return category == null ? "" : safe(category.getTenDanhMuc());
            }

            @Override
            public ProductCategory fromString(String value) {
                return categories.stream()
                        .filter(category -> safe(category.getTenDanhMuc()).equals(value))
                        .findFirst()
                        .orElse(null);
            }
        };
    }

    private VBox createProductDetailGrid(Product product, ProductMetadata metadata) {
        VBox table = new VBox();
        table.getStyleClass().add("detail-info-grid");
        table.getChildren().addAll(
                detailInfoRow("Mã sản phẩm", detailTextValue(product.getCode()), false),
                detailInfoRow("Tên sản phẩm", detailTextValue(product.getName()), true),
                detailInfoRow("Giá bán", detailTextValue(formatMoney(product.getPrice())), false),
                detailInfoRow("Trạng thái KD", createStatusBadge(product.getStatus()), true),
                detailInfoRow("Danh mục SP", detailTextValue(categoryNameFor(product)), false),
                detailInfoRow("Định mức", createRecipeBadge(recipeStatusFor(product)), true),
                detailInfoRow("Ngày phát hành", detailTextValue(formatDateTime(metadata.publishedAt())), false),
                detailInfoRow("Ngày ngừng bán", createMutedValueBadge(formatDateTime(metadata.stoppedAt())), true),
                detailInfoRow("Ghi chú", detailTextValue(metadata.note()), false)
        );
        return table;
    }

    private VBox createDialogSection(String title, Node content) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("product-dialog-section-title");
        VBox section = new VBox(12, titleLabel, content);
        section.getStyleClass().add("product-dialog-section");
        return section;
    }

    private HBox detailInfoRow(String label, Node valueNode, boolean alternate) {
        Label labelNode = new Label(label);
        labelNode.setMinWidth(146);
        labelNode.setPrefWidth(146);
        labelNode.setMaxWidth(146);
        labelNode.setTextOverrun(OverrunStyle.CLIP);
        labelNode.getStyleClass().add("detail-info-label");

        HBox valueCell = new HBox(valueNode);
        valueCell.setAlignment(Pos.CENTER_LEFT);
        valueCell.setMaxWidth(Double.MAX_VALUE);
        valueCell.getStyleClass().add("detail-info-value-cell");
        HBox.setHgrow(valueNode, Priority.ALWAYS);
        HBox.setHgrow(valueCell, Priority.ALWAYS);

        HBox row = new HBox(labelNode, valueCell);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("detail-info-row");
        if (alternate) {
            row.getStyleClass().add("detail-info-row-alt");
        }
        return row;
    }

    private Label detailTextValue(String value) {
        Label label = new Label(safe(value).isEmpty() ? "Chưa có dữ liệu" : value);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setTextOverrun(OverrunStyle.CLIP);
        label.getStyleClass().add("detail-info-value");
        return label;
    }

    private Label createStatusBadge(String status) {
        Label badge = new Label(status);
        badge.getStyleClass().addAll("status-badge", "product-status-badge",
                STATUS_ACTIVE.equals(status) ? "status-success" : "status-danger");
        return badge;
    }

    private Label createRecipeBadge(String status) {
        Label badge = new Label(status);
        badge.getStyleClass().addAll("status-badge", "recipe-status-badge",
                "Đã có".equals(status) ? "status-success" : "status-warning");
        return badge;
    }

    private Label createMutedValueBadge(String value) {
        Label badge = new Label(value);
        badge.getStyleClass().addAll("status-badge", "Chưa có dữ liệu".equals(value) ? "status-neutral" : "status-info");
        return badge;
    }

    private Label label(String text) {
        Label label = new Label(text);
        label.setMinWidth(122);
        label.setPrefWidth(122);
        label.setMaxWidth(122);
        label.setTextOverrun(OverrunStyle.CLIP);
        label.getStyleClass().add("form-label");
        return label;
    }

    private String ingredientDisplay(Ingredient ingredient) {
        return ingredient == null ? "" : ingredient.code() + " - " + ingredient.name();
    }

    private void attachDialogStyles(Dialog<?> dialog) {
        Optional.ofNullable(getClass().getResource("/com/phungloccoffee/gui/css/pages.css"))
                .ifPresent(resource -> dialog.getDialogPane().getStylesheets().add(resource.toExternalForm()));
        dialog.getDialogPane().getStyleClass().add("product-dialog-pane");
    }

    private BigDecimal parsePositiveNumber(String value) {
        BigDecimal number = parseDecimal(value);
        return number == null || number.compareTo(BigDecimal.ZERO) <= 0 ? null : number;
    }

    private BigDecimal parseDecimal(String value) {
        String normalizedValue = safe(value)
                .replace("₫", "")
                .replace("đ", "")
                .replace(" ", "");
        if (normalizedValue.isEmpty()) {
            return null;
        }
        if (normalizedValue.matches("\\d{1,3}(\\.\\d{3})+(,\\d+)?")) {
            normalizedValue = normalizedValue.replace(".", "").replace(",", ".");
        } else if (normalizedValue.matches("\\d{1,3}(,\\d{3})+(\\.\\d+)?")) {
            normalizedValue = normalizedValue.replace(",", "");
        } else if (normalizedValue.contains(",")) {
            normalizedValue = normalizedValue.replace(",", ".");
        }
        try {
            return new BigDecimal(normalizedValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int statusToInt(String status) {
        return STATUS_ACTIVE.equals(status) ? 1 : 0;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "Chưa có dữ liệu" : value.format(DATE_TIME_FORMATTER);
    }

    private String formatQuantity(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private static String formatMoney(BigDecimal value) {
        if (value == null) {
            return "";
        }
        long roundedValue = value.setScale(0, RoundingMode.HALF_UP).longValue();
        return String.format(Locale.GERMANY, "%,d", roundedValue) + " đ";
    }

    private LocalDate toLocalDate(LocalDateTime value, Product product) {
        LocalDateTime resolved = value == null && product != null ? product.getCreatedAt() : value;
        return resolved == null ? LocalDate.now() : resolved.toLocalDate();
    }

    private String normalize(String value) {
        String lower = safe(value).toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.replace("đ", "d");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String readableError(Exception exception) {
        String message = exception.getMessage();
        return safe(message).isEmpty() ? "Không thể thực hiện thao tác." : message;
    }

    private record ProductFormFields(Node container, TextField codeField, TextField nameField,
                                     TextField priceField, ComboBox<String> statusCombo,
                                     ComboBox<ProductCategory> categoryCombo, TextArea noteArea,
                                     DatePicker publishedDatePicker) {
    }

    private record ProductFormData(Product product, ProductMetadata metadata) {
    }

    private record IngredientSearchControl(TextField field, ObjectProperty<Ingredient> selectedIngredient,
                                           Popup suggestionPopup) {
        private void clear() {
            selectedIngredient.set(null);
            field.clear();
            hideSuggestions();
        }

        private void hideSuggestions() {
            suggestionPopup.hide();
        }
    }

    public static class RecipeEditorRow {
        private final SimpleStringProperty materialCode;
        private final SimpleStringProperty materialName;
        private final SimpleStringProperty quantity;
        private final SimpleStringProperty unit;
        private final SimpleStringProperty status;

        public RecipeEditorRow(String materialCode, String materialName, String quantity, String unit, String status) {
            this.materialCode = new SimpleStringProperty(materialCode);
            this.materialName = new SimpleStringProperty(materialName);
            this.quantity = new SimpleStringProperty(quantity);
            this.unit = new SimpleStringProperty(unit);
            this.status = new SimpleStringProperty(status);
        }

        public static RecipeEditorRow fromRecipe(ProductRecipe recipe) {
            return new RecipeEditorRow(
                    recipe.materialCode(),
                    recipe.materialName(),
                    recipe.quantity().stripTrailingZeros().toPlainString(),
                    recipe.unit(),
                    recipe.status()
            );
        }

        public SimpleStringProperty materialNameProperty() {
            return materialName;
        }

        public SimpleStringProperty quantityProperty() {
            return quantity;
        }

        public SimpleStringProperty unitProperty() {
            return unit;
        }

        public SimpleStringProperty statusProperty() {
            return status;
        }

        public String getMaterialCode() {
            return materialCode.get();
        }

        public String getMaterialName() {
            return materialName.get();
        }

        public String getQuantity() {
            return quantity.get();
        }

        public void setQuantity(String value) {
            quantity.set(value);
        }

        public String getUnit() {
            return unit.get();
        }

        public void setUnit(String value) {
            unit.set(value);
        }

        public String getStatus() {
            return status.get();
        }

        public void setStatus(String value) {
            status.set(value);
        }
    }

    private static class PriceCell<T> extends TableCell<T, BigDecimal> {
        @Override
        protected void updateItem(BigDecimal price, boolean empty) {
            super.updateItem(price, empty);
            setText(empty || price == null ? null : formatMoney(price));
            setGraphic(null);
            setAlignment(Pos.CENTER_LEFT);
            getStyleClass().removeAll("cell-price");
            if (!empty && price != null) {
                getStyleClass().add("cell-price");
            }
        }
    }

    private static class QuantityCell<T> extends TableCell<T, BigDecimal> {
        @Override
        protected void updateItem(BigDecimal quantity, boolean empty) {
            super.updateItem(quantity, empty);
            setText(empty || quantity == null ? null : quantity.stripTrailingZeros().toPlainString());
            setGraphic(null);
            setAlignment(Pos.CENTER_LEFT);
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
            badge.getStyleClass().addAll("status-badge", "product-status-badge",
                    STATUS_ACTIVE.equals(status) ? "status-success" : "status-danger");
            setGraphic(badge);
            setText(null);
            setAlignment(Pos.CENTER_LEFT);
        }
    }

    private static class RecipeStatusCell<T> extends TableCell<T, String> {
        @Override
        protected void updateItem(String status, boolean empty) {
            super.updateItem(status, empty);
            if (empty || status == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label badge = new Label(status);
            badge.getStyleClass().addAll("status-badge", "recipe-status-badge",
                    "Đã có".equals(status) ? "status-success" : "status-warning");
            setGraphic(badge);
            setText(null);
            setAlignment(Pos.CENTER_LEFT);
        }
    }
}
