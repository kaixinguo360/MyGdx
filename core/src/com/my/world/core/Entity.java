package com.my.world.core;

import com.my.world.core.util.Disposable;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

public class Entity extends ActivatableRelation<Entity> implements Configurable.OnInit, Disposable {

    @Getter
    @Setter
    @Config
    private String id;

    @Getter
    @Setter
    @Config
    private String name;

    // ----- Components ----- //
    @Getter
    @Config(elementType = Component.class)
    protected final List<Component> components = new LinkedList<>();

    protected final Map<Class<?>, Component> cache1 = new LinkedHashMap<>();
    protected final Map<Class<?>, List<Component>> cache2 = new HashMap<>();

    // ----- Add & Remove & Get & Contain ----- //
    public <T extends Component> T addComponent(T component) {
        if (component == null) return null;
        components.add(component);
        if (component instanceof Component.OnAttachToEntity) {
            ((Component.OnAttachToEntity) component).attachToEntity(this);
        }
        notifyChange();
        clearCache();
        return component;
    }
    public <T extends Component> void removeComponent(Class<T> type) {
        Iterator<Component> it = components.iterator();
        while (it.hasNext()) {
            Component component = it.next();
            if (type.isInstance(component)) {
                if (component instanceof Component.OnDetachFromEntity) {
                    ((Component.OnDetachFromEntity) component).detachFromEntity(this);
                }
                it.remove();
            }
        }
        notifyChange();
        clearCache();
    }
    public <T extends Component> T getComponent(Class<T> type) {
        Component cached = cache1.get(type);
        if (cached != null) {
            return (T) cached;
        } else {
            for (Component component : components) {
                if (type.isInstance(component)) {
                    cache1.put(type, component);
                    return (T) component;
                }
            }
            cache1.put(type, null);
            return null;
        }
    }
    public <T extends Component> List<T> getComponents(Class<T> type) {
        List<Component> cached = cache2.get(type);
        if (cached != null) {
            return (List<T>) cached;
        } else {
            List<Component> list = new ArrayList<>();
            for (Component component : this.components) {
                if (type.isInstance(component)) {
                    list.add(component);
                }
            }
            cache2.put(type, list);
            return (List<T>) list;
        }
    }
    public boolean contain(Class<?> type) {
        if (!cache1.containsKey(type)) getComponent((Class<? extends Component>) type);
        return !(cache1.get(type) == null);
    }
    public boolean contain(Class<?>... types) {
        for (Class<?> type : types) {
            if (!contain(type)) {
                return false;
            }
        }
        return true;
    }
    public boolean contains(Class<? extends Component> type) {
        if (!cache2.containsKey(type)) getComponents(type);
        return !cache2.get(type).isEmpty();
    }

    @Override
    public void init() {
        for (Component component : components) {
            if (component instanceof Component.OnAttachToEntity) {
                ((Component.OnAttachToEntity) component).attachToEntity(this);
            }
        }
        if (parent != null) {
            parent.addChild(this);
        }
    }

    public void notifyChange() {
        changed = true;
    }

    private void clearCache() {
        cache1.clear();
        cache2.clear();
    }

    @Override
    public void dispose() {
        Disposable.disposeAll(components);
        cache1.clear();
        cache2.clear();
        id = null;
        name = null;
        changed = true;
    }
}
