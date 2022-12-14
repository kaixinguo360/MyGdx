package com.my.demo.entity.common;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.my.demo.entity.object.BoxEntity;
import com.my.world.core.AssetsManager;
import com.my.world.core.Engine;
import com.my.world.core.Scene;
import com.my.world.enhanced.entity.EnhancedEntity;
import com.my.world.module.animation.*;
import com.my.world.module.animation.clip.Clip;
import com.my.world.module.animation.clip.ClipGroup;
import com.my.world.module.animation.clip.ReverseLoopClip;
import com.my.world.module.common.EnhancedPosition;
import com.my.world.module.physics.rigidbody.BoxBody;
import com.my.world.module.render.light.GLTFSpotLight;
import com.my.world.module.render.model.GLTFModelInstance;

public class AnimationEntity extends EnhancedEntity {

    public static Playable clip1;
    public static Playable clip2;
    public static Playable timeline1;
    public static TestAnimationController testAnimationController;

    public static void init(Engine engine, Scene scene) {
        AssetsManager assetsManager = engine.getAssetsManager();

        clip1 = assetsManager.addAsset("clip1", Playable.class, createAnimationData1());
        clip2 = assetsManager.addAsset("clip2", Playable.class, createAnimationData2());

        ClipGroup clipGroup1 = new ClipGroup();

        Clip part1 = new Clip();
        part1.playable = clip1;
        part1.start = 0;
        part1.end = 8;
        part1.scale = 2;
        part1.weights = new ConstantCurve<>(0.5f);
        clipGroup1.playables.add(part1);

        Clip part2 = new Clip();
        part2.playable = clip2;
        part2.start = 0;
        part2.end = 8;
        part2.scale = 0.5f;
        part2.weights = new ConstantCurve<>(0.5f);
        clipGroup1.playables.add(part2);

        ReverseLoopClip part3 = new ReverseLoopClip();
        part3.playable = clip2;
        part3.start = 8;
        part3.period = 1f;
        part3.scale = 0.25f;
        part3.reverseRatio = 0.25f;
        clipGroup1.playables.add(part3);

        timeline1 = assetsManager.addAsset("timeline1", Playable.class, clipGroup1);

        testAnimationController = new TestAnimationController();
        assetsManager.addAsset("testAnimationController", AnimationController.class, testAnimationController);
    }

    public final Animation animation;
    public final GLTFModelInstance render;
    public final BoxBody rigidBody;
    public final EnhancedEntity child1;
    public final EnhancedEntity child2;

    public AnimationEntity() {
        setName("AnimationEntity");
        transform.idt().translate(0, 3, 3);
        decompose();
        render = addComponent(new GLTFModelInstance(BoxEntity.model));
        rigidBody = addComponent(new BoxBody(new Vector3(0.5f, 0.5f, 0.5f), 0));
        rigidBody.isKinematic = true;
        animation = addComponent(new Animation());
        animation.animationController = testAnimationController;
        animation.addPlayable("clip1", clip1);
        animation.addPlayable("clip2", clip2);
        animation.addPlayable("clip3", timeline1);

        child1 = new EnhancedEntity();
        child1.setName("child1");
        child1.setParent(this);
        child1.transform.idt().translate(0, 1, 0);
        child1.decompose();
        child1.addComponent(new BoxBody(new Vector3(0.5f, 0.5f, 0.5f), 0)).isKinematic = true;
        child1.addComponent(new GLTFModelInstance(BoxEntity.model));
        addEntity(child1);

        child2 = new EnhancedEntity();
        child2.setName("child2");
        child2.setParent(child1);
        child2.transform.idt().translate(1, 0, 0);
        child2.decompose();
        child2.addComponent(new BoxBody(new Vector3(0.5f, 0.5f, 0.5f), 0)).isKinematic = true;
        child2.addComponent(new GLTFModelInstance(BoxEntity.model));
        child2.addComponent(new GLTFSpotLight(Color.WHITE.cpy(), Vector3.Z.cpy(), 60f, 40f));
        addEntity(child2);
    }

    // ----- Static ----- //

