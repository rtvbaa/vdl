package com.github.engatec.vdl.ui.stage;

import com.github.engatec.vdl.controller.QueueController;
import com.github.engatec.vdl.core.ApplicationContext;
import javafx.stage.Stage;
import javafx.util.Callback;

public class QueueStage extends AppStage {

    public QueueStage() {
        init();
        stage.setTitle(ApplicationContext.INSTANCE.getResourceBundle().getString("stage.queue.title"));
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected String getFxmlPath() {
        return "/fxml/queue/queue.fxml";
    }

    @Override
    protected Callback<Class<?>, Object> getControllerFactory(Stage stage) {
        return param -> new QueueController(stage);
    }
}
