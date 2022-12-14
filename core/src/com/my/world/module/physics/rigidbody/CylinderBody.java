package com.my.world.module.physics.rigidbody;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCylinderShape;
import com.my.world.core.Config;
import com.my.world.core.Configurable;
import com.my.world.module.physics.TemplateRigidBody;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class CylinderBody extends TemplateRigidBody implements Configurable.OnInit {

    @Config private Vector3 halfExtents;

    public CylinderBody(Vector3 halfExtents, float mass) {
        this(halfExtents, mass, false);
    }

    public CylinderBody(Vector3 halfExtents, float mass, boolean isTrigger) {
        super(isTrigger);
        this.halfExtents = halfExtents;
        this.mass = mass;
        init();
    }

    @Override
    public void init() {
        shape = new btCylinderShape(halfExtents);
        super.init();
    }
}
