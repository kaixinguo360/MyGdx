package com.my.world.gdx;

import com.badlogic.gdx.math.Vector3;
import com.my.world.core.Context;
import com.my.world.core.Serializer;

import java.util.ArrayList;
import java.util.List;

public class Vector3Serializer implements Serializer, Serializer.Setter {

    @Override
    public <E, T> T load(E config, Class<T> type, Context context) {
        List<Number> values = (List<Number>) config;
        return (T) new Vector3(
                values.get(0).floatValue(),
                values.get(1).floatValue(),
                values.get(2).floatValue()
        );
    }

    @Override
    public <E, T> E dump(T obj, Class<E> configType, Context context) {
        Vector3 vector3 = (Vector3) obj;
        return (E) new ArrayList<Number>() {{
            add(vector3.x);
            add(vector3.y);
            add(vector3.z);
        }};
    }

    @Override
    public <E, T> boolean canSerialize(Class<E> configType, Class<T> targetType) {
        return (configType == Object.class || List.class.isAssignableFrom(configType)) && targetType == Vector3.class;
    }

    @Override
    public void set(Object sourceObj, Object targetObj) {
        Vector3 source = (Vector3) sourceObj;
        Vector3 target = (Vector3) targetObj;
        target.set(source);
    }

    @Override
    public boolean canSet(Class<?> sourceType, Class<?> targetType) {
        return Vector3.class.isAssignableFrom(sourceType) && Vector3.class.isAssignableFrom(targetType);
    }
}
