package com.my.game.script;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector3;
import com.my.utils.world.Config;
import com.my.utils.world.Entity;
import com.my.utils.world.Prefab;
import com.my.utils.world.World;
import com.my.utils.world.com.Camera;
import com.my.utils.world.sys.KeyInputSystem;
import com.my.utils.world.sys.ScriptSystem;

public class AircraftScript extends EmitterScript implements ScriptSystem.OnStart, ScriptSystem.OnUpdate, KeyInputSystem.OnKeyDown {

    private AircraftController aircraftController_L;
    private AircraftController aircraftController_R;
    private AircraftController aircraftController_T;

    @Config public Prefab bulletPrefab;
    @Config public Vector3 bulletVelocity = new Vector3(0, 0, -2000);
    private final static Vector3 bulletOffset = new Vector3(0, 0, -5);

    @Config public Prefab bombPrefab;
    @Config public Vector3 bombVelocity = new Vector3(0, -10, 0);
    private final static Vector3 bombOffset = new Vector3(0, -2, 0);

    @Override
    public void start(World world, Entity entity) {
        super.start(world, entity);

        main = entity.findChildByName("body");
        this.camera = main.getComponent(Camera.class);

        Entity rotate_L = entity.findChildByName("rotate_L");
        Entity rotate_R = entity.findChildByName("rotate_R");
        Entity rotate_T = entity.findChildByName("rotate_T");
        parts.add(rotate_L);
        parts.add(rotate_R);
        parts.add(rotate_T);
        parts.add(entity.findChildByName("engine"));
        parts.add(entity.findChildByName("wing_L1"));
        parts.add(entity.findChildByName("wing_L2"));
        parts.add(entity.findChildByName("wing_R1"));
        parts.add(entity.findChildByName("wing_R2"));
        parts.add(entity.findChildByName("wing_TL"));
        parts.add(entity.findChildByName("wing_TR"));
        parts.add(entity.findChildByName("wing_VL"));
        parts.add(entity.findChildByName("wing_VR"));

        if (rotate_L.contains(AircraftController.class))
            aircraftController_L = rotate_L.getComponent(AircraftController.class);
        if (rotate_R.contains(AircraftController.class))
            aircraftController_R = rotate_R.getComponent(AircraftController.class);
        if (rotate_T.contains(AircraftController.class))
            aircraftController_T = rotate_T.getComponent(AircraftController.class);
    }

    @Override
    public void update(World world, Entity entity) {
        if (camera != null && !disabled) {
            float v1 = 1f;
            float v2 = 0.5f;
            if (aircraftController_L != null && aircraftController_R != null) {
                if (Gdx.input.isKeyPressed(Input.Keys.W)) aircraftController_T.rotate(v1);
                if (Gdx.input.isKeyPressed(Input.Keys.S)) aircraftController_T.rotate(-v1);
                if (Gdx.input.isKeyPressed(Input.Keys.A)) {
                    aircraftController_L.rotate(v2);
                    aircraftController_R.rotate(-v2);
                }
                if (Gdx.input.isKeyPressed(Input.Keys.D)) {
                    aircraftController_L.rotate(-v2);
                    aircraftController_R.rotate(v2);
                }
            }
            if (Gdx.input.isKeyPressed(Input.Keys.J)) fire(bulletPrefab, bulletVelocity, bulletOffset, (float) Math.random());
            if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) explode();
        }
    }

    @Override
    public void keyDown(World world, Entity entity, int keycode) {
        if (camera == null) return;
        if (keycode == Input.Keys.TAB) changeCamera();
        if (keycode == Input.Keys.SHIFT_LEFT && !disabled) changeCameraFollowType();
        if (camera != null && !disabled) {
            if (keycode == Input.Keys.K) fire(bombPrefab, bombVelocity, AircraftScript.bombOffset, (float) Math.random());
        }
    }

}
