package main;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

public class ObjectPool<T> {
    private final Deque<T> pool = new ArrayDeque<>();
    private final Supplier<T> factory;

    public ObjectPool(Supplier<T> factory, int initialCapacity) {
        this.factory = factory;
        for (int i = 0; i < initialCapacity; i++) {
            pool.push(factory.get());
        }
    }

    public T obtain() {
        return pool.isEmpty() ? factory.get() : pool.pop();
    }

    public void release(T object) {
        pool.push(object);
    }
}