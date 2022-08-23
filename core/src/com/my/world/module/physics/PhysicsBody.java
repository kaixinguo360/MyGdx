package com.my.world.module.physics;

import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld;
import com.my.world.core.Config;
import com.my.world.core.Entity;
import com.my.world.core.Scene;
import com.my.world.module.common.BaseActivatableComponent;
import com.my.world.module.common.Position;
import lombok.Getter;

public abstract class PhysicsBody extends BaseActivatableComponent {

    // ----- Static ----- //
    public final static short STATIC_FLAG = 1 << 8;
    public final static short NORMAL_FLAG = 1 << 9;
    public final static short ALL_FLAG = -1;

    @Config
    public int group = NORMAL_FLAG;

    @Config
    public int mask = ALL_FLAG;

    @Config
    public boolean isTrigger;

    protected Scene scene;
    protected PhysicsSystem physicsSystem;
    protected btDynamicsWorld dynamicsWorld;

    @Getter protected Entity entity;
    @Getter protected Position position;

    @Getter protected boolean registeredToPhysicsSystem = false;
    @Getter protected boolean enteredWorld = false;

    // ----- Physics System ----- //

    public void registerToPhysicsSystem(Scene scene, Entity entity, PhysicsSystem physicsSystem) {
        this.scene = scene;
        this.physicsSystem = physicsSystem;
        this.dynamicsWorld = physicsSystem.getDynamicsWorld();
        this.entity = entity;
        this.position = entity.getComponent(Position.class);
        this.registeredToPhysicsSystem = true;
    }

    public void unregisterFromPhysicsSystem(Scene scene, Entity entity, PhysicsSystem physicsSystem) {
        this.registeredToPhysicsSystem = false;
        this.position = null;
        this.entity = null;
        this.dynamicsWorld = null;
        this.physicsSystem = null;
        this.scene = null;
    }

    // ----- Dynamics World ----- //

    public void enterWorld() {
        this.enteredWorld = true;
    }

    public void leaveWorld() {
        this.enteredWorld = false;
    }

    public void reenterWorld() {
        if (!registeredToPhysicsSystem) throw new RuntimeException("This component not attached to a PhysicsSystem: " + this);
        if (!enteredWorld) throw new RuntimeException("This component not entered DynamicsWorld: " + this);
        leaveWorld();
        enterWorld();
    };

    // ----- Sync Transform ----- //

    public abstract void syncTransformFromEntity();

    public abstract void syncTransformFromWorld();
}
