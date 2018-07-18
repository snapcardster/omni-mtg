package com.snapcardster.omnimtg;

import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.Icon;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class BasicView extends View {

    public BasicView() {
        /*
        Label label = new Label("Hello JavaFX World!");

        Button button = new Button("Change the World!");
        button.setGraphic(new Icon(MaterialDesignIcon.LANGUAGE));
        button.setOnAction(e -> label.setText("Hello JavaFX Universe!"));
        
        VBox controls = new VBox(15.0, label, button);
        controls.setAlignment(Pos.CENTER);
        
        setCenter(controls);*/
        Object x = null;
        try {
            x = Class.forName("com.snapcardster.omnimtg.MainGUI").newInstance();
            Node n = (Node) x.getClass().getMethod("getStage").invoke(x);
            setCenter(n);
        } catch (Exception e) {
            setCenter(new TextField(e.toString()));
        }
    }

    @Override
    protected void updateAppBar(AppBar appBar) {
        //appBar.setNavIcon(MaterialDesignIcon.MENU.button(e -> System.out.println("Menu")));
        appBar.setTitleText("Omni MTG");
        //appBar.getActionItems().add(MaterialDesignIcon.SEARCH.button(e -> System.out.println("Search")));
    }

}
