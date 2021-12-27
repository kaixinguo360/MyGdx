package com.my.game.script;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.my.utils.world.Config;
import com.my.utils.world.Entity;
import com.my.utils.world.World;
import com.my.utils.world.com.Position;
import com.my.utils.world.com.RigidBody;
import com.my.utils.world.sys.PhysicsSystem;
import com.my.utils.world.sys.ScriptSystem;
import com.my.utils.world.util.pool.Matrix4Pool;
import com.my.utils.world.util.pool.Vector3Pool;

public class ExplosionScript implements ScriptSystem.OnStart, PhysicsSystem.OnCollision {

    @Config
    public float maxForce = 10000;

    private World world;
    private Entity self;

    @Override
    public void start(World world, Entity entity) {
        this.world = world;
        this.self = entity;
    }

    @Override
    public void collision(Entity entity) {

        if (entity.contain(RigidBody.class)) {
            Vector3 tmpV1 = Vector3Pool.obtain();
            Vector3 tmpV2 = Vector3Pool.obtain();
            Matrix4 tmpM = Matrix4Pool.obtain();

            self.getComponent(Position.class).getGlobalTransform(tmpM).getTranslation(tmpV1);
            entity.getComponent(Position.class).getLocalTransform().getTranslation(tmpV2);

            tmpV2.sub(tmpV1);
            float len2 = tmpV2.len2();
            tmpV2.nor().scl(maxForce * 1/len2);
            System.out.println("Explosion:\t" + entity.getName() + "\tforce:\t" + tmpV2.len());
            RigidBody rigidBody = entity.getComponent(RigidBody.class);
            rigidBody.body.activate();
            rigidBody.body.applyCentralImpulse(tmpV2);

            Vector3Pool.free(tmpV1);
            Vector3Pool.free(tmpV2);
            Matrix4Pool.free(tmpM);
        }

        world.getEntityManager().getBatch().removeEntity(self.getId());
    }
}