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
public class FixedConstraint extends Constraint {

    @Config
    public Matrix4 frameInA;
    @Config
    public Matrix4 frameInB;

    public FixedConstraint(Entity base, Matrix4 frameInA, Matrix4 frameInB) {
        super(base);
        this.frameInA = frameInA;
        this.frameInB = frameInB;
    }

    @Override
    public btTypedConstraint get(btRigidBody base, btRigidBody self) {
        return new btFixedConstraint(base, self, frameInA, frameInB);
    }
}
