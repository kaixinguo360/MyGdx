package com.my.world.module.physics;

import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.my.world.core.Component;
import com.my.world.core.Config;
import com.my.world.core.Disposable;
import com.my.world.core.Loadable;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Collider implements Component, Loadable.OnInit, Disposable {

    // ----- Static ----- //
    public final static short STATIC_FLAG = 1 << 8;
    public final static short NORMAL_FLAG = 1 << 9;
    public final static short ALL_FLAG = -1;

    @Config(type = Config.Type.Asset)
    public RigidBodyConfig rigidBodyConfig;

    @Config
    public int group = NORMAL_FLAG;

    @Config
    public int mask = ALL_FLAG;

    public btCollisionObject collisionObject;

    public Collider(RigidBodyConfig rigidBodyConfig) {
        this.rigidBodyConfig = rigidBodyConfig;
        init();
    }

    @Override
    public void init() {
        this.collisionObject = new btCollisionObject();
        this.collisionObject.setCollisionShape(rigidBodyConfig.shape);
    }

    @Override
    public void dispose() {
        if (collisionObject != null) collisionObject.dispose();
    }
}