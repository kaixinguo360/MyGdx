package com.my.utils.world.sys;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.my.utils.world.System;
import com.my.utils.world.*;
import com.my.utils.world.com.Camera;
import com.my.utils.world.com.Position;
import com.my.utils.world.util.pool.Matrix4Pool;
import com.my.utils.world.util.pool.QuaternionPool;
import com.my.utils.world.util.pool.Vector3Pool;

import java.util.*;

public class CameraSystem extends BaseSystem implements EntityListener, System.OnUpdate, System.OnStart,
        Loadable.OnLoad, Loadable.OnGetConfig {

    private RenderSystem renderSystem;
    private EnvironmentSystem environmentSystem;

    private final List<CameraInner> cameraInners = new LinkedList<>();
    private final List<SkyBoxInner> skyBoxInners = new LinkedList<>();

    @Override
    public boolean isHandleable(Entity entity) {
        return entity.contain(Camera.class);
    }

    @Override
    public void start(World world) {
        renderSystem = world.getSystemManager().getSystem(RenderSystem.class);
        environmentSystem = world.getSystemManager().getSystem(EnvironmentSystem.class);
    }

    @Override
    public void afterEntityAdded(Entity entity) {
        CameraInner cameraInner = new CameraInner();
        cameraInner.entity = entity;
        cameraInner.camera = entity.getComponent(Camera.class);
        cameraInner.position = entity.getComponent(Position.class);
        this.cameraInners.add(cameraInner);
        updateCameras();
    }

    @Override
    public void afterEntityRemoved(Entity entity) {
        this.cameraInners.removeIf(cameraInner -> cameraInner.entity == entity);
    }

    @Override
    public void load(Map<String, Object> config, Context context) {
        List<String> skyBoxes = (List<String>) config.get("skyBoxes");
        for (String skyBox : skyBoxes) {
            addSkyBox(skyBox);
        }
    }

    @Override
    public Map<String, Object> getConfig(Context context) {
        Map<String, Object> config = new LinkedHashMap<>();
        List<String> skyBoxes = new ArrayList<>();
        for (SkyBoxInner skyBoxInner : this.skyBoxInners) {
            skyBoxes.add(skyBoxInner.id);
        }
        config.put("skyBoxes", skyBoxes);
        return config;
    }

    @Override
    public void update(float deltaTime) {

        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();

        Gdx.gl.glViewport(0, 0, width, height);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Environment environment = environmentSystem.getEnvironment();
        Matrix4 tmpM1 = Matrix4Pool.obtain();

        for (CameraInner cameraInner : cameraInners) {
            setCamera(cameraInner.camera.followType, cameraInner.camera.perspectiveCamera, cameraInner.position.getGlobalTransform(tmpM1));
            Gdx.gl.glViewport(
                    (int) (width * cameraInner.camera.startX),
                    (int) (height * cameraInner.camera.startY),
                    (int) (width * cameraInner.camera.endX - width * cameraInner.camera.startX),
                    (int) (height * cameraInner.camera.endY - height * cameraInner.camera.startY)
            );
            for (SkyBoxInner skyBox : skyBoxInners) {
                if (skyBox.position == null) {
                    skyBox.entity = world.getEntityManager().findEntityById(skyBox.id);
                    skyBox.position = skyBox.entity.getComponent(Position.class);
                }
                skyBox.position.getLocalTransform().setToTranslation(cameraInner.camera.perspectiveCamera.position);
            }
            Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
            renderSystem.render(cameraInner.camera.perspectiveCamera, environment);
        }

        Matrix4Pool.free(tmpM1);
        Gdx.gl.glViewport(0, 0, width, height);
    }

    // ----- Custom ----- //

    public void updateCameras() {
        Collections.sort(this.cameraInners);
    }

    public void addSkyBox(String id) {
        SkyBoxInner skyBoxInner = new SkyBoxInner();
        skyBoxInner.id = id;
        this.skyBoxInners.add(skyBoxInner);
    }

    public void removeSkyBox(String id) {
        this.skyBoxInners.removeIf(skyBoxInner -> id.equals(skyBoxInner.id));
    }

    // ----- Private ----- //

    private static void setCamera(FollowType type, PerspectiveCamera camera, Matrix4 transform) {
        switch (type) {
            case A: {
                camera.position.set(0, 0.8f, -1.5f).mul(transform);
                camera.direction.set(0, 0, -1).rot(transform);
                camera.up.set(0, 1, 0).rot(transform);
                camera.update();
                break;
            }
            case B: {
                Vector3 tmpV1 = Vector3Pool.obtain();
                Quaternion tmpQ = QuaternionPool.obtain();
                Matrix4 tmpM2 = Matrix4Pool.obtain();

                transform.getTranslation(tmpV1);
                float angle = transform.getRotation(tmpQ).getAngleAround(Vector3.Y);
                tmpM2.setToTranslation(tmpV1).rotate(Vector3.Y, angle).translate(0, 0, 20);
                camera.position.setZero().mul(tmpM2);
                camera.lookAt(transform.getTranslation(tmpV1).add(0, 0, 0));
                camera.up.set(0, 1, 0);
                camera.update();

                Vector3Pool.free(tmpV1);
                QuaternionPool.free(tmpQ);
                Matrix4Pool.free(tmpM2);
            }
        }
    }

    // ----- Inner Class ----- //

    private static class CameraInner implements Comparable<CameraInner> {
        private Entity entity;
        private Camera camera;
        private Position position;

        @Override
        public int compareTo(CameraInner o) {
            return this.camera.layer - o.camera.layer;
        }
    }

    private static class SkyBoxInner {
        private String id;
        private Entity entity;
        private Position position;
    }

    public enum FollowType {
        A, B, C
    }
}
