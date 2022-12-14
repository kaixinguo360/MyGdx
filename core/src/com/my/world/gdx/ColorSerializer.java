package com.my.world.gdx;

import com.badlogic.gdx.graphics.Color;
import com.my.world.core.Context;
import com.my.world.core.Serializer;

import java.util.ArrayList;
import java.util.List;

public class ColorSerializer implements Serializer, Serializer.Setter {

    @Override
    public <E, T> T load(E config, Class<T> type, Context context) {
        List<Number> values = (List<Number>) config;
        return (T) new Color(
                values.get(0).floatValue(),
                values.get(1).floatValue(),
                values.get(2).floatValue(),
                values.get(3).floatValue()
        );
    }

    @Override
    public <E, T> E dump(T obj, Class<E> configType, Context context) {
        Color color = (Color) obj;
        return (E) new ArrayList<Number>() {{
            add(color.r);
            add(color.g);
            add(color.b);
            add(color.a);
        }};
    }

    @Override
    public <E, T> boolean canSerialize(Class<E> configType, Class<T> targetType) {
        return (configType == Object.class || List.class.isAssignableFrom(configType)) && targetType == Color.class;
    }

    @Override
    public void set(Object sourceObj, Object targetObj) {
        Color source = (Color) sourceObj;
        Color target = (Color) targetObj;
        target.set(source);
    }

    @Override
    public boolean canSet(Class<?> sourceType, Class<?> targetType) {
        return Color.class.isAssignableFrom(sourceType) && Color.class.isAssignableFrom(targetType);
    }
}
