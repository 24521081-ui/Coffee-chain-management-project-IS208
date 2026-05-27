package com.phungloccoffee.gui.controller.pos;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.phungloccoffee.App;
import com.phungloccoffee.bus.POSBUS;
import com.phungloccoffee.bus.ProductBUS;
import com.phungloccoffee.gui.model.AppUserSession;
import com.phungloccoffee.gui.model.OrderItem;
import com.phungloccoffee.gui.model.ProductOption;
import com.phungloccoffee.gui.model.ProductOption.ProductStatus;
import com.phungloccoffee.gui.model.ToppingItem;
import com.phungloccoffee.gui.service.SessionManager;
import com.phungloccoffee.model.Order;
import com.phungloccoffee.model.OrderDetail;
import com.phungloccoffee.model.OrderDetailTopping;
import com.phungloccoffee.model.Product;
import com.phungloccoffee.offline.NetworkMonitor;
import com.phungloccoffee.offline.OfflineStorage;
import com.phungloccoffee.offline.SyncService;
import com.phungloccoffee.util.AlertUtils;
import com.phungloccoffee.util.CurrencyFormatter;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class POSMainController {
    private static final String CATEGORY_ALL = "Tất cả";
    private static final List<String> SUGAR_LEVELS = List.of("0%", "30%", "50%", "70%", "100%");
    private static final List<String> ICE_LEVELS = List.of("Không đá", "Ít đá", "Bình thường", "Nhiều đá");

    @FXML private TextField searchProductField;
    @FXML private HBox categoryChipBox;
    @FXML private TilePane productTilePane;
    @FXML private VBox cartItemsBox;
    @FXML private Label totalLabel;
    @FXML private Label notificationLabel;
    @FXML private Label pendingSyncCountLabel;
    @FXML private StackPane customizeOverlay;
    @FXML private StackPane customizeDialogContainer;

    private final List<ProductOption> products = new ArrayList<>();
    private final List<ProductOption> toppingCatalog = new ArrayList<>();
    private final List<OrderItem> cartItems = new ArrayList<>();
    private final List<ToppingSelection> toppingSelections = new ArrayList<>();
    private final ProductBUS productBUS = new ProductBUS();
    private final POSBUS posBUS = new POSBUS();
    private final NetworkMonitor networkMonitor = NetworkMonitor.getInstance();
    private final OfflineStorage offlineStorage = OfflineStorage.getInstance();
    private final SyncService syncService = new SyncService();
    private final List<String> categories = new ArrayList<>();

    private String selectedCategory = CATEGORY_ALL;
    private AppUserSession currentUser;
    private ProductOption currentProduct;
    private OrderItem editingItem;
    private int currentQuantity = 1;
    private Label quantityValueLabel;
    private ToggleGroup sugarToggleGroup;
    private ToggleGroup iceToggleGroup;
    private TextArea noteTextArea;
    private Label dialogSubtotalLabel;
    private PauseTransition notificationDelay;

    @FXML
    private void initialize() {
        if (!SessionManager.isLoggedIn()) {
            showInlineMessage("Vui lòng đăng nhập trước khi bán hàng.");
            productTilePane.getChildren().setAll(createEmptyLabel("Chưa đăng nhập"));
            return;
        }
        currentUser = SessionManager.getCurrentUser();
        configureConnectivity();
        initProductData();
        initCategoryChips();
        searchProductField.textProperty().addListener((observable, oldValue, newValue) -> filterProducts());
        filterProducts();
        renderCartItems();
    }

    @FXML
    private void addSelectedProduct() {
        // Kept for backward compatibility with older FXML actions.
    }

    @FXML
    private void removeSelectedProduct() {
        if (!cartItems.isEmpty()) {
            handleRemoveCartItem(cartItems.get(cartItems.size() - 1));
        }
    }

    @FXML
    private void createOrder() {
        handleCreateInvoice();
    }

    @FXML
    private void handleCreateInvoice() {
        if (cartItems.isEmpty()) {
            showInlineMessage("Giỏ hàng đang rỗng.");
            return;
        }
        try {
            Order order = createPendingOrderFromCart();
            clearCartAndRefreshMenu();
            if (isLocalOrder(order.getDonHangId())) {
                AlertUtils.showInfo("Tạo hóa đơn offline thành công. Mã đơn local: " + order.getDonHangId()
                        + "\nĐơn sẽ được đồng bộ khi có mạng.");
            } else {
                AlertUtils.showInfo("Tạo hóa đơn thành công. Mã đơn: " + order.getDonHangId());
            }
            updatePendingSyncCount();
        } catch (Exception e) {
            showInlineMessage(e.getMessage());
            AlertUtils.showError(e.getMessage());
        }
    }

    @FXML
    private void handlePayment() {
        if (cartItems.isEmpty()) {
            showInlineMessage("Giỏ hàng đang rỗng.");
            return;
        }
        try {
            Order order = createPendingOrderFromCart();
            clearCartAndRefreshMenu();
            openPaymentScreen(order.getDonHangId());
            updatePendingSyncCount();
        } catch (Exception e) {
            showInlineMessage(e.getMessage());
            AlertUtils.showError(e.getMessage());
        }
    }

    private Order createPendingOrderFromCart() throws Exception {
        BigDecimal expectedTotal = calculateCartTotal();
        return posBUS.createPendingOrder(toOrderDetails(), expectedTotal);
    }

    private void clearCartAndRefreshMenu() {
        cartItems.clear();
        renderCartItems();
        if (networkMonitor.isOnline()) {
            initProductData();
            initCategoryChips();
        }
        filterProducts();
    }

    private void configureConnectivity() {
        networkMonitor.addListener(online -> Platform.runLater(() -> {
            if (online) {
                syncOfflineOrdersAndRefreshCache();
            }
        }));
        networkMonitor.start();
        updatePendingSyncCount();
        if (networkMonitor.isOnline()) {
            syncOfflineOrdersAndRefreshCache();
        }
    }

    private void syncOfflineOrdersAndRefreshCache() {
        CompletableFuture.runAsync(() -> {
            try {
                syncService.syncPending(currentUser == null ? null : currentUser.getBranchId());
            } catch (Exception e) {
                Platform.runLater(() -> showInlineMessage(e.getMessage()));
            } finally {
                Platform.runLater(this::updatePendingSyncCount);
            }
        });
    }

    private void updatePendingSyncCount() {
        if (pendingSyncCountLabel != null) {
            try {
                pendingSyncCountLabel.setText("Chờ đồng bộ: " + offlineStorage.countPendingOrders());
            } catch (Exception e) {
                pendingSyncCountLabel.setText("Chờ đồng bộ: ?");
            }
        }
    }
    private void initProductData() { 
        products.clear();
        toppingCatalog.clear();
        categories.clear();
        categories.add(CATEGORY_ALL);

        try {
            List<Product> loadedProducts = productBUS.getProductsForPOS(
                    currentUser == null ? null : currentUser.getBranchId());
            for (Product product : loadedProducts) {
                ProductOption option = toProductOption(product);
                if (option.isToppingCategory()) {
                    toppingCatalog.add(option);
                    continue;
                }
                products.add(option);
                if (!safe(option.getCategory()).isBlank() && !categories.contains(option.getCategory())) {
                    categories.add(option.getCategory());
                }
            }
            if (products.isEmpty()) {
                showInlineMessage("Chưa có sản phẩm thành phẩm trong Oracle.");
            }
        } catch (Exception e) {
            products.clear();
            toppingCatalog.clear();
            showInlineMessage(e.getMessage());
        }
    }

    private ProductOption toProductOption(Product product) {
        String category = safe(product.getCategoryName());
        if (category.isBlank()) {
            category = "Khác";
        }
        ProductStatus status = product.getTrangThai() == 1 ? ProductStatus.AVAILABLE : ProductStatus.PAUSED;
        return new ProductOption(
                product.getSanPhamId(),
                product.getTenSanPham(),
                category,
                product.getGiaBan() == null ? BigDecimal.ZERO : product.getGiaBan(),
                status
        );
    }
    private void initCategoryChips() {
        categoryChipBox.getChildren().clear();
        for (String category : categories) {
            Button chip = new Button(category);
            chip.getStyleClass().add("product-category-chip");
            chip.setOnAction(event -> {
                selectedCategory = category;
                updateCategoryChipStyles();
                filterProducts();
            });
            categoryChipBox.getChildren().add(chip);
        }
        updateCategoryChipStyles();
    }

    private void updateCategoryChipStyles() {
        for (Node node : categoryChipBox.getChildren()) {
            if (node instanceof Button button) {
                button.getStyleClass().remove("product-category-chip-active");
                if (Objects.equals(button.getText(), selectedCategory)) {
                    addStyleClass(button, "product-category-chip-active");
                }
            }
        }
    }

    private void filterProducts() {
        String keyword = normalize(searchProductField.getText());
        List<ProductOption> filteredProducts = products.stream()
                .filter(product -> CATEGORY_ALL.equals(selectedCategory) || selectedCategory.equals(product.getCategory()))
                .filter(product -> keyword.isBlank()
                        || normalize(product.getProductName()).contains(keyword)
                        || normalize(product.getProductId()).contains(keyword))
                .toList();
        renderProductCards(filteredProducts);
    }

    private void renderProductCards(List<ProductOption> filteredProducts) {
        if (filteredProducts.isEmpty()) {
            Label emptyState = new Label("Chưa có món phù hợp");
            emptyState.getStyleClass().add("cart-item-detail");
            productTilePane.getChildren().setAll(emptyState);
            return;
        }
        productTilePane.getChildren().setAll(filteredProducts.stream()
                .map(this::createProductCard)
                .toList());
    }

    private VBox createProductCard(ProductOption product) {
        Label categoryBadge = new Label(product.getCategory());
        categoryBadge.getStyleClass().add("product-card-category");

        Label statusBadge = createStatusBadge(product.getStatus());
        Region badgeSpacer = new Region();
        HBox.setHgrow(badgeSpacer, Priority.ALWAYS);
        HBox topRow = new HBox(8, categoryBadge, badgeSpacer, statusBadge);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label name = new Label(product.getProductName());
        name.getStyleClass().add("product-card-name");
        name.setWrapText(true);

        Label price = new Label(formatMoneyCompact(product.getBasePrice()));
        price.getStyleClass().add("product-card-price");

        Button addButton = new Button(product.isToppingCategory() ? "Chọn kèm món" : "Thêm");
        addButton.getStyleClass().add("primary-button");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setOnAction(event -> {
            if (product.isToppingCategory()) {
                showInlineMessage("Topping được chọn khi tùy chỉnh món uống.");
                return;
            }
            openCustomizeProductDialog(product);
        });

        if (!product.getStatus().isAvailable()) {
            addButton.setDisable(true);
            addButton.setText(product.getStatus() == ProductStatus.OUT_OF_STOCK ? "Hết hàng" : "Tạm ngưng");
        }

        VBox card = new VBox(12, topRow, name, price, addButton);
        card.getStyleClass().add("product-card");
        if (!product.getStatus().isAvailable()) {
            addStyleClass(card, "product-card-disabled");
        }
        card.setPrefWidth(225);
        card.setMinHeight(172);
        return card;
    }

    private Label createStatusBadge(ProductStatus status) {
        Label badge = new Label(status.getLabel());
        badge.getStyleClass().add(switch (status) {
            case AVAILABLE -> "product-status-available";
            case OUT_OF_STOCK -> "product-status-out";
            case PAUSED -> "product-status-paused";
        });
        return badge;
    }

    private void openCustomizeProductDialog(ProductOption product) {
        if (!product.getStatus().isAvailable()) {
            showInlineMessage("Sản phẩm hiện không còn phục vụ.");
            return;
        }
        currentProduct = product;
        editingItem = null;
        currentQuantity = 1;
        renderCustomizeDialog(product, null);
        customizeOverlay.setManaged(true);
        customizeOverlay.setVisible(true);
    }

    private void closeCustomizeProductDialog() {
        customizeDialogContainer.getChildren().clear();
        customizeOverlay.setVisible(false);
        customizeOverlay.setManaged(false);
        currentProduct = null;
        editingItem = null;
        toppingSelections.clear();
    }

    private void renderCustomizeDialog(ProductOption product, OrderItem existingItem) {
        toppingSelections.clear();
        sugarToggleGroup = null;
        iceToggleGroup = null;
        noteTextArea = null;
        dialogSubtotalLabel = null;

        VBox headerText = new VBox(5);
        Label dialogTitle = new Label(product.getProductName());
        dialogTitle.getStyleClass().add("order-option-dialog-title");

        Label basePrice = new Label("Giá gốc: " + formatMoneyCompact(product.getBasePrice()));
        basePrice.getStyleClass().add("order-option-meta");

        HBox metadata = new HBox(8, createCategoryBadge(product.getCategory()), basePrice, createStatusBadge(product.getStatus()));
        metadata.setAlignment(Pos.CENTER_LEFT);
        headerText.getChildren().addAll(dialogTitle, metadata);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Button closeButton = new Button("Đóng");
        closeButton.getStyleClass().add("cart-action-button");
        closeButton.setOnAction(event -> closeCustomizeProductDialog());

        HBox header = new HBox(12, headerText, headerSpacer, closeButton);
        header.setAlignment(Pos.TOP_LEFT);
        header.getStyleClass().add("order-option-dialog-header");

        VBox body = new VBox(12);
        body.getChildren().add(createQuantitySection());
        if (product.isDrink()) {
            sugarToggleGroup = new ToggleGroup();
            iceToggleGroup = new ToggleGroup();
            body.getChildren().add(createToggleOptionSection("Mức đường", SUGAR_LEVELS, sugarToggleGroup, existingItem == null ? "100%" : existingItem.getSugarLevel()));
            body.getChildren().add(createToggleOptionSection("Mức đá", ICE_LEVELS, iceToggleGroup, existingItem == null ? "Bình thường" : existingItem.getIceLevel()));
            body.getChildren().add(createToppingSection(existingItem));
        }
        body.getChildren().add(createNoteSection(existingItem));
        dialogSubtotalLabel = new Label();
        dialogSubtotalLabel.getStyleClass().add("cart-item-price");
        Label subtotalTitle = new Label("Tạm tính");
        subtotalTitle.getStyleClass().add("order-option-section-title");
        body.getChildren().add(createSection(subtotalTitle, dialogSubtotalLabel));
        updateDialogSubtotal();

        ScrollPane bodyScroll = new ScrollPane(body);
        bodyScroll.setFitToWidth(true);
        bodyScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        bodyScroll.setMaxHeight(500);
        bodyScroll.getStyleClass().add("order-option-scroll");

        Button cancelButton = new Button("Hủy");
        cancelButton.getStyleClass().add("secondary-button");
        cancelButton.setOnAction(event -> closeCustomizeProductDialog());

        Button submitButton = new Button(existingItem == null ? "Thêm vào giỏ" : "Cập nhật món");
        submitButton.getStyleClass().add("primary-button");
        submitButton.setOnAction(event -> handleAddCustomizedItem());

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        HBox footer = new HBox(10, footerSpacer, cancelButton, submitButton);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("order-option-dialog-footer");

        VBox dialog = new VBox(16, header, bodyScroll, footer);
        dialog.getStyleClass().add("order-option-dialog");
        dialog.setMaxWidth(580);
        dialog.setPrefWidth(560);
        customizeDialogContainer.getChildren().setAll(dialog);
    }

    private Label createCategoryBadge(String category) {
        Label badge = new Label(category);
        badge.getStyleClass().add("product-card-category");
        return badge;
    }

    private VBox createQuantitySection() {
        Label title = new Label("Số lượng");
        title.getStyleClass().add("order-option-section-title");

        Button minusButton = new Button("-");
        minusButton.getStyleClass().add("quantity-button");
        minusButton.setOnAction(event -> {
            if (currentQuantity > 1) {
                currentQuantity--;
                updateQuantityLabel();
                updateDialogSubtotal();
            }
        });

        quantityValueLabel = new Label(String.valueOf(currentQuantity));
        quantityValueLabel.getStyleClass().add("quantity-value");

        Button plusButton = new Button("+");
        plusButton.getStyleClass().add("quantity-button");
        plusButton.setOnAction(event -> {
            currentQuantity++;
            updateQuantityLabel();
            updateDialogSubtotal();
        });

        HBox quantityControls = new HBox(8, minusButton, quantityValueLabel, plusButton);
        quantityControls.setAlignment(Pos.CENTER_LEFT);
        return createSection(title, quantityControls);
    }

    private VBox createToggleOptionSection(String titleText, List<String> options, ToggleGroup toggleGroup, String selectedValue) {
        Label title = new Label(titleText);
        title.getStyleClass().add("order-option-section-title");

        HBox optionRow = new HBox(8);
        optionRow.setAlignment(Pos.CENTER_LEFT);
        for (String option : options) {
            ToggleButton toggleButton = new ToggleButton(option);
            toggleButton.getStyleClass().add("option-chip");
            toggleButton.setUserData(option);
            toggleButton.setToggleGroup(toggleGroup);
            optionRow.getChildren().add(toggleButton);
            if (Objects.equals(option, selectedValue)) {
                toggleButton.setSelected(true);
            }
        }

        if (toggleGroup.getSelectedToggle() == null && !toggleGroup.getToggles().isEmpty()) {
            toggleGroup.getToggles().get(0).setSelected(true);
        }
        toggleGroup.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> updateOptionChipStyles(toggleGroup));
        updateOptionChipStyles(toggleGroup);

        return createSection(title, optionRow);
    }

    private VBox createToppingSection(OrderItem existingItem) {
        Label title = new Label("Topping");
        title.getStyleClass().add("order-option-section-title");

        VBox toppingList = new VBox(8);
        for (ProductOption topping : toppingCatalog) {
            if (!topping.getStatus().isAvailable()) {
                continue;
            }
            ToppingItem existingTopping = findTopping(existingItem, topping.getProductId());
            ToppingSelection selection = new ToppingSelection(topping, existingTopping == null ? 0 : existingTopping.getQuantity());
            toppingSelections.add(selection);
            toppingList.getChildren().add(createToppingRow(selection));
        }
        if (toppingList.getChildren().isEmpty()) {
            toppingList.getChildren().add(createCartDetailLabel("Không có topping khả dụng"));
        }
        return createSection(title, toppingList);
    }

    private HBox createToppingRow(ToppingSelection selection) {
        Label nameLabel = new Label(selection.topping.getProductName() + " +" + formatMoneyCompact(selection.topping.getBasePrice()));
        nameLabel.getStyleClass().add("cart-item-detail");
        nameLabel.setWrapText(true);

        Button minusButton = new Button("-");
        minusButton.getStyleClass().add("quantity-button");
        minusButton.setOnAction(event -> {
            selection.setQuantity(selection.quantity - 1);
            updateDialogSubtotal();
        });

        Button plusButton = new Button("+");
        plusButton.getStyleClass().add("quantity-button");
        plusButton.setOnAction(event -> {
            selection.setQuantity(selection.quantity + 1);
            updateDialogSubtotal();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(8, nameLabel, spacer, minusButton, selection.quantityLabel, plusButton, selection.subtotalLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        selection.refresh(currentQuantity);
        return row;
    }

    private VBox createNoteSection(OrderItem existingItem) {
        Label title = new Label("Ghi chú");
        title.getStyleClass().add("order-option-section-title");

        noteTextArea = new TextArea();
        noteTextArea.setPromptText("Ví dụ: ít ngọt, không lấy đá, thêm sữa, tách riêng topping...");
        noteTextArea.setPrefRowCount(3);
        noteTextArea.setWrapText(true);
        noteTextArea.getStyleClass().add("page-text-area");
        if (existingItem != null && existingItem.getNote() != null) {
            noteTextArea.setText(existingItem.getNote());
        }
        return createSection(title, noteTextArea);
    }

    private VBox createSection(Label title, Node content) {
        VBox section = new VBox(9, title, content);
        section.getStyleClass().add("order-option-section");
        return section;
    }

    private void updateQuantityLabel() {
        if (quantityValueLabel != null) {
            quantityValueLabel.setText(String.valueOf(currentQuantity));
        }
    }

    private void updateDialogSubtotal() {
        for (ToppingSelection selection : toppingSelections) {
            selection.refresh(currentQuantity);
        }
        if (dialogSubtotalLabel != null && currentProduct != null) {
            OrderItem preview = new OrderItem();
            preview.setBasePrice(currentProduct.getBasePrice());
            preview.setQuantity(currentQuantity);
            preview.setToppings(collectSelectedToppings());
            dialogSubtotalLabel.setText(CurrencyFormatter.format(calculateItemTotal(preview)));
        }
    }

    private void updateOptionChipStyles(ToggleGroup toggleGroup) {
        for (Toggle toggle : toggleGroup.getToggles()) {
            if (toggle instanceof ToggleButton button) {
                button.getStyleClass().remove("option-chip-active");
                if (toggle == toggleGroup.getSelectedToggle()) {
                    addStyleClass(button, "option-chip-active");
                }
            }
        }
    }

    private void handleAddCustomizedItem() {
        if (currentProduct == null) {
            return;
        }

        OrderItem item = editingItem == null ? new OrderItem() : editingItem;
        item.setProductId(currentProduct.getProductId());
        item.setProductName(currentProduct.getProductName());
        item.setCategory(currentProduct.getCategory());
        item.setBasePrice(currentProduct.getBasePrice());
        item.setQuantity(currentQuantity);
        item.setSugarLevel(currentProduct.isDrink() ? getSelectedToggleValue(sugarToggleGroup) : "");
        item.setIceLevel(currentProduct.isDrink() ? getSelectedToggleValue(iceToggleGroup) : "");
        item.setToppings(currentProduct.isDrink() ? collectSelectedToppings() : List.of());
        item.setNote(noteTextArea == null ? "" : safe(noteTextArea.getText()));
        item.setLineTotal(calculateItemTotal(item));

        if (editingItem == null) {
            OrderItem existingItem = findMergeableCartItem(item);
            if (existingItem == null) {
                cartItems.add(item);
            } else {
                existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
                existingItem.setLineTotal(calculateItemTotal(existingItem));
            }
        }

        renderCartItems();
        closeCustomizeProductDialog();
    }

    private OrderItem findMergeableCartItem(OrderItem newItem) {
        return cartItems.stream()
                .filter(item -> Objects.equals(item.getProductId(), newItem.getProductId()))
                .filter(item -> Objects.equals(safe(item.getSugarLevel()), safe(newItem.getSugarLevel())))
                .filter(item -> Objects.equals(safe(item.getIceLevel()), safe(newItem.getIceLevel())))
                .filter(item -> Objects.equals(safe(item.getNote()), safe(newItem.getNote())))
                .filter(item -> sameToppings(item, newItem))
                .findFirst()
                .orElse(null);
    }

    private boolean sameToppings(OrderItem first, OrderItem second) {
        return toppingKey(first).equals(toppingKey(second));
    }

    private String toppingKey(OrderItem item) {
        return item.getToppings().stream()
                .filter(topping -> topping.getQuantity() > 0)
                .sorted((first, second) -> safe(first.getToppingId()).compareTo(safe(second.getToppingId())))
                .map(topping -> safe(topping.getToppingId()) + ":" + topping.getQuantity() + ":" + nullToZero(topping.getPrice()).toPlainString())
                .collect(Collectors.joining("|"));
    }

    private List<ToppingItem> collectSelectedToppings() {
        List<ToppingItem> selectedToppings = new ArrayList<>();
        for (ToppingSelection selection : toppingSelections) {
            if (selection.quantity > 0) {
                ProductOption topping = selection.topping;
                selectedToppings.add(new ToppingItem(topping.getProductId(), topping.getProductName(), topping.getBasePrice(), selection.quantity));
            }
        }
        return selectedToppings;
    }

    private String getSelectedToggleValue(ToggleGroup toggleGroup) {
        if (toggleGroup == null || toggleGroup.getSelectedToggle() == null) {
            return "";
        }
        Object value = toggleGroup.getSelectedToggle().getUserData();
        return value == null ? "" : value.toString();
    }

    private void handleEditCartItem(OrderItem item) {
        ProductOption product = products.stream()
                .filter(candidate -> Objects.equals(candidate.getProductId(), item.getProductId()))
                .findFirst()
                .orElse(new ProductOption(item.getProductId(), item.getProductName(), item.getCategory(), item.getBasePrice(), ProductStatus.AVAILABLE));

        currentProduct = product;
        editingItem = item;
        currentQuantity = item.getQuantity();
        renderCustomizeDialog(product, item);
        customizeOverlay.setManaged(true);
        customizeOverlay.setVisible(true);
    }

    private void handleRemoveCartItem(OrderItem item) {
        cartItems.remove(item);
        renderCartItems();
    }

    private void handleIncreaseCartItem(OrderItem item) {
        item.setQuantity(item.getQuantity() + 1);
        item.setLineTotal(calculateItemTotal(item));
        renderCartItems();
    }

    private void handleDecreaseCartItem(OrderItem item) {
        if (item.getQuantity() <= 1) {
            handleRemoveCartItem(item);
            return;
        }
        item.setQuantity(item.getQuantity() - 1);
        item.setLineTotal(calculateItemTotal(item));
        renderCartItems();
    }

    private BigDecimal calculateItemTotal(OrderItem item) {
        return item.getBasePrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                .add(calculateToppingTotal(item));
    }

    private BigDecimal calculateItemUnitPrice(OrderItem item) {
        return item.getBasePrice();
    }

    private BigDecimal calculateToppingTotal(OrderItem item) {
        return item.getToppings().stream()
                .map(topping -> nullToZero(topping.getPrice())
                        .multiply(BigDecimal.valueOf(topping.getQuantity()))
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateCartTotal() {
        return cartItems.stream()
                .map(item -> {
                    BigDecimal lineTotal = calculateItemTotal(item);
                    item.setLineTotal(lineTotal);
                    return lineTotal;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void renderCartItems() {
        BigDecimal cartTotal = calculateCartTotal();
        cartItemsBox.getChildren().clear();
        if (cartItems.isEmpty()) {
            Label emptyState = new Label("Chưa có món trong giỏ");
            emptyState.getStyleClass().add("cart-empty-label");
            cartItemsBox.getChildren().add(emptyState);
        } else {
            cartItemsBox.getChildren().addAll(cartItems.stream()
                    .map(this::createCartItemCard)
                    .toList());
        }
        totalLabel.setText(CurrencyFormatter.format(cartTotal));
    }

    private VBox createCartItemCard(OrderItem item) {
        Label itemName = new Label(item.getProductName() + " x" + item.getQuantity());
        itemName.getStyleClass().add("cart-item-name");
        itemName.setWrapText(true);

        Label linePrice = new Label(CurrencyFormatter.format(item.getLineTotal()));
        linePrice.getStyleClass().add("cart-item-price");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(8, itemName, spacer, linePrice);
        header.setAlignment(Pos.TOP_LEFT);

        VBox optionLines = renderOrderItemOptions(item);

        Button minusButton = new Button("-");
        minusButton.getStyleClass().add("cart-action-button");
        minusButton.setOnAction(event -> handleDecreaseCartItem(item));

        Button plusButton = new Button("+");
        plusButton.getStyleClass().add("cart-action-button");
        plusButton.setOnAction(event -> handleIncreaseCartItem(item));

        Button editButton = new Button("Sửa");
        editButton.getStyleClass().add("cart-action-button");
        editButton.setOnAction(event -> handleEditCartItem(item));

        Button removeButton = new Button("Xóa");
        removeButton.getStyleClass().addAll("cart-action-button", "cart-action-button-danger");
        removeButton.setOnAction(event -> handleRemoveCartItem(item));

        HBox actions = new HBox(8, minusButton, plusButton, editButton, removeButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(8, header, optionLines, actions);
        card.getStyleClass().add("cart-item-card");
        return card;
    }

    private VBox renderOrderItemOptions(OrderItem item) {
        VBox options = new VBox(3);
        String optionSummary = buildOptionSummary(item);
        if (!optionSummary.isBlank()) {
            options.getChildren().add(createCartDetailLabel(optionSummary));
        }

        String toppingSummary = item.getToppings().stream()
                .map(topping -> topping.getToppingName() + " x" + topping.getQuantity())
                .collect(Collectors.joining(", "));
        if (!toppingSummary.isBlank()) {
            options.getChildren().add(createCartDetailLabel("Topping: " + toppingSummary));
        }

        if (!safe(item.getNote()).isBlank()) {
            options.getChildren().add(createCartDetailLabel("Ghi chú: " + safe(item.getNote())));
        }
        options.getChildren().add(createCartDetailLabel("Tạm tính: " + CurrencyFormatter.format(item.getLineTotal())));
        return options;
    }

    private Label createCartDetailLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("cart-item-detail");
        label.setWrapText(true);
        return label;
    }

    private String buildOptionSummary(OrderItem item) {
        List<String> details = new ArrayList<>();
        if (!safe(item.getSugarLevel()).isBlank()) {
            details.add("Đường " + safe(item.getSugarLevel()));
        }
        if (!safe(item.getIceLevel()).isBlank()) {
            details.add(safe(item.getIceLevel()));
        }
        return String.join(" • ", details);
    }

    public String buildOrderDetailText(OrderItem item) {
        List<String> noteParts = new ArrayList<>();
        noteParts.add("Số lượng: " + item.getQuantity());

        if (!safe(item.getSugarLevel()).isBlank()) {
            noteParts.add("Đường: " + safe(item.getSugarLevel()));
        }
        if (!safe(item.getIceLevel()).isBlank()) {
            noteParts.add("Đá: " + safe(item.getIceLevel()));
        }

        if (!safe(item.getNote()).isBlank()) {
            noteParts.add("Ghi chú: " + safe(item.getNote()));
        }

        return String.join(" | ", noteParts);
    }

    private List<OrderDetail> toOrderDetails() {
        List<OrderDetail> details = new ArrayList<>();
        for (OrderItem item : cartItems) {
            OrderDetail detail = new OrderDetail(
                    null,
                    null,
                    item.getProductId(),
                    BigDecimal.valueOf(item.getQuantity()),
                    calculateItemUnitPrice(item),
                    calculateItemTotal(item),
                    buildOrderDetailText(item),
                    null,
                    null
            );
            detail.setToppings(toOrderDetailToppings(item));
            details.add(detail);
        }
        return details;
    }

    private List<OrderDetailTopping> toOrderDetailToppings(OrderItem item) {
        List<OrderDetailTopping> toppings = new ArrayList<>();
        for (ToppingItem topping : item.getToppings()) {
            if (topping.getQuantity() <= 0) {
                continue;
            }
            BigDecimal subtotal = topping.calculateSubtotal(item.getQuantity());
            toppings.add(new OrderDetailTopping(
                    generateId("ODT"),
                    null,
                    topping.getToppingId(),
                    topping.getToppingName(),
                    BigDecimal.valueOf(topping.getQuantity()),
                    nullToZero(topping.getPrice()),
                    subtotal,
                    null,
                    null
            ));
        }
        return toppings;
    }

    private void openPaymentScreen(String orderId) throws Exception {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/phungloccoffee/gui/view/PaymentScreen.fxml"));
        Parent page = loader.load();
        PaymentController controller = loader.getController();
        controller.setOrderId(orderId);
        findContentArea().getChildren().setAll(page);
    }

    private StackPane findContentArea() {
        Node node = cartItemsBox;
        while (node != null) {
            if (node instanceof StackPane stackPane && stackPane.getStyleClass().contains("content-area")) {
                return stackPane;
            }
            node = node.getParent();
        }
        throw new IllegalStateException("Không tìm thấy vùng hiển thị nội dung để mở màn hình thanh toán.");
    }

    private boolean hasTopping(OrderItem item, String toppingId) {
        return item.getToppings().stream()
                .anyMatch(topping -> Objects.equals(topping.getToppingId(), toppingId));
    }

    private ToppingItem findTopping(OrderItem item, String toppingId) {
        if (item == null) {
            return null;
        }
        return item.getToppings().stream()
                .filter(topping -> Objects.equals(topping.getToppingId(), toppingId))
                .findFirst()
                .orElse(null);
    }

    private class ToppingSelection {
        private final ProductOption topping;
        private final Label quantityLabel = new Label();
        private final Label subtotalLabel = new Label();
        private int quantity;

        private ToppingSelection(ProductOption topping, int quantity) {
            this.topping = topping;
            this.quantity = Math.max(0, quantity);
            quantityLabel.getStyleClass().add("quantity-value");
            subtotalLabel.getStyleClass().add("cart-item-price");
        }

        private void setQuantity(int quantity) {
            this.quantity = Math.max(0, quantity);
            refresh(currentQuantity);
        }

        private void refresh(int mainQuantity) {
            quantityLabel.setText(String.valueOf(quantity));
            BigDecimal subtotal = topping.getBasePrice()
                    .multiply(BigDecimal.valueOf(quantity))
                    .multiply(BigDecimal.valueOf(Math.max(1, mainQuantity)));
            subtotalLabel.setText(CurrencyFormatter.format(subtotal));
        }
    }

    private Label createEmptyLabel(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("cart-empty-label");
        return label;
    }

    private void showInlineMessage(String message) {
        if (notificationDelay != null) {
            notificationDelay.stop();
        }
        notificationLabel.setText(message);
        notificationLabel.setManaged(true);
        notificationLabel.setVisible(true);

        notificationDelay = new PauseTransition(Duration.seconds(3.5));
        notificationDelay.setOnFinished(event -> {
            notificationLabel.setVisible(false);
            notificationLabel.setManaged(false);
        });
        notificationDelay.play();
    }

    private String normalize(String value) {
        String lower = safe(value).toLowerCase(Locale.ROOT);
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String formatMoneyCompact(BigDecimal amount) {
        long value = amount == null ? 0L : amount.longValue();
        return String.format(Locale.US, "%,d", value).replace(",", ".") + "d";
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String generateId(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private boolean isLocalOrder(String orderId) {
        return orderId != null && orderId.contains("-");
    }

    private void addStyleClass(Node node, String styleClass) {
        if (!node.getStyleClass().contains(styleClass)) {
            node.getStyleClass().add(styleClass);
        }
    }
}






