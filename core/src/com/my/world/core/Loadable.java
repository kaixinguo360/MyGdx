package com.my.world.core;

import com.my.world.core.util.Disposable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;

/**
 * Config Example:
 * <pre>
 *     intField: 1
 *     floatField: 2.0
 *     stringField: "string"
 *     customField1:
 *          type: com.my.com.customObject1
 *          config: ...
 *     customField2:
 *          type: com.my.com.customObject2
 *          config: ...
 *     listField:
 *        - type: com.my.com.customObject3
 *          config: ...
 *        - type: com.my.com.customObject4
 *          config: ...
 * </pre>
 */
public interface Loadable extends Disposable {

    interface OnLoad extends Loadable {
        void load(Map<String, Object> config, Context context);
    }

    interface OnInit extends Loadable {
        void init();
    }

    interface OnDump extends Loadable {
        Map<String, Object> dump(Context context);
    }

    static void load(Loadable loadable, Map<String, Object> config, Context context) {
        try {
            for (Field field : getFields(loadable)) {
                field.setAccessible(true);
                Config annotation = field.getAnnotation(Config.class);
                String name = annotation.name();
                if ("".equals(name)) name = field.getName();

                Class<?> fieldType = field.getType();
                Object fieldConfig = config.get(name);
                Object obj;
                if (!List.class.isAssignableFrom(fieldType) && fieldType.isInstance(fieldConfig)) {
                    obj = fieldConfig;
                } else {
                    obj = getObject(context, fieldType, annotation, fieldConfig);
                }

                if (Modifier.isFinal(field.getModifiers())) {
                    if (!(field.get(loadable) != null && List.class.isAssignableFrom(fieldType) && obj instanceof Collection)) {
                        throw new RuntimeException("Can not set a final field: " + field);
                    }
                    List<Object> fieldList = (List<Object>) field.get(loadable);
                    fieldList.clear();
                    fieldList.addAll((Collection<?>) obj);
                } else {
                    field.set(loadable, obj);
                }
            }
        } catch (IllegalAccessException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException("Load Loadable Resource(" + loadable.getClass() + ") error: " + e.getMessage(), e);
        }
    }

    static Map<String, Object> dump(Loadable loadable, Context context) {
        Map<String, Object> map = new LinkedHashMap<>();
        try {
            for (Field field : getFields(loadable)) {
                field.setAccessible(true);
                Config annotation = field.getAnnotation(Config.class);
                String name = annotation.name();
                if ("".equals(name)) name = field.getName();
                Object obj = getConfig(context, field.getType(), annotation, field.get(loadable));
                map.put(name, obj);
            }
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException("Get config from Loadable Resource(" + loadable.getClass() + ") error: " + e.getMessage(), e);
        }
        return map;
    }

