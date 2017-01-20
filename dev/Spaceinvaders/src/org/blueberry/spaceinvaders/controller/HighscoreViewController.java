package org.blueberry.spaceinvaders.controller;

import java.net.URL;
import java.util.ResourceBundle;

import com.sun.javafx.scene.control.skin.TableColumnHeader;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.blueberry.spaceinvaders.SpaceInvaders;
import org.blueberry.spaceinvaders.gameengine.Game;
import org.blueberry.spaceinvaders.gameengine.InvaderGroup;
import org.blueberry.spaceinvaders.highscore.Highscore;
import org.blueberry.spaceinvaders.highscore.IDatabaseConnector;
import org.blueberry.spaceinvaders.highscore.MySQLDBConnector;

import static org.blueberry.spaceinvaders.gameengine.Game.GameStatus.PAUSE;
import static org.blueberry.spaceinvaders.gameengine.Game.GameStatus.PLAY;

/**
 * HighscoreViewController-Klasse
 */
public class HighscoreViewController implements Initializable {

    @FXML
    private TableView<Highscore> crudTable;
    @FXML
    private TableColumn nameColumn;
    @FXML
    private TableColumn idColumn;
    @FXML
    private TableColumn punkteColumn;
    @FXML
    private TableColumn dateColumn;
    @FXML
    private TextField nameField;
    @FXML
    private Button addButton;
    @FXML
    private Pagination pagination;
    @FXML
    private HBox hbox_input;
    @FXML
    private HBox hbox_mainmenuBtn;
    @FXML
    private HBox message_banner;

    // TODO should be in application.properties
    private int pageCount = 5;
    private int itemsPerPage = 15;
    private int currentPageIndex = 0;

//    private ObservableList<Highscore> highscore = FXCollections.observableArrayList();
    private ObservableList<Highscore> highscore;
//    private DatabaseConnector mysqlConnector;
    private IDatabaseConnector mysqlConnector;

    private Label messageLabel = new Label();
    private int punkt = 0;
    private String orderBy = "punkte DESC";

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {


        crudTable.addEventFilter(
            MouseEvent.MOUSE_CLICKED, event -> {
//                MouseEvent.MOUSE_RELEASED, event -> {
                if (event.getTarget() instanceof TableColumnHeader) {

                    switch (((TableColumnHeader) event.getTarget()).getId()){
                        case "dateColumn":
                            orderBy = orderBy == "created_at DESC" ? "created_at ASC" : "created_at DESC";
                            break;
                        case "punkteColumn":
                            orderBy = orderBy == "punkte DESC" ? "punkte ASC" : "punkte DESC";
                            break;
                        case "idColumn":
                            orderBy = orderBy == "position ASC" ? "position DESC" : "position ASC";
                            break;
                        case "nameColumn":
                            orderBy = orderBy == "name ASC" ? "name DESC" : "name ASC";
                            break;
                    }

                    if (pagination.getCurrentPageIndex() == 0){
                        crudTable.getItems().setAll(mysqlConnector.getHighscoreListPage(0, itemsPerPage, orderBy));
                    }
                    else {
                        pagination.setCurrentPageIndex(0);
                    }
                    event.consume();
                }
            }
        );



        crudTable.setFocusTraversable(true);

        crudTable.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {

                int index = pagination.getCurrentPageIndex();

                switch (event.getCode()) {
                    case LEFT:
                        if (index > 0) pagination.setCurrentPageIndex(--index);
                        break;
                    case RIGHT:
                        if (index < pagination.getPageCount()) pagination.setCurrentPageIndex(++index);
                        break;
                    case ESCAPE:
                        SpaceInvaders.setScreen("WelcomeView");
                        break;
                }
            }
        });



        mysqlConnector = new MySQLDBConnector();
        mysqlConnector.connect(SpaceInvaders.getSettings("db.url"), SpaceInvaders.getSettings("db.username"), SpaceInvaders.getSettings("db.password"));

