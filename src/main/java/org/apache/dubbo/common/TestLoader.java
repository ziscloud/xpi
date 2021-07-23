package org.apache.dubbo.common;

import java.lang.reflect.InvocationTargetException;

public class TestLoader<T> {
//    private final Class<T> type;

//    public TestLoader(Class<T> type) {
//        this.type = type;
//    }

//    public static <T> TestLoader<T> getLoader(Class<T> type) {
//        return new TestLoader<>(type);
//    }

    public T getExt() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
//        return type.getDeclaredConstructor().newInstance();
        return null;
    }
}