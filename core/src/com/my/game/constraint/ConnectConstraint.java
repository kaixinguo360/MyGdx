package com.my.game.constraint;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.bullet.dynamics.btFixedConstraint;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btTypedConstraint;
import com.my.utils.world.Config;
import com.my.utils.world.Entity;
import com.my.utils.world.com.Constraint;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ConnectConstraint extends Constraint {

    @Config
    public float breakingImpulseThreshold;

    public ConnectConstraint(Entity base, float breakingImpulseThreshold) {
        super(base);
        this.breakingImpulseThreshold = breakingImpulseThreshold;
    }

    @Override
    public btTypedConstraint get(btRigidBody base, btRigidBody self) {
        Matrix4 tmp1 = new Matrix4();
        Matrix4 tmp2 = new Matrix4();
        tmp1.set(base.getWorldTransform());
        tmp2.set(self.getWorldTransform());
        tmp1.inv().mul(tmp2);
        tmp2.idt();
        btTypedConstraint constraint = new btFixedConstraint(base, self, tmp1, tmp2);
        constraint.setBreakingImpulseThreshold(breakingImpulseThreshold);
        return constraint;
    }
}
