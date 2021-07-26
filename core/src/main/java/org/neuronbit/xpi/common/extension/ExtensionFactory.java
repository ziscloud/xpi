/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neuronbit.xpi.common.extension;

import org.neuronbit.xpi.common.ActivateCriteria;
import org.neuronbit.xpi.common.context.Lifecycle;
import org.neuronbit.xpi.common.extension.inject.DisableInject;
import org.neuronbit.xpi.common.extension.inject.InjectProvider;
import org.neuronbit.xpi.common.extension.support.ActivateComparator;
import org.neuronbit.xpi.common.extension.support.WrapperComparator;
import org.neuronbit.xpi.common.lang.Prioritized;
import org.neuronbit.xpi.common.logger.Logger;
import org.neuronbit.xpi.common.logger.LoggerFactory;
import org.neuronbit.xpi.common.utils.ArrayUtils;
import org.neuronbit.xpi.common.utils.CollectionUtils;
import org.neuronbit.xpi.common.utils.Holder;
import org.neuronbit.xpi.common.utils.ReflectUtils;
import org.neuronbit.xpi.common.utils.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Arrays.asList;
import static java.util.Collections.sort;
import static org.neuronbit.xpi.common.constants.Constants.COMMA_SPLIT_PATTERN;
import static org.neuronbit.xpi.common.constants.Constants.DEFAULT_KEY;
import static org.neuronbit.xpi.common.constants.Constants.REMOVE_VALUE_PREFIX;

/**
 * this class is
 * at present designed to be singleton or static (by itself totally static or uses some static fields).
 * So the instances returned from them are of process or classloader scope.
 * <p>
 * Load extensions
 * <ul>
 * <li>auto inject dependency extension </li>
 * <li>auto wrap extension in wrapper </li>
 * <li>default extension is an adaptive instance</li>
 * </ul>
 *
 * @see <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jar/jar.html#Service%20Provider">Service Provider in Java 5</a>
 * @see SPI
 * @see Adaptive
 * @see Activate
 */
public class ExtensionFactory<T> {
    private static final Logger logger = LoggerFactory.getLogger(ExtensionFactory.class);

    private static final ConcurrentMap<Class<?>, ExtensionFactory<?>> EXTENSION_FACTORY = new ConcurrentHashMap<>(64);
    private static final ConcurrentMap<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>(64);

    private final Class<?> type;
    private final String cachedDefaultName;

    private final InjectProvider injectProvider;
    private final ExtensionClassLoader extensionClassLoader;

    private final Map<String, Set<String>> cachedActivateGroups = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<String, String[]> cachedActivateValues = Collections.synchronizedMap(new LinkedHashMap<>());
    private final ConcurrentMap<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();
    private final Holder<Object> cachedAdaptiveInstance = new Holder<>();

    private volatile Throwable createAdaptiveInstanceError;

    ExtensionFactory(Class<?> type) {
        this.type = type;
        this.extensionClassLoader = new ExtensionClassLoader(type);
        this.injectProvider = (type == InjectProvider.class ? null : ExtensionFactory.getExtensionFactory(InjectProvider.class).getAdaptiveExtension());
        this.cachedDefaultName = cacheDefaultExtensionName();
    }

    private static <T> boolean withExtensionAnnotation(Class<T> type) {
        return type.isAnnotationPresent(SPI.class);
    }

