package com.my.utils.world.sys;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.dynamics.*;
import com.badlogic.gdx.utils.Array;
import com.my.utils.world.BaseSystem;
import com.my.utils.world.Entity;
import com.my.utils.world.World;
import com.my.utils.world.com.RigidBody;

public class ConstraintSystem extends BaseSystem {

    // ----- Create ----- //
    public ConstraintSystem() {
    }

    // ----- Check ----- //
    public boolean check(Entity entity) {
        return false;
    }

    // ----- Custom ----- //

    private Array<Constraint> constraints = new Array<>();
    public void addConstraint(String bodyA, String bodyB, Config config) {
        Constraint constraint = new Constraint();
        constraint.bodyA = bodyA;
        constraint.bodyB = bodyB;
        constraint.config = config;
        constraints.add(constraint);
    }
    public void addController(String bodyA, String bodyB, Controller controller) {
        Constraint constraint = get(bodyA, bodyB);
        if (constraint == null) throw new RuntimeException("No Such Constraint: [" + bodyA + "]-[" + bodyB + "]");
        constraint.controller = controller;
    }
    private Constraint get(String bodyA, String bodyB) {
        for (Constraint constraint : constraints) {
            if ((constraint.bodyA.equals(bodyA) && constraint.bodyB.equals(bodyB))
                    || (constraint.bodyA.equals(bodyB) && constraint.bodyB.equals(bodyA))) {
                return constraint;
            }
        }
        return null;
    }
    public void init(World world) {
        btDynamicsWorld dynamicsWorld = world.getSystem(PhysicsSystem.class).dynamicsWorld;
        for (Constraint constraint : constraints) {
            if (constraint.btConstraint == null) {
                btRigidBody bodyA = world.getEntity(constraint.bodyA).get(RigidBody.class).body;
                btRigidBody bodyB = world.getEntity(constraint.bodyB).get(RigidBody.class).body;
                constraint.btConstraint = constraint.config.get(bodyA, bodyB);
                dynamicsWorld.addConstraint(constraint.btConstraint);
            }
        }
    }
    public void update() {
        for (Constraint constraint : constraints) {
            if (constraint.btConstraint != null && constraint.controller != null) {
                constraint.controller.update(constraint.btConstraint);
            }
        }
    }
    public void clear(World world) {
        btDynamicsWorld dynamicsWorld = world.getSystem(PhysicsSystem.class).dynamicsWorld;
        for (Constraint constraint : constraints) {
            if (constraint.btConstraint != null) {
                dynamicsWorld.removeConstraint(constraint.btConstraint);
                constraint.btConstraint.dispose();
                constraint.btConstraint = null;
            }
        }
    }

    private class Constraint {
        private String bodyA;
        private String bodyB;
        private Config config;
        private Controller controller;
        private btTypedConstraint btConstraint;
    }
    public interface Controller {
        void update(btTypedConstraint constraint);
    }
    public interface Config {
        btTypedConstraint get(btRigidBody bodyA, btRigidBody bodyB);
    }
    public static class Point2PointConstraint implements Config {
        private final Vector3 pivotInA;
        private final Vector3 pivotInB;
        public Point2PointConstraint(Vector3 pivotInA, Vector3 pivotInB) {
            this.pivotInA = pivotInA;
            this.pivotInB = pivotInB;
        }
        @Override
        public btTypedConstraint get(btRigidBody bodyA, btRigidBody bodyB) {
            btPoint2PointConstraint constraint = new btPoint2PointConstraint(bodyA, bodyB, pivotInA, pivotInB);
            return constraint;
        }
    }
    public static class FixedConstraint implements Config {
        private final Matrix4 frameInA;
        private final Matrix4 frameInB;
        public FixedConstraint(Matrix4 frameInA, Matrix4 frameInB) {
            this.frameInA = frameInA;
            this.frameInB = frameInB;
        }
        @Override
        public btTypedConstraint get(btRigidBody bodyA, btRigidBody bodyB) {
            btFixedConstraint constraint = new btFixedConstraint(bodyA, bodyB, frameInA, frameInB);
            return constraint;
        }
    }
    public static class ConnectConstraint implements Config {
        private static final Matrix4 tmp1 = new Matrix4();
        private static final Matrix4 tmp2 = new Matrix4();
        @Override
        public btTypedConstraint get(btRigidBody bodyA, btRigidBody bodyB) {
            tmp1.set(bodyA.getWorldTransform());
            tmp2.set(bodyB.getWorldTransform());
            tmp1.inv().mul(tmp2);
            tmp2.idt();
            btFixedConstraint constraint = new btFixedConstraint(bodyA, bodyB, tmp1, tmp2);
            return constraint;
        }
    }
    public static class SliderConstraint implements Config {
        private final Matrix4 frameInA;
        private final Matrix4 frameInB;
        private final boolean useLinearReferenceFrameA;
        public SliderConstraint(Matrix4 frameInA, boolean useLinearReferenceFrameA) {
            this(frameInA, frameInA, useLinearReferenceFrameA);
        }
        public SliderConstraint(Matrix4 frameInA, Matrix4 frameInB, boolean useLinearReferenceFrameA) {
            this.frameInA = frameInA;
            this.frameInB = frameInB;
            this.useLinearReferenceFrameA = useLinearReferenceFrameA;
        }
        @Override
        public btTypedConstraint get(btRigidBody bodyA, btRigidBody bodyB) {
            btSliderConstraint constraint = (bodyA != bodyB) ?
                    new btSliderConstraint(bodyA, bodyB, frameInA, frameInB, useLinearReferenceFrameA) :
                    new btSliderConstraint(bodyA, frameInA, useLinearReferenceFrameA);
            return constraint;
        }
    }
    public static class HingeConstraint implements Config {
        private final Matrix4 frameInA;
        private final Matrix4 frameInB;
        private final boolean useLinearReferenceFrameA;
        public HingeConstraint(Matrix4 frameInA, boolean useLinearReferenceFrameA) {
            this(frameInA, frameInA, useLinearReferenceFrameA);
        }
        public HingeConstraint(Matrix4 frameInA, Matrix4 frameInB, boolean useLinearReferenceFrameA) {
            this.frameInA = frameInA;
            this.frameInB = frameInB;
            this.useLinearReferenceFrameA = useLinearReferenceFrameA;
        }
        @Override
        public btTypedConstraint get(btRigidBody bodyA, btRigidBody bodyB) {
            btHingeConstraint constraint = (bodyA != bodyB) ?
                    new btHingeConstraint(bodyA, bodyB, frameInA, frameInB, useLinearReferenceFrameA) :
                    new btHingeConstraint(bodyA, frameInA, useLinearReferenceFrameA);
            return constraint;
        }
    }
}