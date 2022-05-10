package com.my.demo.builder.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.my.world.core.Config;
import com.my.world.core.Entity;
import com.my.world.core.Scene;
import com.my.world.gdx.Matrix4Pool;
import com.my.world.gdx.Vector3Pool;
import com.my.world.module.camera.CameraSystem;
import com.my.world.module.common.Position;
import com.my.world.module.physics.PhysicsSystem;
import com.my.world.module.physics.RigidBody;
import com.my.world.module.render.Render;
import com.my.world.module.render.RenderSystem;
import com.my.world.module.script.ScriptSystem;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.badlogic.gdx.graphics.GL20.*;

public class PortalScript implements ScriptSystem.OnStart, ScriptSystem.OnUpdate, PhysicsSystem.OnCollision,
        CameraSystem.BeforeRender, CameraSystem.AfterRender {

    @Config
    public String targetPortalName;

    @Config
    public Matrix4 targetTransform;

    @Config
    public float radius = 1;

    @Config
    public Shader shader;

    private RenderSystem renderSystem;

    private Entity selfEntity;
    private Position selfPosition;
    private Render selfRender;

    private Entity targetEntity;
    private Position targetPosition;
    private PortalScript targetScript;

    private final PerspectiveCamera camera = new PerspectiveCamera();

    private static final ModelBatch batch = new ModelBatch();
    private static final SpriteBatch spriteBatch = new SpriteBatch();
    private static final FrameBuffer fbo = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

    @Override
    public void start(Scene scene, Entity entity) {
        this.renderSystem = scene.getSystemManager().getSystem(RenderSystem.class);

        this.selfEntity = entity;
        this.selfPosition = entity.getComponent(Position.class);
        this.selfRender = entity.getComponent(Render.class);
        this.selfRender.setActive(false);

        if (targetPortalName != null) {
            this.targetEntity = scene.getEntityManager().findEntityByName(targetPortalName);
            this.targetPosition = this.targetEntity.getComponent(Position.class);
            this.targetScript = this.targetEntity.getComponent(PortalScript.class);
        }
    }

    @Override
    public void beforeRender(PerspectiveCamera cam) {
        selfRender.setTransform(selfPosition);

        Matrix4 targetTransform = getTargetTransform();

        Vector3 tmpV = Vector3Pool.obtain();
        Matrix4 tmpM = Matrix4Pool.obtain();

        getConvertTransform(tmpM, targetTransform);

        this.camera.far = cam.far;
        this.camera.near = cam.near;
        this.camera.fieldOfView = cam.fieldOfView;
        this.camera.viewportWidth = cam.viewportWidth;
        this.camera.viewportHeight = cam.viewportHeight;
        this.camera.position.set(cam.position).mul(tmpM);
        this.camera.direction.set(cam.direction).rot(tmpM).nor();
        this.camera.up.set(cam.up).rot(tmpM);

        // 基于相机近剪裁平面实现传送门前方遮挡物体剔除 (问题: 不精确)
        this.camera.near = -radius + targetTransform.getTranslation(tmpV).sub(this.camera.position).dot(this.camera.direction);
        if (this.camera.near <= 0) {
            this.camera.near = 0.01f;
        }

        this.camera.update();

        Matrix4Pool.free(tmpM);
        Vector3Pool.free(tmpV);
    }

    private Matrix4 getTargetTransform() {
        Matrix4 targetTransform;
        if (targetPosition != null) {
            targetTransform = this.targetPosition.getGlobalTransform();
        } else {
            targetTransform = this.targetTransform;
        }
        return targetTransform;
    }

    private Matrix4 getConvertTransform(Matrix4 matrix4, Matrix4 targetTransform) {
        return matrix4.set(selfPosition.getGlobalTransform()).inv().mul(targetTransform);
    }

    @Override
    public void afterRender(PerspectiveCamera cam) {
        if (!selfRender.isVisible(cam)) return;

        // 传送门外虚像
        if (this.targetScript != null) {
            for (Map.Entry<Entity, Info> entry : this.targetScript.ids.entrySet()) {
                renderSystem.beginBatch(cam);

                Info info = entry.getValue();
                List<Render> renders = info.entity.getComponents(Render.class);
                info.calculateTransform();

                info.position.setGlobalTransform(info.virtualTransform);
                for (Render render : renders) {
                    render.setTransform(info.position);
                    renderSystem.addToBatch(render);
                }

                renderSystem.endBatch();

                info.position.setGlobalTransform(info.realTransform);
                for (Render render : renders) {
                    render.setTransform(info.position);
                }
            }
        }

        // 准备工作
        Gdx.gl.glEnable(GL_STENCIL_TEST);
        Gdx.gl.glClear(GL_STENCIL_BUFFER_BIT);
        Gdx.gl.glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);
        Gdx.gl.glEnable(GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL_CULL_FACE);

        // 渲染传送门轮廓至模板
        Gdx.gl.glStencilFunc(GL_ALWAYS, 1, 0xFF); //所有片段都要写入模板缓冲
        Gdx.gl.glStencilMask(0xFF); // 设置模板缓冲为可写状态
        Gdx.gl.glColorMask(false, false, false, false);
        Gdx.gl.glFrontFace(GL_CW);
        batch.begin(cam);
        batch.render(selfRender, shader);
        batch.end();
        Gdx.gl.glFrontFace(GL_CCW);
        Gdx.gl.glColorMask(true, true, true, true);

        // 渲染传送门内场景至帧缓冲
        fbo.begin();
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        renderSystem.render(camera);
        // 传送门内虚像
        for (Map.Entry<Entity, Info> entry : ids.entrySet()) {
            renderSystem.beginBatch(camera);

            Info info = entry.getValue();
            List<Render> renders = info.entity.getComponents(Render.class);
            info.calculateTransform();

            info.position.setGlobalTransform(info.virtualTransform);
            for (Render render : renders) {
                render.setTransform(info.position);
                renderSystem.addToBatch(render);
            }

            renderSystem.endBatch();

            info.position.setGlobalTransform(info.realTransform);
            for (Render render : renders) {
                render.setTransform(info.position);
            }
        }
        fbo.end();

        // 渲染帧缓冲内容至传送门轮廓
        Gdx.gl.glStencilFunc(GL_EQUAL, 1, 0xFF);
        Gdx.gl.glStencilMask(0x00);
        spriteBatch.begin();
        spriteBatch.draw(
                fbo.getColorBufferTexture(),
                0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
                0, 0, 1, 1
        );
        spriteBatch.end();
        Gdx.gl.glStencilMask(0xFF);

        // 收尾工作
        Gdx.gl.glDisable(GL_STENCIL_TEST);
    }

    private final Map<Entity, Info> ids = new HashMap<>();

    @Override
    public void collision(Entity entity) {
        String name = entity.getName();
        if (name == null) return;
        if (name.equals("ground")) return;
        if (name.equals("Box")) return;
        if (ids.containsKey(entity)) {
            Info info = ids.get(entity);
            if (!info.isKnown) {
                info.isKnown = true;
            }
        } else {
            System.out.println(selfEntity.getId() + " <- " + entity.getId());
            ids.put(entity, new Info(entity, true));
        }
    }

    @Override
    public void update(Scene scene, Entity entity) {
        Iterator<Map.Entry<Entity, Info>> it = ids.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Entity, Info> entry = it.next();
            Info info = entry.getValue();
            if (info.isKnown) {
                info.isKnown = false;
            } else {
                if (info.in) {
                    info.move();
                    it.remove();
                } else {
                    System.out.println(selfEntity.getId() + " -> " + entity.getId());
                    it.remove();
                }
            }
        }
    }

    private class Info {

        private boolean in;
        private boolean isKnown = true;
        private final Entity entity;
        private final Position position;

        private final Matrix4 realTransform = new Matrix4();
        private final Matrix4 virtualTransform = new Matrix4();
        private final Matrix4 offsetTransform = new Matrix4();

        private Info(Entity entity, boolean in) {
            this.in = in;
            this.entity = entity;
            this.position = entity.getComponent(Position.class);
            calculateTransform();
        }

        private void calculateTransform() {
            position.getGlobalTransform(this.realTransform);
            getConvertTransform(offsetTransform, getTargetTransform());
            virtualTransform.set(realTransform);
            if (in) {
                virtualTransform.mulLeft(offsetTransform);
            } else {
                Matrix4 tmpM = Matrix4Pool.obtain();
                tmpM.set(offsetTransform);
                tmpM.inv();
                virtualTransform.mulLeft(tmpM);
                Matrix4Pool.free(tmpM);
            }
        }

        private void move() {
            calculateTransform();

            position.setGlobalTransform(virtualTransform);

            RigidBody body = entity.getComponent(RigidBody.class);
            if (body != null) {
                body.body.proceedToTransform(virtualTransform);
                body.body.setLinearVelocity(body.body.getLinearVelocity().rot(offsetTransform));
                body.body.setAngularVelocity(body.body.getAngularVelocity().rot(offsetTransform));
            }

            System.out.println(selfEntity.getId() + " -> " + targetEntity.getId());
            in = !in;
            targetScript.ids.put(entity, this);
        }
    }