    static Object getObject(Context context, Class<?> elementType, Config annotation, Object value) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        if (value == null) {
            return null;
        } else if (List.class.isAssignableFrom(elementType)) {
            List<Object> objList = new ArrayList<>();
            for (Object value1 : (List<Object>) value) {
                Class<?> listElementType = annotation.elementType();
                if (!List.class.isAssignableFrom(listElementType) && listElementType.isInstance(value1)) {
                    objList.add(value1);
                } else {
                    objList.add(getObject(context, listElementType, annotation, value1));
                }
            }
            return objList;
        } else if (annotation.type() == Config.Type.Primitive || elementType.isPrimitive() || elementType == String.class) {
            if ((elementType == float.class || elementType == Float.class) && Number.class.isAssignableFrom(value.getClass())) {
                value = ((Number) value).floatValue();
            }
            return value;
        } else if (annotation.type() == Config.Type.Asset || Prefab.class.isAssignableFrom(elementType)) {
            String assetId = (String) value;
            AssetsManager assetsManager = context.getEnvironment(AssetsManager.CONTEXT_FIELD_NAME, AssetsManager.class);
            return assetsManager.getAsset(assetId, elementType);
        } else if (annotation.type() == Config.Type.Entity || Entity.class.isAssignableFrom(elementType)) {
            String entityId = (String) value;
            Function<String, Entity> entityFinder = context.getEnvironment(EntityManager.CONTEXT_ENTITY_PROVIDER, Function.class);
            return entityFinder.apply(entityId);
        } else if (elementType.isEnum()) {
            Method valueOf = elementType.getMethod("valueOf", String.class);
            return valueOf.invoke(null, value);
        } else {
            if (value instanceof Map && ((Map<?, ?>) value).containsKey("type")) {
                Map<String, Object> map = (Map<String, Object>) value;
                String typeName = (String) map.get("type");
                Object configValue = map.get("config");
                Class<?> type = context.getEnvironment(Engine.CONTEXT_FIELD_NAME, Engine.class).getJarManager().loadClass(typeName);
                return context.getEnvironment(LoaderManager.CONTEXT_FIELD_NAME, LoaderManager.class).load(configValue, type, context);
            } else {
                return context.getEnvironment(LoaderManager.CONTEXT_FIELD_NAME, LoaderManager.class).load(value, elementType, context);
            }
        }
    }

    static Object getConfig(Context context, Class<?> elementType, Config annotation, Object value) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (value == null) {
            return null;
        } else if (annotation.type() == Config.Type.Primitive || elementType.isPrimitive() || elementType == String.class) {
            return value;
        } else if (List.class.isAssignableFrom(elementType)) {
            List<Object> configList = new ArrayList<>();
            for (Object value1 : (List<Object>) value) {
                configList.add(getConfig(context, annotation.elementType(), annotation, value1));
            }
            return configList;
        } else if (annotation.type() == Config.Type.Asset || Prefab.class.isAssignableFrom(elementType)) {
            AssetsManager assetsManager = context.getEnvironment(AssetsManager.CONTEXT_FIELD_NAME, AssetsManager.class);
            return assetsManager.getId(elementType, value);
        } else if (annotation.type() == Config.Type.Entity || Entity.class.isAssignableFrom(elementType)) {
            return ((Entity) value).getId();
        } else if (elementType.isEnum()) {
            Method toString = elementType.getMethod("toString");
            return toString.invoke(value);
        } else {
            try {
                // Use LoaderManager <Object.class> to get config
                return context.getEnvironment(LoaderManager.CONTEXT_FIELD_NAME, LoaderManager.class).dump(value, Object.class, context);
            } catch (RuntimeException e) {
                if (!(e.getMessage().startsWith("No such loader") || e.getMessage().startsWith("Can not get config")))
                    throw e;
                // Use LoaderManager <configType> to get config
                Class<?> type = value.getClass();
                return new LinkedHashMap<String, Object>() {{
                    put("type", type.getName());
                    put("config", context.getEnvironment(LoaderManager.CONTEXT_FIELD_NAME, LoaderManager.class).dump(type.cast(value), Map.class, context));
                }};
            }
        }
    }

    static List<Field> getFields(Object obj) {
        List<Field> fields;
        fields = new ArrayList<>();
        Class<?> tmpType = obj.getClass();
        while (tmpType != null && tmpType != Object.class) {
            for (Field field : tmpType.getDeclaredFields()) {
                if (field.isAnnotationPresent(Config.class)) {
                    fields.add(field);
                }
            }
            tmpType = tmpType.getSuperclass();
        }
        return fields;
    }

    @Override
    default void dispose() {
        try {
            for (Field field : getFields(this)) {
                field.setAccessible(true);
                Class<?> type = field.getType();
                Config annotation = field.getAnnotation(Config.class);
                if (annotation.type() == Config.Type.Primitive || type.isPrimitive()) {
                    // Primitive Field
                    if (Modifier.isFinal(field.getModifiers())) throw new RuntimeException("Can not dispose a final field: " + field);
                    if (type == byte.class || type == char.class || type == short.class || type == int.class || type == long.class
                            || type == float.class || type == double.class) {
                        field.set(this, 0);
                    } else if (type == boolean.class) {
                        field.set(this, false);
                    }
                } else if (type == String.class || annotation.type() == Config.Type.Asset || annotation.type() == Config.Type.Entity
                        || Entity.class.isAssignableFrom(type) || Prefab.class.isAssignableFrom(type)) {
                    // Don't need to dispose
                    if (Modifier.isFinal(field.getModifiers())) throw new RuntimeException("Can not dispose a final field: " + field);
                    field.set(this, null);
                } else {
                    // Need to dispose
                    Object obj = field.get(this);
                    if (obj == null) continue;
                    if (obj instanceof List) {
                        Disposable.disposeAll((List<?>) obj);
                        if (!Modifier.isFinal(field.getModifiers())) field.set(this, null);
                    } else if (obj instanceof Collection) {
                        Disposable.disposeAll((Collection<?>) obj);
                        if (!Modifier.isFinal(field.getModifiers())) field.set(this, null);
                    } else if (obj instanceof Map) {
                        Disposable.disposeAll((Map<?, ?>) obj);
                        if (!Modifier.isFinal(field.getModifiers())) field.set(this, null);
                    } else {
                        if (Modifier.isFinal(field.getModifiers())) throw new RuntimeException("Can not dispose a final field: " + field);
                        Disposable.dispose(obj);
                        field.set(this, null);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Dispose Resource(" + this.getClass() + ") error: " + e.getMessage(), e);
        }
    }
}
