package com.my.utils.world.util.pool;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public class Pool<T> {

    private final Supplier<T> supplier;
    private final List<T> objects = new LinkedList<>();

    public Pool(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T obtain() {
        if (!objects.isEmpty()) {
            return objects.remove(0);
        } else {
            return supplier.get();
        }
    }

    public void free(T obj) {
        if (obj == null) throw new RuntimeException("object cannot be null");
        objects.add(obj);
    }

    public void clear() {
        objects.clear();
    }
}
