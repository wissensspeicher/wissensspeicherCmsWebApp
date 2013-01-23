package org.bbaw.wsp.cms.app.test2;

import java.util.Date;

import com.vaadin.Application;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;

public class TestApp2 extends Application {
  private static final long serialVersionUID = 1L;

  @Override 
  public void init() {
    final Window mainWindow = new Window("Test Application 2");
    Label label = new Label("Hello Test Application 2");
    mainWindow.addComponent(label);
    mainWindow.addComponent(
      new Button("What is the time?",
        new Button.ClickListener() {
          public void buttonClick(ClickEvent event) {
            mainWindow.showNotification(
                "The time is " + new Date());
          }
       }));
    setMainWindow(mainWindow);
  }
}