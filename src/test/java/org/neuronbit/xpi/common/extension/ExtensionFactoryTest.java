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
import org.neuronbit.xpi.common.extension.activate.ActivateExt1;
import org.neuronbit.xpi.common.extension.activate.impl.ActivateExt1Impl1;
import org.neuronbit.xpi.common.extension.activate.impl.OrderActivateExtImpl1;
import org.neuronbit.xpi.common.extension.duplicated.DuplicatedOverriddenExt;
import org.neuronbit.xpi.common.extension.duplicated.DuplicatedWithoutOverriddenExt;
import org.neuronbit.xpi.common.extension.ext1.SimpleExt;
import org.neuronbit.xpi.common.extension.ext1.impl.SimpleExtImpl1;
import org.neuronbit.xpi.common.extension.ext1.impl.SimpleExtImpl2;
import org.neuronbit.xpi.common.extension.ext10_multi_names.Ext10MultiNames;
import org.neuronbit.xpi.common.extension.ext2.Ext2;
import org.neuronbit.xpi.common.extension.ext6_wrap.WrappedExt;
import org.neuronbit.xpi.common.extension.ext6_wrap.impl.Ext5Wrapper1;
import org.neuronbit.xpi.common.extension.ext6_wrap.impl.Ext5Wrapper2;
import org.neuronbit.xpi.common.extension.ext7.InitErrorExt;
import org.neuronbit.xpi.common.extension.ext8_add.AddExt1;
import org.neuronbit.xpi.common.extension.ext8_add.AddExt2;
import org.neuronbit.xpi.common.extension.ext8_add.impl.AddExt1_ManualAdaptive;
import org.neuronbit.xpi.common.extension.ext8_add.impl.AddExt1_ManualAdd1;
import org.neuronbit.xpi.common.extension.ext8_add.impl.AddExt2_ManualAdaptive;
import org.neuronbit.xpi.common.extension.ext9_empty.Ext9Empty;
import org.neuronbit.xpi.common.extension.ext9_empty.impl.Ext9EmptyImpl;
import org.neuronbit.xpi.common.extension.injection.InjectExt;
import org.neuronbit.xpi.common.extension.injection.impl.InjectExtImpl;
import org.neuronbit.xpi.common.lang.Prioritized;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.neuronbit.xpi.common.extension.ExtensionClassLoader.getLoadingStrategies;
import static org.neuronbit.xpi.common.extension.ExtensionFactory.getExtensionFactory;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ExtensionFactoryTest {
    @Test
    public void test_getExtensionFactory_Null() throws Exception {
        try {
            getExtensionFactory(null);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(),
                    containsString("Extension type == null"));
        }
    }

    @Test
    public void test_getExtensionFactory_NotInterface() throws Exception {
        try {
            getExtensionFactory(ExtensionFactoryTest.class);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(),
                    containsString("Extension type (class org.neuronbit.xpi.common.extension.ExtensionFactoryTest) is not an interface"));
        }
    }

    @Test
    public void test_getExtensionFactory_NotSpiAnnotation() throws Exception {
        try {
            getExtensionFactory(NoSpiExt.class);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(),
                    allOf(containsString("NoSpiExt"),
                            containsString("is not an extension"),
                            containsString("NOT annotated with @SPI")));
        }
    }

    @Test
    public void test_getDefaultExtension() throws Exception {
        SimpleExt ext = getExtensionFactory(SimpleExt.class).getDefaultExtension();
        assertThat(ext, instanceOf(SimpleExtImpl1.class));

        String name = getExtensionFactory(SimpleExt.class).getDefaultExtensionName();
        assertEquals("impl1", name);
    }

    @Test
    public void test_getDefaultExtension_NULL() throws Exception {
        Ext2 ext = getExtensionFactory(Ext2.class).getDefaultExtension();
        assertNull(ext);

        String name = getExtensionFactory(Ext2.class).getDefaultExtensionName();
        assertNull(name);
    }

    @Test
    public void test_getExtension() throws Exception {
        assertTrue(getExtensionFactory(SimpleExt.class).getExtension("impl1") instanceof SimpleExtImpl1);
        assertTrue(getExtensionFactory(SimpleExt.class).getExtension("impl2") instanceof SimpleExtImpl2);
    }

    @Test
    public void test_getExtension_WithWrapper() throws Exception {
        WrappedExt impl1 = getExtensionFactory(WrappedExt.class).getExtension("impl1");
        assertThat(impl1, anyOf(instanceOf(Ext5Wrapper1.class), instanceOf(Ext5Wrapper2.class)));

        WrappedExt impl2 = getExtensionFactory(WrappedExt.class).getExtension("impl2");
        assertThat(impl2, anyOf(instanceOf(Ext5Wrapper1.class), instanceOf(Ext5Wrapper2.class)));


        ActivateCriteria url = new ActivateCriteria();
        int echoCount1 = Ext5Wrapper1.echoCount.get();
        int echoCount2 = Ext5Wrapper2.echoCount.get();

        assertEquals("Ext5Impl1-echo", impl1.echo(url, "ha"));
        assertEquals(echoCount1 + 1, Ext5Wrapper1.echoCount.get());
        assertEquals(echoCount2 + 1, Ext5Wrapper2.echoCount.get());
    }

    @Test
    public void test_getActivateExtension_WithWrapper() throws Exception {
        ActivateCriteria url = new ActivateCriteria();
        List<ActivateExt1> list = getExtensionFactory(ActivateExt1.class)
                                          .getActivateExtension(url, new String[]{}, "order");
        assertEquals(2, list.size());
    }

    @Test
    public void test_getExtension_ExceptionNoExtension() throws Exception {
        try {
            getExtensionFactory(SimpleExt.class).getExtension("XXX");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("No such extension org.neuronbit.xpi.common.extension.ext1.SimpleExt by name XXX"));
        }
    }

    @Test
    public void test_getExtension_ExceptionNoExtension_WrapperNotAffactName() throws Exception {
        try {
            getExtensionFactory(WrappedExt.class).getExtension("XXX");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("No such extension org.neuronbit.xpi.common.extension.ext6_wrap.WrappedExt by name XXX"));
        }
    }

    @Test
    public void test_getExtension_ExceptionNullArg() throws Exception {
        try {
            getExtensionFactory(SimpleExt.class).getExtension(null);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("Extension name == null"));
        }
    }

    @Test
    public void test_hasExtension() throws Exception {
        assertTrue(getExtensionFactory(SimpleExt.class).hasExtension("impl1"));
        assertFalse(getExtensionFactory(SimpleExt.class).hasExtension("impl1,impl2"));
        assertFalse(getExtensionFactory(SimpleExt.class).hasExtension("xxx"));

        try {
            getExtensionFactory(SimpleExt.class).hasExtension(null);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("Extension name == null"));
        }
    }

    @Test
    public void test_hasExtension_wrapperIsNotExt() throws Exception {
        assertTrue(getExtensionFactory(WrappedExt.class).hasExtension("impl1"));
        assertFalse(getExtensionFactory(WrappedExt.class).hasExtension("impl1,impl2"));
        assertFalse(getExtensionFactory(WrappedExt.class).hasExtension("xxx"));

        assertFalse(getExtensionFactory(WrappedExt.class).hasExtension("wrapper1"));

        try {
            getExtensionFactory(WrappedExt.class).hasExtension(null);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("Extension name == null"));
        }
    }

    @Test
    public void test_getSupportedExtensions() throws Exception {
        Set<String> exts = getExtensionFactory(SimpleExt.class).getSupportedExtensions();

        Set<String> expected = new HashSet<String>();
        expected.add("impl1");
        expected.add("impl2");
        expected.add("impl3");

        assertEquals(expected, exts);
    }

    @Test
    public void test_getSupportedExtensions_wrapperIsNotExt() throws Exception {
        Set<String> exts = getExtensionFactory(WrappedExt.class).getSupportedExtensions();

        Set<String> expected = new HashSet<String>();
        expected.add("impl1");
        expected.add("impl2");

        assertEquals(expected, exts);
    }

    @Test
    public void test_AddExtension() throws Exception {
        try {
            getExtensionFactory(AddExt1.class).getExtension("Manual1");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("No such extension org.neuronbit.xpi.common.extension.ext8_add.AddExt1 by name Manual"));
        }

        getExtensionFactory(AddExt1.class).addExtension("Manual1", AddExt1_ManualAdd1.class);
        AddExt1 ext = getExtensionFactory(AddExt1.class).getExtension("Manual1");

        assertThat(ext, instanceOf(AddExt1_ManualAdd1.class));
        assertEquals("Manual1", getExtensionFactory(AddExt1.class).getExtensionName(AddExt1_ManualAdd1.class));
        ExtensionFactory.resetExtensionFactory(AddExt1.class);
    }

    @Test
    public void test_AddExtension_NoExtend() throws Exception {
//        ExtensionFactory.getExtensionFactory(Ext9Empty.class).getSupportedExtensions();
        getExtensionFactory(Ext9Empty.class).addExtension("ext9", Ext9EmptyImpl.class);
        Ext9Empty ext = getExtensionFactory(Ext9Empty.class).getExtension("ext9");

        assertThat(ext, instanceOf(Ext9Empty.class));
        assertEquals("ext9", getExtensionFactory(Ext9Empty.class).getExtensionName(Ext9EmptyImpl.class));
    }

    @Test
    public void test_AddExtension_ExceptionWhenExistedExtension() throws Exception {
        SimpleExt ext = getExtensionFactory(SimpleExt.class).getExtension("impl1");

        try {
            getExtensionFactory(AddExt1.class).addExtension("impl1", AddExt1_ManualAdd1.class);
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Extension name impl1 already exists (Extension interface org.neuronbit.xpi.common.extension.ext8_add.AddExt1)!"));
        }
    }

    @Test
    public void test_AddExtension_Adaptive() throws Exception {
        ExtensionFactory<AddExt2> loader = getExtensionFactory(AddExt2.class);
        loader.addExtension(null, AddExt2_ManualAdaptive.class);

        AddExt2 adaptive = loader.getAdaptiveExtension();
        assertTrue(adaptive instanceof AddExt2_ManualAdaptive);
    }

    @Test
    public void test_AddExtension_Adaptive_ExceptionWhenExistedAdaptive() throws Exception {
        ExtensionFactory<AddExt1> loader = getExtensionFactory(AddExt1.class);

        loader.getAdaptiveExtension();

        try {
            loader.addExtension(null, AddExt1_ManualAdaptive.class);
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Adaptive Extension already exists (Extension interface org.neuronbit.xpi.common.extension.ext8_add.AddExt1)!"));
        }
    }

    @Test
    public void test_InitError() throws Exception {
        ExtensionFactory<InitErrorExt> loader = getExtensionFactory(InitErrorExt.class);

        loader.getExtension("ok");

        try {
            loader.getExtension("error");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Failed to load extension class (interface: interface org.neuronbit.xpi.common.extension.ext7.InitErrorExt"));
            assertThat(expected.getMessage(), containsString("java.lang.ExceptionInInitializerError"));
        }
    }

    /*@Test
    public void testLoadActivateExtension() throws Exception {
        // test default
        URL url = URL.valueOf("test://localhost/test");
        List<ActivateExt1> list = getExtensionFactory(ActivateExt1.class)
                .getActivateExtension(url, new String[]{}, "default_group");
        Assertions.assertEquals(1, list.size());
        Assertions.assertSame(list.get(0).getClass(), ActivateExt1Impl1.class);

        // test group
        url = url.addParameter(GROUP_KEY, "group1");
        list = getExtensionFactory(ActivateExt1.class)
                .getActivateExtension(url, new String[]{}, "group1");
        Assertions.assertEquals(1, list.size());
        Assertions.assertSame(list.get(0).getClass(), GroupActivateExtImpl.class);

        // test old @Activate group
        url = url.addParameter(GROUP_KEY, "old_group");
        list = getExtensionFactory(ActivateExt1.class)
                .getActivateExtension(url, new String[]{}, "old_group");
        Assertions.assertEquals(2, list.size());
        Assertions.assertTrue(list.get(0).getClass() == OldActivateExt1Impl2.class
                || list.get(0).getClass() == OldActivateExt1Impl3.class);

        // test value
        url = url.removeParameter(GROUP_KEY);
        url = url.addParameter(GROUP_KEY, "value");
        url = url.addParameter("value", "value");
        list = getExtensionFactory(ActivateExt1.class)
                .getActivateExtension(url, new String[]{}, "value");
        Assertions.assertEquals(1, list.size());
        Assertions.assertSame(list.get(0).getClass(), ValueActivateExtImpl.class);

        // test order
        url = URL.valueOf("test://localhost/test");
        url = url.addParameter(GROUP_KEY, "order");
        list = getExtensionFactory(ActivateExt1.class)
                .getActivateExtension(url, new String[]{}, "order");
        Assertions.assertEquals(2, list.size());
        Assertions.assertSame(list.get(0).getClass(), OrderActivateExtImpl1.class);
        Assertions.assertSame(list.get(1).getClass(), OrderActivateExtImpl2.class);
    }*/

    @Test
    public void testLoadDefaultActivateExtension() throws Exception {
        // test default
        ActivateCriteria url = new ActivateCriteria("ext", "order1,default");
        List<ActivateExt1> list = getExtensionFactory(ActivateExt1.class)
                                          .getActivateExtension(url, "ext", "default_group");
        Assertions.assertEquals(2, list.size());
        Assertions.assertSame(list.get(0).getClass(), OrderActivateExtImpl1.class);
        Assertions.assertSame(list.get(1).getClass(), ActivateExt1Impl1.class);

        ActivateCriteria url2 = new ActivateCriteria("ext", "default,order1");
        list = getExtensionFactory(ActivateExt1.class)
                       .getActivateExtension(url2, "ext", "default_group");
        Assertions.assertEquals(2, list.size());
        Assertions.assertSame(list.get(0).getClass(), ActivateExt1Impl1.class);
        Assertions.assertSame(list.get(1).getClass(), OrderActivateExtImpl1.class);
    }

    @Test
    public void testInjectExtension() {
        // test default
        InjectExt injectExt = getExtensionFactory(InjectExt.class).getExtension("injection");
        InjectExtImpl injectExtImpl = (InjectExtImpl) injectExt;
        Assertions.assertNotNull(injectExtImpl.getSimpleExt());
        Assertions.assertNull(injectExtImpl.getSimpleExt1());
        Assertions.assertNull(injectExtImpl.getGenericType());
    }

    @Test
    void testMultiNames() {
        Ext10MultiNames ext10MultiNames = getExtensionFactory(Ext10MultiNames.class).getExtension("impl");
        Assertions.assertNotNull(ext10MultiNames);
        ext10MultiNames = getExtensionFactory(Ext10MultiNames.class).getExtension("implMultiName");
        Assertions.assertNotNull(ext10MultiNames);
        Assertions.assertThrows(
                IllegalStateException.class,
                () -> getExtensionFactory(Ext10MultiNames.class).getExtension("impl,implMultiName")
        );
    }

    @Test
    public void testGetOrDefaultExtension() {
        ExtensionFactory<InjectExt> loader = getExtensionFactory(InjectExt.class);
        InjectExt injectExt = loader.getOrDefaultExtension("non-exists");
        assertEquals(InjectExtImpl.class, injectExt.getClass());
        assertEquals(InjectExtImpl.class, loader.getOrDefaultExtension("injection").getClass());
    }

    @Test
    public void testGetSupported() {
        ExtensionFactory<InjectExt> loader = getExtensionFactory(InjectExt.class);
        assertEquals(1, loader.getSupportedExtensions().size());
        assertEquals(Collections.singleton("injection"), loader.getSupportedExtensions());
    }

    /**
     */
    @Test
    public void testGetLoadingStrategies() {
        List<LoadingStrategy> strategies = getLoadingStrategies();

        assertEquals(4, strategies.size());

        int i = 0;

        LoadingStrategy loadingStrategy = strategies.get(i++);
        assertEquals(InternalLoadingStrategy.class, loadingStrategy.getClass());
        assertEquals(Prioritized.HIGHEST_PRIORITY, loadingStrategy.getPriority());

        loadingStrategy = strategies.get(i++);
        assertEquals(ExternalLoadingStrategy.class, loadingStrategy.getClass());
        assertEquals(Prioritized.HIGHEST_PRIORITY + 1, loadingStrategy.getPriority());


        loadingStrategy = strategies.get(i++);
        assertEquals(DefaultLoadingStrategy.class, loadingStrategy.getClass());
        assertEquals(Prioritized.DEFAULT_PRIORITY, loadingStrategy.getPriority());

        loadingStrategy = strategies.get(i++);
        assertEquals(ServicesLoadingStrategy.class, loadingStrategy.getClass());
        assertEquals(Prioritized.LOWEST_PRIORITY, loadingStrategy.getPriority());
    }

    @Test
    public void testDuplicatedImplWithoutOverriddenStrategy() {
        List<LoadingStrategy> loadingStrategies = getLoadingStrategies();
        ExtensionClassLoader.setLoadingStrategies(new DubboExternalLoadingStrategyTest(false),
                new DubboInternalLoadingStrategyTest(false));
        ExtensionFactory<DuplicatedWithoutOverriddenExt> extensionFactory = ExtensionFactory.getExtensionFactory(DuplicatedWithoutOverriddenExt.class);
        try {
            extensionFactory.getExtension("duplicated");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Failed to load extension class (interface: interface org.neuronbit.xpi.common.extension.duplicated.DuplicatedWithoutOverriddenExt"));
            assertThat(expected.getMessage(), containsString("cause: Duplicate extension org.neuronbit.xpi.common.extension.duplicated.DuplicatedWithoutOverriddenExt name duplicated"));
        }finally {
            //recover the loading strategies
            ExtensionClassLoader.setLoadingStrategies(loadingStrategies.toArray(new LoadingStrategy[loadingStrategies.size()]));
        }
    }

    @Test
    public void testDuplicatedImplWithOverriddenStrategy() {
        List<LoadingStrategy> loadingStrategies = getLoadingStrategies();
        ExtensionClassLoader.setLoadingStrategies(new DubboExternalLoadingStrategyTest(true),
                new DubboInternalLoadingStrategyTest(true));
        ExtensionFactory<DuplicatedOverriddenExt> extensionFactory = ExtensionFactory.getExtensionFactory(DuplicatedOverriddenExt.class);
        DuplicatedOverriddenExt duplicatedOverriddenExt = extensionFactory.getExtension("duplicated");
        assertEquals("DuplicatedOverriddenExt1", duplicatedOverriddenExt.echo());
        //recover the loading strategies
        ExtensionClassLoader.setLoadingStrategies(loadingStrategies.toArray(new LoadingStrategy[loadingStrategies.size()]));
    }

    /**
     * The external {@link LoadingStrategy}, which can set if it support overridden
     */
    private static class DubboExternalLoadingStrategyTest implements LoadingStrategy {

        public DubboExternalLoadingStrategyTest(boolean overridden) {
            this.overridden = overridden;
        }

        private boolean overridden;

        @Override
        public String directory() {
            return "META-INF/xpi/external/";
        }

        @Override
        public boolean overridden() {
            return this.overridden;
        }

        @Override
        public int getPriority() {
            return HIGHEST_PRIORITY + 1;
        }
    }

    /**
     * The internal {@link LoadingStrategy}, which can set if it support overridden
     */
    private static class DubboInternalLoadingStrategyTest implements LoadingStrategy {

        public DubboInternalLoadingStrategyTest(boolean overridden) {
            this.overridden = overridden;
        }

        private boolean overridden;

        @Override
        public String directory() {
            return "META-INF/xpi/internal/";
        }

        @Override
        public boolean overridden() {
            return this.overridden;
        }

        @Override
        public int getPriority() {
            return HIGHEST_PRIORITY;
        }
    }
}
