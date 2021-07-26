package org.neuronbit.xpi.common.extension;

import org.neuronbit.xpi.common.lang.Prioritized;
import org.neuronbit.xpi.common.logger.Logger;
import org.neuronbit.xpi.common.logger.LoggerFactory;
import org.neuronbit.xpi.common.utils.ArrayUtils;
import org.neuronbit.xpi.common.utils.ClassUtils;
import org.neuronbit.xpi.common.utils.ConcurrentHashSet;
import org.neuronbit.xpi.common.utils.Holder;
import org.neuronbit.xpi.common.utils.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Arrays.asList;
import static java.util.ServiceLoader.load;
import static java.util.stream.StreamSupport.stream;
import static org.neuronbit.xpi.common.constants.Constants.COMMA_SPLIT_PATTERN;

public class ExtensionClassLoader {
    private static final Logger logger = LoggerFactory.getLogger(ExtensionClassLoader.class);

    private static LoadingStrategy[] strategies = loadLoadingStrategies();

    private final Class<?> type;

    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();
    private final ConcurrentMap<Class<?>, String> cachedNames = new ConcurrentHashMap<>();
    private final Map<String, Object> cachedActivates = Collections.synchronizedMap(new LinkedHashMap<>());

    private final Map<String, IllegalStateException> exceptions = new ConcurrentHashMap<>();
    /**
     * Record all unacceptable exceptions when using SPI
     */
    private final Set<String> unacceptableExceptions = new ConcurrentHashSet<>();

    private volatile Class<?> cachedAdaptiveClass = null;
    private Set<Class<?>> cachedWrapperClasses = null;

    public ExtensionClassLoader(Class<?> type) {
        this.type = type;
    }

    /**
     * Load all {@link Prioritized prioritized} {@link LoadingStrategy Loading Strategies} via {@link ServiceLoader}
     *
     * @return non-null
     */
    private static LoadingStrategy[] loadLoadingStrategies() {
        return stream(load(LoadingStrategy.class).spliterator(), false)
                       .sorted()
                       .toArray(LoadingStrategy[]::new);
    }

    /**
     * Get all {@link LoadingStrategy Loading Strategies}
     *
     * @return non-null
     * @see LoadingStrategy
     * @see Prioritized
     */
    public static List<LoadingStrategy> getLoadingStrategies() {
        return asList(strategies);
    }

    public static void setLoadingStrategies(LoadingStrategy... strategies) {
        if (ArrayUtils.isNotEmpty(strategies)) {
            ExtensionClassLoader.strategies = strategies;
        }
    }

    public Map<String, Class<?>> getExtensionClasses() {
        return loadExtensionClasses();
    }

