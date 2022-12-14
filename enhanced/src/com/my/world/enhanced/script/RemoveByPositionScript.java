package com.my.world.enhanced.script;

import com.badlogic.gdx.math.Vector3;
import com.my.world.core.Config;
import com.my.world.core.Entity;
import com.my.world.core.Scene;
import com.my.world.module.common.Position;
import com.my.world.module.script.ScriptSystem;

public class RemoveByPositionScript implements ScriptSystem.OnStart, ScriptSystem.OnUpdate {

    @Config
    public float maxDistance = 10000;

    private static final Vector3 TMP_1 = new Vector3();

    private Position position;
    private boolean canHandle;

    @Override
    public void start(Scene scene, Entity entity) {
        this.canHandle = entity.contain(Position.class);
        this.position = entity.getComponent(Position.class);
    }

    @Override
    public void update(Scene scene, Entity entity) {
        if (!canHandle) return;
        float dst = position.getGlobalTransform().getTranslation(TMP_1).dst(0, 0, 0);
        if (dst > maxDistance) {
            scene.getEntityManager().getBatch().removeEntity(entity.getId());
        }
    }
}
