package org.apache.dubbo.common;

import org.apache.dubbo.common.extension.ExtensionFactory;
import org.apache.dubbo.common.extension.factory.SpiExtensionFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.TreeSet;

public class Test {
    public static void main(String[] args) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
//        final TestLoader<Version> loader = TestLoader.getLoader(Version.class);
//        final Version v = loader.getExt();

//        final TestLoader<Version> objectTestLoader = new TestLoader<>();
//        final Version ext = objectTestLoader.getExt();

        final TreeSet<Class> extensionFactories = new TreeSet<>();
        extensionFactories.add(SpiExtensionFactory.class);
    }
}
