package com.my.world.module.render;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.my.world.core.Component;
import com.my.world.core.Config;
import com.my.world.core.Loadable;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class Render implements Component, Loadable.OnInit {

    @Config(type = Config.Type.Asset)
    public RenderModel renderModel;

    @Config
    public boolean includeEnv = true;

    public ModelInstance modelInstance;
    public final Vector3 center = new Vector3();
    public final Vector3 dimensions = new Vector3();
    public float radius;

    public Render(RenderModel renderModel) {
        this.renderModel = renderModel;
        init();
    }

    public Render(ModelInstance modelInstance) {
        this.modelInstance = modelInstance;
        calculateBoundingBox();
    }

    @Override
    public void init() {
        this.modelInstance = new ModelInstance(renderModel.model);
        this.center.set(renderModel.center);
        this.dimensions.set(renderModel.dimensions);
        this.radius = renderModel.radius;
    }

    public void calculateBoundingBox() {
        if (this.modelInstance == null) throw new RuntimeException("ModelInstance is null");
        this.modelInstance.model.calculateBoundingBox(boundingBox);
        boundingBox.getCenter(center);
        boundingBox.getDimensions(dimensions);
        radius = dimensions.len() / 2f;
    }

    private static final BoundingBox boundingBox = new BoundingBox();
}