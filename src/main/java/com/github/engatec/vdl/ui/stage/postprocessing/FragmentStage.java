package com.github.engatec.vdl.ui.stage.postprocessing;

import java.util.function.Consumer;

import com.github.engatec.vdl.controller.postprocessing.FragmentController;
import com.github.engatec.vdl.core.ApplicationContext;
import com.github.engatec.vdl.model.postprocessing.FragmentCutPostprocessing;
import com.github.engatec.vdl.model.postprocessing.Postprocessing;
import com.github.engatec.vdl.ui.stage.AppStage;
import javafx.stage.Stage;
import javafx.util.Callback;

public class FragmentStage extends AppStage {

    private final FragmentCutPostprocessing model;
    private final Consumer<? super Postprocessing> okClickCallback;

    public FragmentStage(FragmentCutPostprocessing model, Consumer<? super Postprocessing> okClickCallback) {
        this.model = model;
        this.okClickCallback = okClickCallback;
        init();
    }

    @Override
    protected void init() {
        super.init();
        stage.setTitle(ApplicationContext.INSTANCE.getResourceBundle().getString("stage.postprocessing.fragment"));
        stage.setResizable(false);
    }

    @Override
    protected String getFxmlPath() {
        return "/fxml/postprocessing/fragment.fxml";
    }

    @Override
    protected Callback<Class<?>, Object> getControllerFactory(Stage stage) {
        return param -> new FragmentController(stage, model, okClickCallback);
    }
}
