package com.my.utils.world.sys;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.my.utils.world.System;
import com.my.utils.world.*;
import lombok.Getter;

public class KeyInputSystem extends BaseSystem implements System.OnStart {

    @Getter
    private final InputMultiplexer inputMultiplexer;

    public KeyInputSystem() {
        inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(new InputAdapter(){
            @Override
            public boolean keyDown(int keycode) {
                for (Entity entity : KeyInputSystem.this.getEntities()) {
                    for (OnKeyDown script : entity.getComponents(OnKeyDown.class)) {
                        script.keyDown(world, entity, keycode);
                    }
                }
                return false;
            }
        });
    }

    @Override
    public boolean isHandleable(Entity entity) {
        return entity.contain(OnKeyDown.class);
    }

    @Override
    public void start(World world) {
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    public interface OnKeyDown extends Component {
        void keyDown(World world, Entity entity, int keycode);
    }
}