package com.phungloccoffee.gui.controller.layout;

import com.phungloccoffee.App;
import com.phungloccoffee.gui.controller.common.PlaceholderPageController;
import com.phungloccoffee.gui.model.AppUserSession;
import com.phungloccoffee.gui.model.MenuItemModel;
import com.phungloccoffee.gui.service.ActionConfig;
import com.phungloccoffee.gui.service.MenuConfig;
import com.phungloccoffee.gui.service.PermissionService;
import com.phungloccoffee.gui.service.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class MainController {
    @FXML private StackPane contentArea;
    @FXML private SidebarController sidebarController;
    @FXML private TopbarController topbarController;

    private AppUserSession currentUser;

    @FXML
    private void initialize() {
        if (!SessionManager.isLoggedIn()) {
            Platform.runLater(this::returnToLogin);
            return;
        }
        currentUser = SessionManager.getCurrentUser();
        setupLayout();
        loadDefaultPageByRole();
    }

    private void setupLayout() {
        topbarController.setUserContext(currentUser);
        sidebarController.setup(currentUser, this::navigateTo, this::logout);
    }

    private void loadDefaultPageByRole() {
        MenuItemModel defaultItem = MenuConfig.getDefaultMenuByRole(currentUser.getRole());
        sidebarController.setActive(defaultItem.getId());
        navigateTo(defaultItem);
    }

    public void navigateTo(MenuItemModel item) {
        if (!PermissionService.canAccess(currentUser.getRole(), item.getId())) {
            topbarController.setPage("Hệ thống > Quyền truy cập", "Không có quyền truy cập");
            topbarController.clearActions();
            loadPlaceholder(new MenuItemModel(
                    "ACCESS_DENIED",
                    "Không có quyền truy cập",
                    "!",
                    "Hệ thống > Quyền truy cập",
                    "Không có quyền truy cập",
                    "",
                    false
            ));
            return;
        }

        sidebarController.setActive(item.getId());
        loadPage(item);
    }

    private void loadPage(MenuItemModel item) {
        topbarController.setPage(item.getBreadcrumb(), item.getPageTitle(), isApprovalPageWithHiddenTitle(item.getId()));
        topbarController.setActions(ActionConfig.getActionsFor(item.getId()));

        URL resource = App.class.getResource(item.getFxmlPath());
        if (resource == null) {
            loadPlaceholder(item);
            return;
        }

        try {
            Parent page = FXMLLoader.load(resource);
            contentArea.getChildren().setAll(prepareContentPage(item, page));
        } catch (Exception e) {
            loadPlaceholder(item);
        }
    }

    private Parent prepareContentPage(MenuItemModel item, Parent page) {
        if (!shouldUseMainScroll(item.getId()) || page instanceof ScrollPane) {
            return page;
        }
        addStyleClass(page, "scroll-content");
        if (isReportLikePage(item.getId())) {
            addStyleClass(page, "report-content");
        }

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);
        scrollPane.getStyleClass().add("main-scroll-pane");
        return scrollPane;
    }

    private void addStyleClass(Parent page, String styleClass) {
        if (!page.getStyleClass().contains(styleClass)) {
            page.getStyleClass().add(styleClass);
        }
    }

    private boolean shouldUseMainScroll(String menuId) {
        return switch (menuId) {
            case "BEST_SELLER_REPORT",
                 "REVENUE_REPORT",
                 "INVENTORY_REPORT",
                 "BRANCH_REVENUE_REPORT",
                 "BRANCH_INVENTORY_REPORT",
                 "DIRECTOR_DASHBOARD",
                 "BRANCH_DASHBOARD",
                 "BRANCH_MANAGEMENT",
                 "PRODUCT_MANAGEMENT" -> true;
            default -> false;
        };
    }

    private boolean isReportLikePage(String menuId) {
        return switch (menuId) {
            case "BEST_SELLER_REPORT",
                 "REVENUE_REPORT",
                 "INVENTORY_REPORT",
                 "BRANCH_REVENUE_REPORT",
                 "BRANCH_INVENTORY_REPORT",
                 "DIRECTOR_DASHBOARD",
                 "BRANCH_DASHBOARD" -> true;
            default -> false;
        };
    }

    private void loadPlaceholder(MenuItemModel item) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/phungloccoffee/gui/view/common/PlaceholderPage.fxml"));
            Parent page = loader.load();
            PlaceholderPageController controller = loader.getController();
            controller.setContext(item, currentUser);
            contentArea.getChildren().setAll(page);
        } catch (IOException e) {
            contentArea.getChildren().clear();
        }
    }

    private boolean isApprovalPageWithHiddenTitle(String menuId) {
        return "APPROVE_IMPORT".equals(menuId)
                || "APPROVE_EXPORT".equals(menuId)
                || "APPROVE_TRANSFER".equals(menuId)
                || "APPROVE_MATERIAL_LOSS".equals(menuId)
                || "BRANCH_INVENTORY_REPORT".equals(menuId)
                || "POS_MAIN".equals(menuId)
                || "POS_PAYMENT".equals(menuId)
                || "POS_HISTORY".equals(menuId)
                || "OFFLINE_TRANSACTION".equals(menuId);
    }

    private void logout() {
        SessionManager.logout();
        returnToLogin();
    }

    private void returnToLogin() {
        try {
            Parent root = FXMLLoader.load(App.class.getResource("/com/phungloccoffee/gui/view/Login.fxml"));
            Scene scene = new Scene(root, 1280, 760);
            scene.getStylesheets().add(App.class.getResource("/com/phungloccoffee/gui/css/app.css").toExternalForm());
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Phụng Lộc Coffee");
        } catch (IOException e) {
            throw new IllegalStateException("Không thể quay lại màn hình đăng nhập.", e);
        }
    }
}
