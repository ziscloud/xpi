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
import org.neuronbit.xpi.common.extension.adaptive.HasAdaptiveExt;
import org.neuronbit.xpi.common.extension.adaptive.impl.HasAdaptiveExt_ManualAdaptive;
import org.neuronbit.xpi.common.extension.ext1.SimpleExt;
import org.neuronbit.xpi.common.extension.ext2.Ext2;
import org.neuronbit.xpi.common.extension.ext2.UrlHolder;
import org.neuronbit.xpi.common.extension.ext3.UseProtocolKeyExt;
import org.neuronbit.xpi.common.extension.ext4.NoUrlParamExt;
import org.neuronbit.xpi.common.extension.ext5.NoAdaptiveMethodExt;
import org.neuronbit.xpi.common.extension.ext6_inject.Ext6;
import org.neuronbit.xpi.common.extension.ext6_inject.impl.Ext6Impl2;
import org.neuronbit.xpi.common.utils.LogUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ExtensionLoader_Adaptive_Test {

    @Test
    public void test_useAdaptiveClass() throws Exception {
        ExtensionLoader<HasAdaptiveExt> loader = ExtensionLoader.getExtensionLoader(HasAdaptiveExt.class);
        HasAdaptiveExt ext = loader.getAdaptiveExtension();
        assertTrue(ext instanceof HasAdaptiveExt_ManualAdaptive);
    }

    @Test
    public void test_getAdaptiveExtension_defaultAdaptiveKey() throws Exception {
        {
            SimpleExt ext = ExtensionLoader.getExtensionLoader(SimpleExt.class).getAdaptiveExtension();

            Map<String, String> map = new HashMap<>();
            ActivateCriteria url = new ActivateCriteria(map);

            String echo = ext.echo(url, "haha");
            assertEquals("Ext1Impl1-echo", echo);
        }

        {
            SimpleExt ext = ExtensionLoader.getExtensionLoader(SimpleExt.class).getAdaptiveExtension();

            Map<String, String> map = new HashMap<String, String>();
            map.put("simple.ext", "impl2");
            ActivateCriteria url = new ActivateCriteria(map);

            String echo = ext.echo(url, "haha");
            assertEquals("Ext1Impl2-echo", echo);
        }
    }

    @Test
    public void test_getAdaptiveExtension_customizeAdaptiveKey() throws Exception {
        SimpleExt ext = ExtensionLoader.getExtensionLoader(SimpleExt.class).getAdaptiveExtension();

        Map<String, String> map = new HashMap<>();
        map.put("key2", "impl2");
        map.put("protocol", "p1");
        ActivateCriteria url = new ActivateCriteria(map);

        String echo = ext.yell(url, "haha");
        assertEquals("Ext1Impl2-yell", echo);

        Map<String, String> map2 = new HashMap<>();
        map2.put("key2", "impl2");
        map2.put("protocol", "p1");
        map2.put("key1", "impl3");
        ActivateCriteria url2 = new ActivateCriteria(map2); // note: URL is value's type
        echo = ext.yell(url2, "haha");
        assertEquals("Ext1Impl3-yell", echo);
    }

    @Test
    public void test_getAdaptiveExtension_protocolKey() throws Exception {
        UseProtocolKeyExt ext = ExtensionLoader.getExtensionLoader(UseProtocolKeyExt.class).getAdaptiveExtension();

        {
            String echo = ext.echo(new ActivateCriteria(), "s");
            assertEquals("Ext3Impl1-echo", echo); // default value

            Map<String, String> map = new HashMap<String, String>();
            map.put("protocol", "impl3");
            ActivateCriteria url = new ActivateCriteria(map);

            echo = ext.echo(url, "s");
            assertEquals("Ext3Impl3-echo", echo); // use 2nd key, protocol


            Map<String, String> map2 = new HashMap<String, String>();
            map2.put("protocol", "impl3");
            map2.put("key1", "impl2");
            ActivateCriteria url2 = new ActivateCriteria(map2);
            echo = ext.echo(url2, "s");
            assertEquals("Ext3Impl2-echo", echo); // use 1st key, key1
        }

        {

            Map<String, String> map = new HashMap<String, String>();
            ActivateCriteria url = new ActivateCriteria(map);
            String yell = ext.yell(url, "s");
            assertEquals("Ext3Impl1-yell", yell); // default value

            ActivateCriteria url2 = new ActivateCriteria("key2", "impl2"); // use 2nd key, key2
            yell = ext.yell(url2, "s");
            assertEquals("Ext3Impl2-yell", yell);

            ActivateCriteria url3 = new ActivateCriteria("protocol", "impl3"); // use 1st key, protocol
            yell = ext.yell(url3, "d");
            assertEquals("Ext3Impl3-yell", yell);
        }
    }

    @Test
    public void test_getAdaptiveExtension_UrlNpe() throws Exception {
        SimpleExt ext = ExtensionLoader.getExtensionLoader(SimpleExt.class).getAdaptiveExtension();

        try {
            ext.echo(null, "haha");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("url == null", e.getMessage());
        }
    }

    @Test
    public void test_getAdaptiveExtension_ExceptionWhenNoAdaptiveMethodOnInterface() throws Exception {
        try {
            ExtensionLoader.getExtensionLoader(NoAdaptiveMethodExt.class).getAdaptiveExtension();
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(),
                    allOf(containsString("Can't create adaptive extension interface org.neuronbit.xpi.common.extension.ext5.NoAdaptiveMethodExt"),
                            containsString("No adaptive method exist on extension org.neuronbit.xpi.common.extension.ext5.NoAdaptiveMethodExt, refuse to create the adaptive class")));
        }
        // report same error when get is invoked for multiple times
        try {
            ExtensionLoader.getExtensionLoader(NoAdaptiveMethodExt.class).getAdaptiveExtension();
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(),
                    allOf(containsString("Can't create adaptive extension interface org.neuronbit.xpi.common.extension.ext5.NoAdaptiveMethodExt"),
                            containsString("No adaptive method exist on extension org.neuronbit.xpi.common.extension.ext5.NoAdaptiveMethodExt, refuse to create the adaptive class")));
        }
    }

    @Test
    public void test_getAdaptiveExtension_ExceptionWhenNotAdaptiveMethod() throws Exception {
        SimpleExt ext = ExtensionLoader.getExtensionLoader(SimpleExt.class).getAdaptiveExtension();

        Map<String, String> map = new HashMap<String, String>();
        ActivateCriteria url = new ActivateCriteria(map);

        try {
            ext.bang(url, 33);
            fail();
        } catch (UnsupportedOperationException expected) {
            assertThat(expected.getMessage(), containsString("method "));
            assertThat(
                    expected.getMessage(),
                    containsString("of interface org.neuronbit.xpi.common.extension.ext1.SimpleExt is not adaptive method!"));
        }
    }

    @Test
    public void test_getAdaptiveExtension_ExceptionWhenNoUrlAttribute() throws Exception {
        try {
            ExtensionLoader.getExtensionLoader(NoUrlParamExt.class).getAdaptiveExtension();
            fail();
        } catch (Exception expected) {
            assertThat(expected.getMessage(), containsString("Failed to create adaptive class for interface "));
            assertThat(expected.getMessage(), containsString(": not found url parameter or url attribute in parameters of method "));
        }
    }

    @Test
    public void test_urlHolder_getAdaptiveExtension() throws Exception {
        Ext2 ext = ExtensionLoader.getExtensionLoader(Ext2.class).getAdaptiveExtension();

        Map<String, String> map = new HashMap<String, String>();
        map.put("ext2", "impl1");
        ActivateCriteria url = new ActivateCriteria(map);

        UrlHolder holder = new UrlHolder();
        holder.setUrl(url);

        String echo = ext.echo(holder, "haha");
        assertEquals("Ext2Impl1-echo", echo);
    }

    @Test
    public void test_urlHolder_getAdaptiveExtension_noExtension() throws Exception {
        Ext2 ext = ExtensionLoader.getExtensionLoader(Ext2.class).getAdaptiveExtension();

        ActivateCriteria url = new ActivateCriteria();

        UrlHolder holder = new UrlHolder();
        holder.setUrl(url);

        try {
            ext.echo(holder, "haha");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Failed to get extension"));
        }

        holder.setUrl(new ActivateCriteria("ext2", "XXX"));
        try {
            ext.echo(holder, "haha");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("No such extension"));
        }
    }

    @Test
    public void test_urlHolder_getAdaptiveExtension_UrlNpe() throws Exception {
        Ext2 ext = ExtensionLoader.getExtensionLoader(Ext2.class).getAdaptiveExtension();

        try {
            ext.echo(null, "haha");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("org.neuronbit.xpi.common.extension.ext2.UrlHolder argument == null", e.getMessage());
        }

        try {
            ext.echo(new UrlHolder(), "haha");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("org.neuronbit.xpi.common.extension.ext2.UrlHolder argument getUrl() == null", e.getMessage());
        }
    }

    @Test
    public void test_urlHolder_getAdaptiveExtension_ExceptionWhenNotAdativeMethod() throws Exception {
        Ext2 ext = ExtensionLoader.getExtensionLoader(Ext2.class).getAdaptiveExtension();

        Map<String, String> map = new HashMap<String, String>();
        ActivateCriteria url = new ActivateCriteria(map);

        try {
            ext.bang(url, 33);
            fail();
        } catch (UnsupportedOperationException expected) {
            assertThat(expected.getMessage(), containsString("method "));
            assertThat(
                    expected.getMessage(),
                    containsString("of interface org.neuronbit.xpi.common.extension.ext2.Ext2 is not adaptive method!"));
        }
    }

    @Test
    public void test_urlHolder_getAdaptiveExtension_ExceptionWhenNameNotProvided() throws Exception {
        Ext2 ext = ExtensionLoader.getExtensionLoader(Ext2.class).getAdaptiveExtension();

        ActivateCriteria url = new ActivateCriteria();

        UrlHolder holder = new UrlHolder();
        holder.setUrl(url);

        try {
            ext.echo(holder, "impl1");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Failed to get extension"));
        }

        holder.setUrl(new ActivateCriteria("key1", "impl1"));
        try {
            ext.echo(holder, "haha");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Failed to get extension (org.neuronbit.xpi.common.extension.ext2.Ext2) name from url"));
        }
    }

    @Test
    public void test_getAdaptiveExtension_inject() throws Exception {
        LogUtil.start();
        Ext6 ext = ExtensionLoader.getExtensionLoader(Ext6.class).getAdaptiveExtension();

        ActivateCriteria url = new ActivateCriteria("ext6", "impl1");

        assertEquals("Ext6Impl1-echo-Ext1Impl1-echo", ext.echo(url, "ha"));

        Assertions.assertTrue(LogUtil.checkNoError(), "can not find error.");
        LogUtil.stop();

        final HashMap<String, String> map = new HashMap<>();
        map.put("protocol", "p1");
        map.put("ext6", "impl1");
        map.put("simple.ext", "impl2");
        ActivateCriteria url2 = new ActivateCriteria(map);
        assertEquals("Ext6Impl1-echo-Ext1Impl2-echo", ext.echo(url2, "ha"));

    }

    @Test
    public void test_getAdaptiveExtension_InjectNotExtFail() throws Exception {
        Ext6 ext = ExtensionLoader.getExtensionLoader(Ext6.class).getExtension("impl2");

        Ext6Impl2 impl = (Ext6Impl2) ext;
        assertNull(impl.getList());
    }
}