    public static AnimationClip createAnimationData1() {

        AnimationClip clip = new AnimationClip();

        // Channel - root.rotation
        AnimationChannel c1 = new AnimationChannel();
        c1.component = EnhancedPosition.class;
        c1.field = "rotation";
        c1.values = time -> new Vector3(time * 100, time * 100, time * 100);
        clip.channels.add(c1);

        // Channel - child1.rotation
        AnimationChannel c2 = new AnimationChannel();
        c2.entity = "child1";
        c2.component = EnhancedPosition.class;
        c2.field = "rotation";
        c2.values = time -> new Vector3(time * 100, 0, 0);
        clip.channels.add(c2);

        // Channel - child2.rotation
        AnimationChannel c3 = new AnimationChannel();
        c3.entity = "child2";
        c3.component = EnhancedPosition.class;
        c3.field = "rotation";
        c3.values = time -> new Vector3(0, time * 100, 0);
        clip.channels.add(c3);

        // Channel - child2.scale
        AnimationChannel c4 = new AnimationChannel();
        c4.entity = "child2";
        c4.component = EnhancedPosition.class;
        c4.field = "scale";
        c4.values = time -> new Vector3(
                (float) Math.sin(time * 6) * 0.2f + 0.8f,
                (float) Math.sin(time * 2.7) * 0.2f + 0.8f,
                (float) Math.sin(time * 10) * 0.2f + 0.8f
        );
        clip.channels.add(c4);

        // Channel - child2.light.color
        AnimationChannel c5 = new AnimationChannel();
        c5.entity = "child2";
        c5.component = GLTFSpotLight.class;
        c5.field = "light.color";
        c5.values = time -> new Color(
                (float) Math.sin(time * 6) * 0.2f + 0.8f,
                (float) Math.sin(time * 2.7) * 0.2f + 0.8f,
                (float) Math.sin(time * 10) * 0.2f + 0.8f
                , 1f
        );
        clip.channels.add(c5);

        // Channel - child2.light.intensity
        AnimationChannel c6 = new AnimationChannel();
        c6.entity = "child2";
        c6.component = GLTFSpotLight.class;
        c6.field = "light.intensity";
        c6.values = time -> (float) Math.sin(time * 2) * 90 + 90;
        clip.channels.add(c6);

        return clip;
    }

    public static AnimationClip createAnimationData2() {

        AnimationClip clip = new AnimationClip();

        // Channel - root.rotation
        AnimationChannel c1 = new AnimationChannel();
        c1.component = EnhancedPosition.class;
        c1.field = "rotation";
        c1.values = time -> new Vector3((time + 1) * 100, (time + 1) * 100, (time + 1) * 100);
        clip.channels.add(c1);

        // Channel - child1.rotation
        AnimationChannel c2 = new AnimationChannel();
        c2.entity = "child1";
        c2.component = EnhancedPosition.class;
        c2.field = "rotation";
        c2.values = time -> new Vector3((time + 1) * 100, 0, 0);
        clip.channels.add(c2);

        // Channel - child2.rotation
        AnimationChannel c3 = new AnimationChannel();
        c3.entity = "child2";
        c3.component = EnhancedPosition.class;
        c3.field = "rotation";
        c3.values = time -> new Vector3(0, (time + 1) * 100, 0);
        clip.channels.add(c3);

        // Channel - child2.scale
        AnimationChannel c4 = new AnimationChannel();
        c4.entity = "child2";
        c4.component = EnhancedPosition.class;
        c4.field = "scale";
        c4.values = time -> new Vector3(
                (float) Math.sin((time + 1) * 6) * 0.2f + 0.8f,
                (float) Math.sin((time + 1) * 2.7) * 0.2f + 0.8f,
                (float) Math.sin((time + 1) * 10) * 0.2f + 0.8f
        );
        clip.channels.add(c4);

        // Channel - child2.light.color
        AnimationChannel c5 = new AnimationChannel();
        c5.entity = "child2";
        c5.component = GLTFSpotLight.class;
        c5.field = "light.color";
        c5.values = time -> new Color(
                (float) Math.sin((time + 1) * 6) * 0.2f + 0.8f,
                (float) Math.sin((time + 1) * 2.7) * 0.2f + 0.8f,
                (float) Math.sin((time + 1) * 10) * 0.2f + 0.8f
                , 1f
        );
        clip.channels.add(c5);

        // Channel - child2.light.intensity
        AnimationChannel c6 = new AnimationChannel();
        c6.entity = "child2";
        c6.component = GLTFSpotLight.class;
        c6.field = "light.intensity";
        c6.values = time -> (float) Math.sin((time + 1) * 2) * 90 + 90;
        clip.channels.add(c6);

        return clip;
    }

    public static class TestAnimationController extends DefaultAnimationController {{
        setInitState("state1");
        addState("state1", "clip1");
        addState("state2", "clip2");
        addState("state3", "clip3");
        addTransition(new Transition() {{
            this.start = 0;
            this.end = 4;
            this.nextState = "state2";
            this.canSwitch = instance -> Gdx.input.isKeyPressed(Input.Keys.NUM_2);
        }});
        addTransition(new Transition() {{
            this.start = 0;
            this.end = 4;
            this.nextState = "state1";
            this.canSwitch = instance -> Gdx.input.isKeyPressed(Input.Keys.NUM_1);
        }});
    }}
}
