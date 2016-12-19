package org.blueberry.spaceinvaders.controller;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import org.blueberry.spaceinvaders.SpaceInvaders;

public class WelcomeViewController implements Initializable{

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }
    


    @FXML
    private void goToScreen2(ActionEvent event){
       SpaceInvaders.setScreen("GameplayView");
    }
    
    @FXML
    private void goToScreen3(ActionEvent event){
       SpaceInvaders.setScreen("HighscoreView");
    }
}