//    // ----- 基于斜近平面平截头体映射矩阵实现传送门前方遮挡物体剔除 (废弃: 未测试) ----- //
//
//    public void onPreRender() {
//        Vector4 cameraSpaceClipPlane = cameraSpacePlane(camera, new Vector3(0.0f, 0.0f, 0.0f), Vector3.Y, 1.0f);
//        calculateObliqueMatrix(camera.projection, cameraSpaceClipPlane);
//    }
//
//    public static Vector4 cameraSpacePlane(Camera cam, Vector3 pos, Vector3 normal, float sideSign) {
//        tmpV1.set(normal).scl(0.07f).add(pos);
//        tmpV1.mul(cam.view);
//        Vector3 point = new Vector3(0.0f, 0.0f, 0.0f).mul(tmpM.set(cam.view).inv());
//        tmpV1.sub(0.0f, point.y, 0.0f);
//        tmpV2.set(normal).mul(cam.view).nor().scl(sideSign);
//        return new Vector4(tmpV2.x, tmpV2.y, tmpV2.z, -tmpV1.dot(tmpV2));
//    }
//
//    private final static Vector3 tmpV1 = new Vector3();
//    private final static Vector3 tmpV2 = new Vector3();
//    private final static Matrix4 tmpM = new Matrix4();
//    public static void calculateObliqueMatrix(Matrix4 projection, Vector4 clipPlane) {
//        Vector4 q = new Vector4(
//                sgn(clipPlane.x),
//                sgn(clipPlane.y),
//                1.0f,
//                1.0f
//        ).mul(tmpM.set(projection).inv());
//        Vector4 c = clipPlane.scl(2.0f / clipPlane.dot(q));
//        // third row = clip plane - fourth row
//        projection.val[2] = c.x - projection.val[3];
//        projection.val[6] = c.y - projection.val[7];
//        projection.val[10] = c.z - projection.val[11];
//        projection.val[14] = c.w - projection.val[15];
//    }
//
//    public static float sgn(float a) {
//        if (a > 0.0f) return 1.0f;
//        if (a < 0.0f) return -1.0f;
//        return 0.0f;
//    }
//
//    public static class Vector4 {
//
//        private float x;
//        private float y;
//        private float z;
//        private float w;
//
//        public Vector4(float x, float y, float z, float w) {
//            this.x = x;
//            this.y = y;
//            this.z = z;
//            this.w = w;
//        }
//
//        public Vector4 mul(Matrix4 matrix) {
//            final float[] l_mat = matrix.val;
//            x = x * l_mat[Matrix4.M00] + y * l_mat[Matrix4.M01] + z * l_mat[Matrix4.M02] + w * l_mat[Matrix4.M03];
//            y = x * l_mat[Matrix4.M10] + y * l_mat[Matrix4.M11] + z * l_mat[Matrix4.M12] + w * l_mat[Matrix4.M13];
//            x = x * l_mat[Matrix4.M20] + y * l_mat[Matrix4.M21] + z * l_mat[Matrix4.M22] + w * l_mat[Matrix4.M23];
//            w = x * l_mat[Matrix4.M20] + y * l_mat[Matrix4.M21] + z * l_mat[Matrix4.M22] + w * l_mat[Matrix4.M23];
//            return this;
//        }
//
//        public float dot(Vector4 vector) {
//            return x * vector.x + y * vector.y + z * vector.z + w * vector.w;
//        }
//
//        public Vector4 scl(float f) {
//            x = x * f;
//            y = x * f;
//            x = x * f;
//            w = x * f;
//            return this;
//        }
//    }
}
