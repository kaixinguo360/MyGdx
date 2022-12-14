package com.my.world.core;

import com.my.world.core.util.Disposable;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SystemManager implements Disposable {

    @Getter
    private final Map<Class<?>, System> systems = new LinkedHashMap<>();
    private final Map<Class<?>, System> cache1 = new LinkedHashMap<>();
    private final Map<Class<?>, List<System>> cache2 = new LinkedHashMap<>();

    private final Scene scene;

    SystemManager(Scene scene) {
        this.scene = scene;
    }

    public <T extends System> T addSystem(T system) {
        Class<? extends System> type = system.getClass();
        if (systems.containsKey(type)) throw new RuntimeException("Duplicate System: " + type);
        systems.put(type, system);
        notifyChange();
        if (system instanceof System.AfterAdded) ((System.AfterAdded) system).afterAdded(scene);
        return system;
    }
    public <T extends System> T removeSystem(Class<T> type) {
        if (!systems.containsKey(type)) throw new RuntimeException("No Such System: " + type);
        T removed = (T) systems.remove(type);
        notifyChange();
        if (removed instanceof System.AfterRemoved) ((System.AfterRemoved) removed).afterRemoved(scene);
        return removed;
    }
    public <T extends System> T getSystem(Class<T> type) {
        System cached = cache1.get(type);
        if (cached != null) {
            return (T) cached;
        } else {
            for (System system : systems.values()) {
                if (type.isInstance(system)) {
                    cache1.put(type, system);
                    return (T) system;
                }
            }
            throw new RuntimeException("No Such System: " + type);
        }
    }
    public <T extends System> List<T> getSystems(Class<T> type) {
        List<System> cached = cache2.get(type);
        if (cached != null) {
            return (List<T>) cached;
        } else {
            List<System> list = new ArrayList<>();
            for (System system : systems.values()) {
                if (type.isInstance(system)) {
                    list.add(system);
                }
            }
            cache2.put(type, list);
            return (List<T>) list;
        }
    }

    private void notifyChange() {
        cache2.clear();
    }

    @Override
    public void dispose() {
        Disposable.disposeAll(systems);
        cache2.clear();
    }
}
