package org.bbaw.wsp.cms.app.test;

import java.util.Date;

import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.lucene.IndexHandler;

import com.vaadin.Application;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;

public class TestApp extends Application {
  private static final long serialVersionUID = 1L;

  @Override 
  public void init() {
    final Window mainWindow = new Window("Test Application");
    Label label = new Label("Hello Test Application");
    mainWindow.addComponent(label);
    mainWindow.addComponent(
      new Button("What is the time?",
        new Button.ClickListener() {
          public void buttonClick(ClickEvent event) {
            mainWindow.showNotification(
                "The time is " + new Date());
          }
       }));

    String exampleDocId = "/MEGA/mega/docs/MEGA_A2_B001-01_ETX.xml";
    final TextField tf = new TextField("");
    tf.setValue(exampleDocId);
    tf.setColumns(40);
    ValueChangeListener valueChangeListener = 
      new Property.ValueChangeListener() {
        public void valueChange(ValueChangeEvent event) {
          // ToDo
        }
      };
    tf.addListener(valueChangeListener);
    mainWindow.addComponent(tf);
    mainWindow.addComponent(
      new Button("Show metadata",
        new Button.ClickListener() {
          public void buttonClick(ClickEvent event) {
            try {
              IndexHandler indexHandler = IndexHandler.getInstance();
              String docId = (String) tf.getValue();
              MetadataRecord mdRecord = indexHandler.getDocMetadata(docId);
              String author = mdRecord.getCreator();
              String title = mdRecord.getTitle();
              String year = mdRecord.getYear();
              String template =
                  "<p>" + "Metadata of: " + docId + "</p>"+
                  "<table>" +
                  "<tr><td>Author: </td><td>" + author + "</td></tr>" +
                  "<tr><td>Title: </td><td>" + title + "</td></tr>" +
                  "<tr><td>Year: </td><td>" + year + "</td></tr>" +
                  "</table>";
              mainWindow.showNotification(template);
            } catch (Exception e) {
              // nothing
            }
          }
        }));
    setMainWindow(mainWindow);
  }
}