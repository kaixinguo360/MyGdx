package com.my.demo.builder.scene.test;

import com.badlogic.gdx.Input;
import com.my.demo.builder.BaseBuilder;
import com.my.demo.builder.object.CharacterBuilder;
import com.my.demo.builder.object.GroundBuilder;
import com.my.demo.builder.test.AnimationBuilder;
import com.my.demo.builder.test.PortalEntity;
import com.my.world.core.Component;
import com.my.world.core.Entity;
import com.my.world.core.Scene;
import com.my.world.module.animation.Animation;
import com.my.world.module.input.InputSystem;

import java.util.Map;

public class TestSceneBuilder extends BaseBuilder<TestSceneBuilder> {

    public GroundBuilder groundBuilder;
    public CharacterBuilder characterBuilder;
    public AnimationBuilder animationBuilder;

    @Override
    protected void initDependencies() {
        groundBuilder = getDependency(GroundBuilder.class);
        characterBuilder = getDependency(CharacterBuilder.class);
        animationBuilder = getDependency(AnimationBuilder.class);
    }

    @Override
    public Entity build(Scene scene, Map<String, Object> params) {
        Entity ground = groundBuilder.build(scene, null);

        characterBuilder.build(scene);

        Entity animationEntity = animationBuilder.build(scene);
        scene.addEntity(newEntity((InputSystem.OnKeyDown) keycode -> {
            if (keycode == Input.Keys.R) {
                Animation animation = animationEntity.getComponent(Animation.class);
                animation.animationController.initState = "state2";
            }
        }));

        PortalEntity portal1 = new PortalEntity(2.5f);
        portal1.setName("portal1");
        portal1.position.getLocalTransform().setToTranslation(5, 2.5f, 0);
        portal1.portalScript.targetPortalName = "portal2";
        portal1.addToScene(scene);

        PortalEntity portal2 = new PortalEntity(2.5f);
        portal2.setName("portal2");
        portal2.position.getLocalTransform().setToTranslation(-5, 2.5f, 0);
        portal2.portalScript.targetPortalName = "portal1";
        portal2.addToScene(scene);

        return ground;
    }

    public static Entity newEntity(Component component) {
        Entity entity = new Entity();
        entity.addComponent(component);
        return entity;
    }
}