//        mysqlConnector = new DatabaseConnector();
//        mysqlConnector.launchConnection();



        // set resize policy
        crudTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        //populate the table
        idColumn.setCellValueFactory(new PropertyValueFactory<Highscore, Integer>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<Highscore, String>("name"));
        punkteColumn.setCellValueFactory(new PropertyValueFactory<Highscore, Integer>("punkte"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<Highscore, String>("created_at"));

        // default inputs not visible
        hbox_input.setVisible(false);

        //Fetch data from gameplay
        punkt = Game.getInstance().getPlayer().getScore();

        // if clause to determine game score
        // TODO there should be a flag from game instance
        if (punkt != 0) {
            hbox_mainmenuBtn.setVisible(false);
            int position = mysqlConnector.determinePosition(punkt);
            messageLabel.setText("Sie haben " + punkt + " Punkte erreicht und damit Platz " + position + " in der Highscore belegt!!!");
            messageLabel.setTextFill(Color.GREEN);
            messageLabel.setFont(Font.font("Impact", 30));
            message_banner.getChildren().add(messageLabel);
            hbox_input.setVisible(true);
        }

        // disable Button until Namefield has 4 chars
        addButton.disableProperty().bind(
                Bindings.greaterThan(4, nameField.textProperty().length())
        );

//        highscore = mysqlConnector.getHighscoreList();

//        highscore = FXCollections.observableArrayList();
//        for(Object record : mysqlConnector.getRecords("highscore")){
//            highscore.add((Highscore) record);
//        }

//        pageCount = getPageCount(highscore.size(), itemsPerPage);
        int highScoreCompleteCount = mysqlConnector.getCount();

        // hide pagination if highscore.size items perpage (only one site)
        if (highScoreCompleteCount < itemsPerPage) {
            pagination.setVisible(false);
        }
        else {
            pagination.setPageCount(getPageCount(highScoreCompleteCount, itemsPerPage));

            pagination.currentPageIndexProperty().addListener((observable, oldValue, newValue) ->
                    crudTable.getItems().setAll(mysqlConnector.getHighscoreListPage(newValue.intValue() * itemsPerPage, itemsPerPage, orderBy))
            );
        }


        // init tableView content
        crudTable.getItems().setAll(mysqlConnector.getHighscoreListPage(0, itemsPerPage, orderBy));
    }

    /**
     * onEventOccured
     * @param event
     */
    // if there multiple action buttons (for settingsview)
    public void onEventOccured(ActionEvent event) {
        Button button = (Button) event.getSource();

        // case switch
        switch (button.getId()) {
            case "addButton": {
                System.out.println("addButton");
                addNewHighscore(punkt);
//                refreshList();
                crudTable.getItems().setAll(mysqlConnector.getHighscoreListPage(0, itemsPerPage, "created_at DESC"));
                break;
            }
        }
    }

    /**
     * refreshList
     */
    private void refreshList() {
        highscore = mysqlConnector.getHighscoreList();
//        crudTable.getItems().setAll(highscore.subList(currentPageIndex * itemsPerPage, ((currentPageIndex * itemsPerPage + itemsPerPage <= highscore.size())  ? currentPageIndex * itemsPerPage + itemsPerPage : highscore.size())));
        crudTable.getItems().setAll(
                highscore.subList(currentPageIndex * itemsPerPage,
                        ((currentPageIndex * itemsPerPage + itemsPerPage <= highscore.size())
                                ? currentPageIndex * itemsPerPage + itemsPerPage
                                : highscore.size())
                ));
    }

    /**
     * addNewHighscore
     */
    public void addNewHighscore(int punkte) {
        int id = 1;
        String name = nameField.getText();
        String date = " ";
        Highscore newHighscore = new Highscore(id, name, punkte, date);
        mysqlConnector.insertHighscore(newHighscore);
//        refreshList();
        hbox_input.setVisible(false);

        Game.reset();
        message_banner.setVisible(false);
        // TODO show toast message crud successfully
        // controlfx notification toast message
        // http://controlsfx.bitbucket.org/org/controlsfx/control/Notifications.html
        // TODO adding new highscore value jump to page and highlight this row
        // pagination.currentPageIndexProperty().setValue(2);
        hbox_mainmenuBtn.setVisible(true);
    }

    /**
     * getPageCount
     * @param totalCount
     * @param itemsPerPage
     * @return
     */
    // determine pagecount for pagination
    private int getPageCount(int totalCount, int itemsPerPage) {
        float floatCount = Float.valueOf(totalCount) / Float.valueOf(itemsPerPage);
        int intCount = totalCount / itemsPerPage;
        return ((floatCount > intCount) ? ++intCount : intCount);

    }

    /**
     * Wechselt zur Welcome-View.
     * @param event
     */
    @FXML
    private void goToScreenWelcomeView(ActionEvent event) {
        SpaceInvaders.setScreen("WelcomeView");
    }


}