    public Class<?> getExtensionClass(String name) {
        final Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null || unacceptableExceptions.contains(name)) {
            throw findException(name);
        }
        return clazz;
    }

    public Set<Class<?>> getExtensionWrapperClasses() {
        loadExtensionClasses();
        return cachedWrapperClasses;
    }

    public boolean containsExtension(String name) {
        return getExtensionClasses().containsKey(name);
    }

    /**
     * synchronized in getExtensionClasses
     */
    private Map<String, Class<?>> loadExtensionClasses() {
        Map<String, Class<?>> classes = cachedClasses.get();
        if (classes != null) {
            return classes;
        }
        synchronized (cachedClasses) {
            classes = cachedClasses.get();
            if (classes != null) {
                return classes;
            }

            Map<String, Class<?>> extensionClasses = new HashMap<>();
            for (LoadingStrategy strategy : strategies) {
                loadDirectory(extensionClasses, type.getName(), strategy);
            }
            cachedClasses.set(extensionClasses);
            return extensionClasses;
        }
    }

    public String getExtensionName(Class<?> extensionClass) {
        loadExtensionClasses();
        return cachedNames.get(extensionClass);
    }

    public Map<String, Object> getActivates() {
        loadExtensionClasses();
        return cachedActivates;
    }

    public Class<?> getAdaptiveExtensionClass(String name) {
        loadExtensionClasses();
        if (cachedAdaptiveClass != null) {
            return cachedAdaptiveClass;
        }
        return cachedAdaptiveClass = createAdaptiveExtensionClass(name);
    }

    private Class<?> createAdaptiveExtensionClass(String name) {
        String code = new AdaptiveClassCodeGenerator(type, name).generate();
        ClassLoader classLoader = findClassLoader();
        org.neuronbit.xpi.common.compiler.Compiler compiler = ExtensionFactory.getExtensionFactory(org.neuronbit.xpi.common.compiler.Compiler.class).getAdaptiveExtension();
        return compiler.compile(code, classLoader);
    }

    private void loadDirectory(Map<String, Class<?>> extensionClasses, String type, LoadingStrategy strategy) {
        String fileName = strategy.directory() + type;
        try {
            Enumeration<URL> urls = null;
            ClassLoader classLoader = findClassLoader();

            // try to load from ExtensionFactory's ClassLoader first
            if (strategy.preferExtensionClassLoader()) {
                ClassLoader extensionLoaderClassLoader = ExtensionFactory.class.getClassLoader();
                if (ClassLoader.getSystemClassLoader() != extensionLoaderClassLoader) {
                    urls = extensionLoaderClassLoader.getResources(fileName);
                }
            }

            if (urls == null || !urls.hasMoreElements()) {
                if (classLoader != null) {
                    urls = classLoader.getResources(fileName);
                } else {
                    urls = ClassLoader.getSystemResources(fileName);
                }
            }

            if (urls != null) {
                while (urls.hasMoreElements()) {
                    java.net.URL resourceURL = urls.nextElement();
                    loadResource(extensionClasses, classLoader, resourceURL, strategy.overridden(), strategy.excludedPackages());
                }
            }
        } catch (Throwable t) {
            logger.error("Exception occurred when loading extension class (interface: " +
                                 type + ", description file: " + fileName + ").", t);
        }
    }

    private void loadResource(Map<String, Class<?>> extensionClasses, ClassLoader classLoader,
                              java.net.URL resourceURL, boolean overridden, String... excludedPackages) {
        try {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceURL.openStream(), StandardCharsets.UTF_8))) {
                String line;
                String clazz = null;
                while ((line = reader.readLine()) != null) {
                    final int ci = line.indexOf('#');
                    if (ci >= 0) {
                        line = line.substring(0, ci);
                    }
                    line = line.trim();
                    if (line.length() > 0) {
                        try {
                            String name = null;
                            int i = line.indexOf('=');
                            if (i > 0) {
                                name = line.substring(0, i).trim();
                                clazz = line.substring(i + 1).trim();
                            } else {
                                clazz = line;
                            }
                            if (StringUtils.isNotEmpty(clazz) && !isExcluded(clazz, excludedPackages)) {
                                loadClass(extensionClasses, resourceURL, Class.forName(clazz, true, classLoader), name, overridden);
                            }
                        } catch (Throwable t) {
                            IllegalStateException e =
                                    new IllegalStateException("Failed to load extension class (interface: "
                                                                      + type + ", class line: " + line + ") in "
                                                                      + resourceURL + ", cause: " + t.getMessage(), t);
                            exceptions.put(line, e);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            logger.error("Exception occurred when loading extension class (interface: " +
                                 type + ", class file: " + resourceURL + ") in " + resourceURL, t);
        }
    }

    private boolean isExcluded(String className, String... excludedPackages) {
        if (excludedPackages != null) {
            for (String excludePackage : excludedPackages) {
                if (className.startsWith(excludePackage + ".")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void loadClass(Map<String, Class<?>> extensionClasses, java.net.URL resourceURL, Class<?> clazz, String name,
                           boolean overridden) throws NoSuchMethodException {
        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Error occurred when loading extension class (interface: " +
                                                    type + ", class line: " + clazz.getName() + "), class "
                                                    + clazz.getName() + " is not subtype of interface.");
        }
        if (clazz.isAnnotationPresent(Adaptive.class)) {
            cacheAdaptiveClass(clazz, overridden);
        } else if (isWrapperClass(clazz)) {
            cacheWrapperClass(clazz);
        } else {
            if (StringUtils.isEmpty(name)) {
                name = findAnnotationName(clazz);
                if (name.length() == 0) {
                    throw new IllegalStateException("No such extension name for the class " + clazz.getName() + " in the config " + resourceURL);
                }
            }

            String[] names = COMMA_SPLIT_PATTERN.split(name);
            if (ArrayUtils.isNotEmpty(names)) {
                cacheActivateClass(clazz, names[0]);
                for (String n : names) {
                    cacheName(clazz, n);
                    saveInExtensionClass(extensionClasses, clazz, n, overridden);
                }
            }
        }
    }

    private static ClassLoader findClassLoader() {
        return ClassUtils.getClassLoader(ExtensionClassLoader.class);
    }

    /**
     * put clazz in extensionClasses
     */
    private void saveInExtensionClass(Map<String, Class<?>> extensionClasses, Class<?> clazz, String name, boolean overridden) {
        Class<?> c = extensionClasses.get(name);
        if (c == null || overridden) {
            extensionClasses.put(name, clazz);
        } else if (c != clazz) {
            // duplicate implementation is unacceptable
            unacceptableExceptions.add(name);
            String duplicateMsg = "Duplicate extension " + type.getName() + " name " + name + " on " + c.getName() + " and " + clazz.getName();
            logger.error(duplicateMsg);
            throw new IllegalStateException(duplicateMsg);
        }
    }

    /**
     * test if clazz is a wrapper class
     * <p>
     * which has Constructor with given class type as its only argument
     */
    private boolean isWrapperClass(Class<?> clazz) {
        try {
            clazz.getConstructor(type);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private String findAnnotationName(Class<?> clazz) {
        String name = clazz.getSimpleName();
        if (name.endsWith(type.getSimpleName())) {
            name = name.substring(0, name.length() - type.getSimpleName().length());
        }
        return name.toLowerCase();
    }

    /**
     * cache name
     */
    private void cacheName(Class<?> clazz, String name) {
        if (!cachedNames.containsKey(clazz)) {
            cachedNames.put(clazz, name);
        }
    }

    /**
     * Register new extension via API
     *
     * @param name  extension name
     * @param clazz extension class
     * @throws IllegalStateException when extension with the same name has already been registered.
     */
    public void addExtension(String name, Class<?> clazz) {
        loadExtensionClasses();

        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Input type " +
                                                    clazz + " doesn't implement the Extension " + type);
        }
        if (clazz.isInterface()) {
            throw new IllegalStateException("Input type " +
                                                    clazz + " can't be interface!");
        }

        if (!clazz.isAnnotationPresent(Adaptive.class)) {
            if (StringUtils.isBlank(name)) {
                throw new IllegalStateException("Extension name is blank (Extension " + type + ")!");
            }
            if (cachedClasses.get().containsKey(name)) {
                throw new IllegalStateException("Extension name " +
                                                        name + " already exists (Extension " + type + ")!");
            }

            cachedNames.put(clazz, name);
            cachedClasses.get().put(name, clazz);
        } else {
            if (cachedAdaptiveClass != null) {
                throw new IllegalStateException("Adaptive Extension already exists (Extension " + type + ")!");
            }

            cachedAdaptiveClass = clazz;
        }
    }

    /**
     * cache Activate class which is annotated with <code>Activate</code>
     * <p>
     * for compatibility, also cache class with old alibaba Activate annotation
     */
    private void cacheActivateClass(Class<?> clazz, String name) {
        Activate activate = clazz.getAnnotation(Activate.class);
        if (activate != null) {
            cachedActivates.put(name, activate);
        }
    }

    /**
     * cache Adaptive class which is annotated with <code>Adaptive</code>
     */
    private void cacheAdaptiveClass(Class<?> clazz, boolean overridden) {
        if (cachedAdaptiveClass == null || overridden) {
            cachedAdaptiveClass = clazz;
        } else if (!cachedAdaptiveClass.equals(clazz)) {
            throw new IllegalStateException("More than 1 adaptive class found: "
                                                    + cachedAdaptiveClass.getName()
                                                    + ", " + clazz.getName());
        }
    }

    /**
     * cache wrapper class
     * <p>
     * like: ProtocolFilterWrapper, ProtocolListenerWrapper
     */
    private void cacheWrapperClass(Class<?> clazz) {
        if (cachedWrapperClasses == null) {
            cachedWrapperClasses = new ConcurrentHashSet<>();
        }
        cachedWrapperClasses.add(clazz);
    }

    private IllegalStateException findException(String name) {
        StringBuilder buf = new StringBuilder("No such extension " + type.getName() + " by name " + name);

        int i = 1;
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (entry.getKey().toLowerCase().startsWith(name.toLowerCase())) {
                if (i == 1) {
                    buf.append(", possible causes: ");
                }
                buf.append("\r\n(");
                buf.append(i++);
                buf.append(") ");
                buf.append(entry.getKey());
                buf.append(":\r\n");
                buf.append(StringUtils.toString(entry.getValue()));
            }
        }

        if (i == 1) {
            buf.append(", no related exception was found, please check whether related SPI module is missing.");
        }
        return new IllegalStateException(buf.toString());
    }
}