    @SuppressWarnings("unchecked")
    public static <T> ExtensionFactory<T> getExtensionFactory(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Extension type == null");
        }
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type (" + type + ") is not an interface!");
        }
        if (!withExtensionAnnotation(type)) {
            throw new IllegalArgumentException("Extension type (" + type +
                                                       ") is not an extension, because it is NOT annotated with @" + SPI.class.getSimpleName() + "!");
        }

        ExtensionFactory<T> factory = (ExtensionFactory<T>) EXTENSION_FACTORY.get(type);
        if (factory == null) {
            EXTENSION_FACTORY.putIfAbsent(type, new ExtensionFactory<T>(type));
            factory = (ExtensionFactory<T>) EXTENSION_FACTORY.get(type);
        }
        return factory;
    }

    // For testing purposes only
    public static void resetExtensionFactory(Class<?> type) {
        ExtensionFactory<?> loader = EXTENSION_FACTORY.get(type);
        if (loader != null) {
            // Remove all instances associated with this loader as well
            Map<String, Class<?>> classes = loader.extensionClassLoader.getExtensionClasses();
            for (Map.Entry<String, Class<?>> entry : classes.entrySet()) {
                EXTENSION_INSTANCES.remove(entry.getValue());
            }
            classes.clear();
            EXTENSION_FACTORY.remove(type);
        }
    }

    public static void destroyAll() {
        EXTENSION_INSTANCES.forEach((_type, instance) -> {
            if (instance instanceof Lifecycle) {
                Lifecycle lifecycle = (Lifecycle) instance;
                try {
                    lifecycle.destroy();
                } catch (Exception e) {
                    logger.error("Error destroying extension " + lifecycle, e);
                }
            }
        });

        // TODO Improve extension loader, clear static refer extension instance.
        // Some extension instances may be referenced by static fields, if clear EXTENSION_INSTANCES may cause inconsistent.
        // e.g. org.neuronbit.xpi.registry.client.metadata.MetadataUtils.localMetadataService
        // EXTENSION_INSTANCES.clear();

        EXTENSION_FACTORY.clear();
    }

    public String getExtensionName(Class<?> extensionClass) {
        return extensionClassLoader.getExtensionName(extensionClass);
    }

    /**
     * This is equivalent to {@code getActivateExtension(url, key, null)}
     *
     * @param url url
     * @param key url parameter key which used to get extension point names
     * @return extension list which are activated.
     * @see #getActivateExtension(ActivateCriteria, String, String)
     */
    public List<T> getActivateExtension(ActivateCriteria url, String key) {
        return getActivateExtension(url, key, null);
    }

    /**
     * This is equivalent to {@code getActivateExtension(url, values, null)}
     *
     * @param url    url
     * @param values extension point names
     * @return extension list which are activated
     * @see #getActivateExtension(ActivateCriteria, String[], String)
     */
    public List<T> getActivateExtension(ActivateCriteria url, String[] values) {
        return getActivateExtension(url, values, null);
    }

    /**
     * This is equivalent to {@code getActivateExtension(url, url.getParameter(key).split(","), null)}
     *
     * @param url   url
     * @param key   url parameter key which used to get extension point names
     * @param group group
     * @return extension list which are activated.
     * @see #getActivateExtension(ActivateCriteria, String[], String)
     */
    public List<T> getActivateExtension(ActivateCriteria url, String key, String group) {
        String value = url.getParameter(key);
        return getActivateExtension(url, StringUtils.isEmpty(value) ? null : COMMA_SPLIT_PATTERN.split(value), group);
    }

    /**
     * Get activate extensions.
     *
     * @param url    url
     * @param values extension point names
     * @param group  group
     * @return extension list which are activated
     * @see Activate
     */
    public List<T> getActivateExtension(ActivateCriteria url, String[] values, String group) {
        List<T> activateExtensions = new ArrayList<>();
        // solve the bug of using @SPI's wrapper method to report a null pointer exception.
        TreeMap<Class<?>, T> activateExtensionsMap = new TreeMap<>(ActivateComparator.COMPARATOR);
        List<String> names = values == null ? new ArrayList<>(0) : asList(values);
        if (!names.contains(REMOVE_VALUE_PREFIX + DEFAULT_KEY)) {
            if (cachedActivateGroups.size() == 0) {
                synchronized (cachedActivateGroups) {
                    if (cachedActivateGroups.size() == 0) {
                        for (Map.Entry<String, Object> entry : extensionClassLoader.getActivates().entrySet()) {
                            String name = entry.getKey();
                            Object activate = entry.getValue();

                            String[] activateGroup, activateValue;

                            if (activate instanceof Activate) {
                                activateGroup = ((Activate) activate).group();
                                activateValue = ((Activate) activate).value();
                            } else {
                                continue;
                            }
                            cachedActivateGroups.put(name, new HashSet<>(Arrays.asList(activateGroup)));
                            cachedActivateValues.put(name, activateValue);
                        }
                    }
                }
            }

            cachedActivateGroups.forEach((name, activateGroup) -> {
                if (isMatchGroup(group, activateGroup)
                            && !names.contains(name)
                            && !names.contains(REMOVE_VALUE_PREFIX + name)
                            && isActive(cachedActivateValues.get(name), url)) {

                    activateExtensionsMap.put(getExtensionClass(name), getExtension(name));
                }
            });

            if (!activateExtensionsMap.isEmpty()) {
                activateExtensions.addAll(activateExtensionsMap.values());
            }
        }
        List<T> loadedExtensions = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            if (!name.startsWith(REMOVE_VALUE_PREFIX)
                        && !names.contains(REMOVE_VALUE_PREFIX + name)) {
                if (DEFAULT_KEY.equals(name)) {
                    if (!loadedExtensions.isEmpty()) {
                        activateExtensions.addAll(0, loadedExtensions);
                        loadedExtensions.clear();
                    }
                } else {
                    loadedExtensions.add(getExtension(name));
                }
            }
        }
        if (!loadedExtensions.isEmpty()) {
            activateExtensions.addAll(loadedExtensions);
        }
        return activateExtensions;
    }

    public List<T> getActivateExtensions() {
        List<T> activateExtensions = new ArrayList<>();
        TreeMap<Class<?>, T> activateExtensionsMap = new TreeMap<>(ActivateComparator.COMPARATOR);

        for (Map.Entry<String, Object> entry : extensionClassLoader.getActivates().entrySet()) {
            String name = entry.getKey();
            Object activate = entry.getValue();
            if (!(activate instanceof Activate)) {
                continue;
            }
            activateExtensionsMap.put(getExtensionClass(name), getExtension(name));
        }
        if (!activateExtensionsMap.isEmpty()) {
            activateExtensions.addAll(activateExtensionsMap.values());
        }

        return activateExtensions;
    }

    private boolean isMatchGroup(String group, Set<String> groups) {
        if (StringUtils.isEmpty(group)) {
            return true;
        }
        if (CollectionUtils.isNotEmpty(groups)) {
            return groups.contains(group);
        }
        return false;
    }

    private boolean isActive(String[] keys, ActivateCriteria url) {
        if (keys.length == 0) {
            return true;
        }
        for (String key : keys) {
            // @Active(value="key1:value1, key2:value2")
            String keyValue = null;
            if (key.contains(":")) {
                String[] arr = key.split(":");
                key = arr[0];
                keyValue = arr[1];
            }

            String realValue = url.getParameter(key);

            if ((keyValue != null && keyValue.equals(realValue)) || (keyValue == null && StringUtils.isNotNil(realValue))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get extension's instance. Return <code>null</code> if extension is not found or is not initialized. Pls. note
     * that this method will not trigger extension load.
     * <p>
     * In order to trigger extension load, call {@link #getExtension(String)} instead.
     *
     * @see #getExtension(String)
     */
    @SuppressWarnings("unchecked")
    public T getLoadedExtension(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Extension name == null");
        }
        Holder<Object> holder = getOrCreateHolder(name);
        return (T) holder.get();
    }

    private Holder<Object> getOrCreateHolder(String name) {
        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<>());
            holder = cachedInstances.get(name);
        }
        return holder;
    }

    /**
     * Return the list of extensions which are already loaded.
     * <p>
     * Usually {@link #getSupportedExtensions()} should be called in order to get all extensions.
     *
     * @see #getSupportedExtensions()
     */
    public Set<String> getLoadedExtensions() {
        return Collections.unmodifiableSet(new TreeSet<>(cachedInstances.keySet()));
    }

    @SuppressWarnings("unchecked")
    public List<T> getLoadedExtensionInstances() {
        List<T> instances = new ArrayList<>();
        cachedInstances.values().forEach(holder -> instances.add((T) holder.get()));
        return instances;
    }

    public Object getLoadedAdaptiveExtensionInstances() {
        return cachedAdaptiveInstance.get();
    }

    /**
     * Find the extension with the given name. If the specified name is not found, then {@link IllegalStateException}
     * will be thrown.
     */
    public T getExtension(String name) {
        return getExtension(name, true);
    }

    @SuppressWarnings("unchecked")
    public T getExtension(String name, boolean wrap) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Extension name == null");
        }
        if ("true".equals(name)) {
            return getDefaultExtension();
        }
        final Holder<Object> holder = getOrCreateHolder(name);
        Object instance = holder.get();
        if (instance == null) {
            synchronized (holder) {
                instance = holder.get();
                if (instance == null) {
                    instance = createExtension(name, wrap);
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }

    /**
     * Get the extension by specified name if found, or {@link #getDefaultExtension() returns the default one}
     *
     * @param name the name of extension
     * @return non-null
     */
    public T getOrDefaultExtension(String name) {
        return containsExtension(name) ? getExtension(name) : getDefaultExtension();
    }

    /**
     * Return default extension, return <code>null</code> if it's not configured.
     */
    public T getDefaultExtension() {
        if (StringUtils.isBlank(cachedDefaultName) || "true".equals(cachedDefaultName)) {
            return null;
        }
        //getExtensionClasses();
        return getExtension(cachedDefaultName);
    }

    public boolean hasExtension(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Extension name == null");
        }

        try {
            Class<?> c = this.getExtensionClass(name);
            return c != null;
        } catch (Exception e) {
            //ignore exception
            return false;
        }
    }

    public Set<String> getSupportedExtensions() {
        Map<String, Class<?>> clazzes = extensionClassLoader.getExtensionClasses();
        return Collections.unmodifiableSet(new TreeSet<>(clazzes.keySet()));
    }

    public Set<T> getSupportedExtensionInstances() {
        List<T> instances = new LinkedList<>();
        Set<String> supportedExtensions = getSupportedExtensions();
        if (CollectionUtils.isNotEmpty(supportedExtensions)) {
            for (String name : supportedExtensions) {
                instances.add(getExtension(name));
            }
        }
        // sort the Prioritized instances
        sort(instances, Prioritized.COMPARATOR);
        return new LinkedHashSet<>(instances);
    }

    /**
     * Return default extension name, return <code>null</code> if not configured.
     */
    public String getDefaultExtensionName() {
        return cachedDefaultName;
    }

    public void addExtension(String name, Class<?> clazz) {
        extensionClassLoader.addExtension(name, clazz);
    }

    @SuppressWarnings("unchecked")
    public T getAdaptiveExtension() {
        Object instance = cachedAdaptiveInstance.get();
        if (instance == null) {
            if (createAdaptiveInstanceError != null) {
                throw new IllegalStateException("Failed to create adaptive instance: " + createAdaptiveInstanceError.toString(),
                        createAdaptiveInstanceError);
            }

            synchronized (cachedAdaptiveInstance) {
                instance = cachedAdaptiveInstance.get();
                if (instance == null) {
                    try {
                        instance = createAdaptiveExtension();
                        cachedAdaptiveInstance.set(instance);
                    } catch (Throwable t) {
                        createAdaptiveInstanceError = t;
                        throw new IllegalStateException("Failed to create adaptive instance: " + t, t);
                    }
                }
            }
        }

        return (T) instance;
    }

    @SuppressWarnings("unchecked")
    private T createExtension(String name, boolean wrap) {
        Class<?> clazz = getExtensionClass(name);
        try {
            T instance = (T) EXTENSION_INSTANCES.get(clazz);
            if (instance == null) {
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.getDeclaredConstructor().newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            }
            injectExtension(instance);

            if (wrap) {
                final Set<Class<?>> cachedWrapperClasses = extensionClassLoader.getExtensionWrapperClasses();
                List<Class<?>> wrapperClassesList = new ArrayList<>();
                if (cachedWrapperClasses != null) {
                    wrapperClassesList.addAll(cachedWrapperClasses);
                    wrapperClassesList.sort(WrapperComparator.COMPARATOR);
                    Collections.reverse(wrapperClassesList);
                }

                if (CollectionUtils.isNotEmpty(wrapperClassesList)) {
                    for (Class<?> wrapperClass : wrapperClassesList) {
                        Wrapper wrapper = wrapperClass.getAnnotation(Wrapper.class);
                        if (wrapper == null
                                    || (ArrayUtils.contains(wrapper.matches(), name) && !ArrayUtils.contains(wrapper.mismatches(), name))) {
                            instance = injectExtension((T) wrapperClass.getConstructor(type).newInstance(instance));
                        }
                    }
                }
            }

            initExtension(instance);
            return instance;
        } catch (Throwable t) {
            throw new IllegalStateException("Extension instance (name: " + name + ", class: " +
                                                    type + ") couldn't be instantiated: " + t.getMessage(), t);
        }
    }

    private boolean containsExtension(String name) {
        return extensionClassLoader.containsExtension(name);
    }

    private T injectExtension(T instance) {
        if (injectProvider == null) {
            return instance;
        }

        try {
            for (Method method : instance.getClass().getMethods()) {
                if (!isSetter(method)) {
                    continue;
                }
                /**
                 * Check {@link DisableInject} to see if we need auto injection for this property
                 */
                if (method.getAnnotation(DisableInject.class) != null) {
                    continue;
                }
                Class<?> pt = method.getParameterTypes()[0];
                if (ReflectUtils.isPrimitives(pt)) {
                    continue;
                }

                try {
                    String property = getSetterProperty(method);
                    Object object = injectProvider.getInstance(pt, property);
                    if (object != null) {
                        method.invoke(instance, object);
                    }
                } catch (Exception e) {
                    logger.error("Failed to inject via method " + method.getName()
                                         + " of interface " + type.getName() + ": " + e.getMessage(), e);
                }

            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return instance;
    }

    private void initExtension(T instance) {
        if (instance instanceof Lifecycle) {
            Lifecycle lifecycle = (Lifecycle) instance;
            lifecycle.initialize();
        }
    }

    /**
     * get properties name for setter, for instance: setVersion, return "version"
     * <p>
     * return "", if setter name with length less than 3
     */
    private String getSetterProperty(Method method) {
        return method.getName().length() > 3 ? method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4) : "";
    }

    /**
     * return true if and only if:
     * <p>
     * 1, public
     * <p>
     * 2, name starts with "set"
     * <p>
     * 3, only has one parameter
     */
    private boolean isSetter(Method method) {
        return method.getName().startsWith("set")
                       && method.getParameterTypes().length == 1
                       && Modifier.isPublic(method.getModifiers());
    }

    private Class<?> getExtensionClass(String name) {
        if (type == null) {
            throw new IllegalArgumentException("Extension type == null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Extension name == null");
        }
        return extensionClassLoader.getExtensionClass(name);
    }

    /**
     * extract and cache default extension name if exists
     */
    private String cacheDefaultExtensionName() {
        final SPI defaultAnnotation = type.getAnnotation(SPI.class);
        if (defaultAnnotation == null) {
            return null;
        }

        String value = defaultAnnotation.value();
        if ((value = value.trim()).length() > 0) {
            String[] names = COMMA_SPLIT_PATTERN.split(value);
            if (names.length > 1) {
                throw new IllegalStateException("More than 1 default extension name on extension " + type.getName()
                                                        + ": " + Arrays.toString(names));
            }
            if (names.length == 1) {
                return names[0];
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private T createAdaptiveExtension() {
        try {
            return injectExtension(
                    (T) extensionClassLoader
                                .getAdaptiveExtensionClass(cachedDefaultName)
                                .getDeclaredConstructor()
                                .newInstance());
        } catch (Exception e) {
            throw new IllegalStateException("Can't create adaptive extension " + type + ", cause: " + e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "[" + type.getName() + "]";
    }

}
