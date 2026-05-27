package com.phungloccoffee.gui.controller.layout;

import com.phungloccoffee.gui.model.AppUserSession;
import com.phungloccoffee.gui.util.IconFactory;
import com.phungloccoffee.gui.util.UiTextUtil;
import com.phungloccoffee.offline.NetworkMonitor;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TopbarController {
    @FXML private Label breadcrumbLabel;
    @FXML private Label pageTitleLabel;
    @FXML private Label contextLabel;
    @FXML private Label timeLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private HBox actionContainer;
    @FXML private StackPane breadcrumbIconContainer;
    @FXML private VBox topbarLeft;
    @FXML private HBox breadcrumbRow;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final NetworkMonitor networkMonitor = NetworkMonitor.getInstance();

    @FXML
    private void initialize() {
        networkMonitor.addListener(online -> Platform.runLater(() -> updateConnectionStatus(online)));
        networkMonitor.start();
        updateConnectionStatus(networkMonitor.isOnline());
        breadcrumbIconContainer.getChildren().setAll(IconFactory.createTopbarIcon("breadcrumb-home"));
        updateClock();
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(30), event -> updateClock()));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    public void setUserContext(AppUserSession user) {
        contextLabel.setText(UiTextUtil.buildUserContext(user));
    }

    public void setPage(String breadcrumb, String pageTitle) {
        setPage(breadcrumb, pageTitle, false);
    }

    public void setPage(String breadcrumb, String pageTitle, boolean hideTitle) {
        breadcrumbLabel.setText(breadcrumb);
        pageTitleLabel.setText(hideTitle ? "" : pageTitle);
        pageTitleLabel.setVisible(!hideTitle);
        pageTitleLabel.setManaged(!hideTitle);
        topbarLeft.setSpacing(hideTitle ? 0 : 4);
        topbarLeft.setAlignment(Pos.CENTER_LEFT);
        breadcrumbRow.setAlignment(Pos.CENTER_LEFT);

        topbarLeft.getStyleClass().remove("title-hidden");
        if (hideTitle) {
            topbarLeft.getStyleClass().add("title-hidden");
        }
    }

    public void setActions(List<Button> actions) {
        actionContainer.getChildren().setAll(actions);
    }

    public void clearActions() {
        actionContainer.getChildren().clear();
    }

    private void updateClock() {
        timeLabel.setText(LocalDateTime.now().format(timeFormatter));
    }

    private void updateConnectionStatus(boolean online) {
        connectionStatusLabel.setText(online ? "Trực tuyến" : "Ngoại tuyến");
        connectionStatusLabel.getStyleClass().removeAll("status-online", "status-offline", "status-checking");
        connectionStatusLabel.getStyleClass().add(online ? "status-online" : "status-offline");
    }
}
