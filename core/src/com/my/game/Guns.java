package com.my.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btCylinderShape;
import com.badlogic.gdx.physics.bullet.dynamics.btHingeConstraint;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btTypedConstraint;
import com.badlogic.gdx.utils.ArrayMap;
import com.my.utils.world.*;
import com.my.utils.world.com.*;
import com.my.utils.world.sys.*;

import java.lang.System;
import java.util.Map;

public class Guns {

    public static void initAssets(AssetsManager assetsManager) {
        long attributes = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        ModelBuilder mdBuilder = new ModelBuilder();
        ArrayMap<String, Model> models = new ArrayMap<>();

        models.put("bullet", mdBuilder.createCapsule(0.5f, 2, 8, new Material(ColorAttribute.createDiffuse(Color.YELLOW)), VertexAttributes.Usage.Position));
        models.put("barrel", mdBuilder.createBox(1, 1, 5, new Material(ColorAttribute.createDiffuse(Color.GREEN)), attributes));
        models.put("gunRotate", mdBuilder.createCylinder(1, 1, 1, 8, new Material(ColorAttribute.createDiffuse(Color.CYAN)), attributes));

        assetsManager.addAsset("bullet", RenderSystem.RenderConfig.class, new RenderSystem.RenderConfig(models.get("bullet")));
        assetsManager.addAsset("barrel", RenderSystem.RenderConfig.class, new RenderSystem.RenderConfig(models.get("barrel")));
        assetsManager.addAsset("gunRotate", RenderSystem.RenderConfig.class, new RenderSystem.RenderConfig(models.get("gunRotate")));

        assetsManager.addAsset("bullet", PhysicsSystem.RigidBodyConfig.class, new PhysicsSystem.RigidBodyConfig(new btCapsuleShape(0.5f, 1), 50f));
        assetsManager.addAsset("barrel", PhysicsSystem.RigidBodyConfig.class, new PhysicsSystem.RigidBodyConfig(new btBoxShape(new Vector3(0.5f,0.5f,2.5f)), 5f));
        assetsManager.addAsset("gunRotate", PhysicsSystem.RigidBodyConfig.class, new PhysicsSystem.RigidBodyConfig(new btCylinderShape(new Vector3(0.5f,0.5f,0.5f)), 50f));

    }

    public static class GunBuilder {

        private static final String group = "group";
        // ----- Variables ----- //
        private World world;
        private AssetsManager assetsManager;

        // ----- Init ----- //
        public GunBuilder(World world) {
            this.world = world;
            this.assetsManager = world.getAssetsManager();
        }

        // ----- Builder Methods ----- //

        private int barrelNum = 0;
        private Entity createBarrel(Matrix4 transform, Entity base) {
            String id = "Barrel-" + barrelNum++;
            return addObject(
                    id, transform, new MyInstance(assetsManager, "barrel", group),
                    base == null ? null : new Constraints.ConnectConstraint(base.getId(), id, 2000)
            );
        }

        private int rotateNum = 0;
        private Entity createRotate(Matrix4 transform, ConstraintController controller, Entity base) {
            Matrix4 relTransform = new Matrix4(base.getComponent(Position.class).transform).inv().mul(transform);
            String id = "GunRotate-" + rotateNum++;
            Entity entity = addObject(
                    id, transform, new MyInstance(assetsManager, "gunRotate", group),
                    base == null ? null : new Constraints.HingeConstraint(
                            base.getId(), id,
                            relTransform.rotate(Vector3.X, 90),
                            new Matrix4().rotate(Vector3.X, 90),
                            false)
            );
            entity.addComponent(controller);
            return entity;
        }

        private int gunNum = 0;
        public Entity createGun(String baseObjectId, Matrix4 transform) {

            // Gun
            GunScript gunScript = new GunScript();

            gunScript.gunController_Y = new GunController();
            gunScript.gunController_X = new GunController(-90, 0);
            gunScript.rotate_Y = createRotate(transform.cpy().translate(0, 0.5f, 0), gunScript.gunController_Y, world.getEntityManager().getEntity(baseObjectId));
            gunScript.rotate_X = createRotate(transform.cpy().translate(0, 1.5f, 0).rotate(Vector3.Z, 90), gunScript.gunController_X, gunScript.rotate_Y);
            gunScript.barrel = createBarrel(transform.cpy().translate(0, 1.5f, -3), gunScript.rotate_X);

            // Gun Entity
            Entity entity = new Entity();
            entity.setId("Gun-" + gunNum++);
            entity.addComponent(gunScript);
            world.getEntityManager().addEntity(entity);

            return entity;
        }

        // ----- Private ----- //
        private Entity addObject(String id, Matrix4 transform, Entity entity, Constraint constraint) {
            entity.setId(id);
            world.getEntityManager().addEntity(entity)
                    .getComponent(Position.class).transform.set(transform);
            if (constraint != null) {
                entity.addComponent(constraint);
            }
            return entity;
        }
    }

    public static class GunScript extends Script implements ScriptSystem.OnStart, ScriptSystem.OnUpdate, KeyInputSystem.OnKeyDown {

        // ----- Constants ----- //
        private final static short BOMB_FLAG = 1 << 8;
        private final static short GUN_FLAG = 1 << 9;
        private final static short ALL_FLAG = -1;

        // ----- Temporary ----- //
        private static final Vector3 tmpV = new Vector3();
        private static final Matrix4 tmpM = new Matrix4();
        private static final Quaternion tmpQ = new Quaternion();

        private World world;
        private AssetsManager assetsManager;
        private PhysicsSystem physicsSystem;
        private Camera camera;

        public Entity rotate_Y, rotate_X, barrel;
        public GunController gunController_Y;
        public GunController gunController_X;

        int bulletNum;

        @Override
        public void load(Map<String, Object> config, LoadContext context) {
            super.load(config, context);
            EntityManager entityManager = context.getEnvironment("world", World.class).getEntityManager();
            rotate_Y = entityManager.getEntity((String) config.get("rotate_Y"));
            rotate_X = entityManager.getEntity((String) config.get("rotate_X"));
            barrel = entityManager.getEntity((String) config.get("barrel"));
            if (rotate_Y.contains(GunController.class)) gunController_Y = rotate_Y.getComponent(GunController.class);
            if (rotate_X.contains(GunController.class)) gunController_X = rotate_X.getComponent(GunController.class);
            bulletNum = (Integer) config.get("bulletNum");
        }

        @Override
        public Map<String, Object> getConfig(Class<Map<String, Object>> configType, LoadContext context) {
            Map<String, Object> config = super.getConfig(configType, context);
            config.put("rotate_Y", rotate_Y.getId());
            config.put("rotate_X", rotate_X.getId());
            config.put("barrel", barrel.getId());
            config.put("bulletNum", bulletNum);
            return config;
        }

        @Override
        public void start(World world, Entity entity) {
            this.world = world;
            this.assetsManager = world.getAssetsManager();
            this.physicsSystem = world.getSystemManager().getSystem(PhysicsSystem.class);
            this.camera = barrel.getComponent(Camera.class);
        }

        @Override
        public void update(World world, Entity entity) {
            if (camera == null) return;
            update();
        }

        @Override
        public void keyDown(World world, Entity entity, int keycode) {
            if (camera == null) return;
            if (keycode == Input.Keys.TAB) changeCamera();
            if (keycode == Input.Keys.SHIFT_LEFT && !disabled) changeCameraFollowType();
        }

        public void update() {
            float v = 0.025f;
            if (gunController_Y != null && gunController_X != null) {
                if (Gdx.input.isKeyPressed(Input.Keys.W)) rotate(0, -v);
                if (Gdx.input.isKeyPressed(Input.Keys.S)) rotate(0, v);
                if (Gdx.input.isKeyPressed(Input.Keys.A)) rotate(v, 0);
                if (Gdx.input.isKeyPressed(Input.Keys.D)) rotate(-v, 0);
            }
            if (Gdx.input.isKeyPressed(Input.Keys.J)) fire();
            if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) explode();
        }
        public void fire() {
            tmpM.set(getTransform()).translate(0, 0, -20 + (float) (Math.random() * 15)).rotate(Vector3.X, 90);
            getTransform().getRotation(tmpQ);
            tmpV.set(getBody().getLinearVelocity());
            tmpV.add(new Vector3(0, 0, -1).mul(tmpQ).scl(2000));
            btRigidBody body = createBullet(tmpM).getComponent(RigidBody.class).body;
            body.setLinearVelocity(tmpV);
            body.setCcdMotionThreshold(1e-7f);
            body.setCcdSweptSphereRadius(2);
        }
        public void explode() {
            System.out.println("Explosion!");
            rotate_Y.removeComponent(Constraint.class);
            rotate_X.removeComponent(Constraint.class);
            rotate_Y.removeComponent(ConstraintController.class);
            rotate_X.removeComponent(ConstraintController.class);
            barrel.removeComponent(Constraint.class);
            physicsSystem.addExplosion(getTransform().getTranslation(tmpV), 2000);
        }

        private Entity createBullet(Matrix4 transform) {
            Entity entity = new MyInstance(assetsManager, "bullet", "bullet", null,
                    new Collision(BOMB_FLAG, ALL_FLAG));
            entity.setId("Bullet-" + bulletNum++);
            world.getEntityManager().addEntity(entity).getComponent(Position.class).transform.set(transform);
            entity.addComponent(new Scripts.RemoveScript());
            entity.addComponent(new Collisions.BulletCollisionHandler());
            return entity;
        }
        public void rotate(float stepY, float stepX) {
            setDirection(gunController_Y.target + stepY, gunController_X.target + stepX);
        }
        public void setDirection(float angleY, float angleX) {
            getBody().activate();
            gunController_Y.target = angleY;
            gunController_X.target = angleX;
        }
        public Matrix4 getTransform() {
            return barrel.getComponent(Position.class).transform;
        }
        public btRigidBody getBody() {
            return barrel.getComponent(RigidBody.class).body;
        }
        public void changeCamera() {
            disabled = !disabled;
            if (!disabled) {
                camera.layer = 0;
                camera.startX = 0;
                camera.startY = 0;
                camera.endX = 1;
                camera.endY = 1;
            } else {
                camera.layer = 1;
                camera.startX = 0;
                camera.startY = 0.7f;
                camera.endX = 0.3f;
                camera.endY = 1;
            }
            world.getSystemManager().getSystem(CameraSystem.class).updateCameras();
        }
        public void changeCameraFollowType() {
            switch (camera.followType) {
                case A: camera.followType = CameraSystem.FollowType.B; break;
                case B: camera.followType = CameraSystem.FollowType.A; break;
            }
        }
    }

    public static class GunController extends ConstraintController {

        @Config public float target = 0;
        @Config public float max = 0;
        @Config public float min = 0;
        @Config public boolean limit = false;

        public GunController() {}

        private GunController(float min, float max) {
            limit = true;
            this.min = (float) Math.toRadians(min);
            this.max = (float) Math.toRadians(max);
        }

        @Override
        public void update(btTypedConstraint constraint) {
            if (limit) {
                target = Math.min(max, target);
                target = Math.max(min, target);
            }
            btHingeConstraint hingeConstraint = (btHingeConstraint) constraint;
            hingeConstraint.setLimit(target, target, 0, 0.5f);
        }
    }
}
