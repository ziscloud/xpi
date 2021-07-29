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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neuronbit.xpi.common.extension.adaptive.HasAdaptiveExt;
import org.neuronbit.xpi.common.extension.adaptive.impl.HasAdaptiveExt_ManualAdaptive;
import org.neuronbit.xpi.common.extension.ext1.SimpleExt;
import org.neuronbit.xpi.common.extension.ext1.SimpleParam;
import org.neuronbit.xpi.common.extension.ext2.Ext2;
import org.neuronbit.xpi.common.extension.ext3.UseProtocolKeyExt;
import org.neuronbit.xpi.common.extension.ext4.NoUrlParamExt;
import org.neuronbit.xpi.common.extension.ext5.NoAdaptiveMethodExt;
import org.neuronbit.xpi.common.extension.ext6_inject.Ext6;
import org.neuronbit.xpi.common.extension.ext6_inject.SimpleParamExt6;
import org.neuronbit.xpi.common.extension.ext6_inject.impl.Ext6Impl2;
import org.neuronbit.xpi.common.utils.LogUtil;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ExtensionFactory_Adaptive_Test {

    @Test
    public void test_useAdaptiveClass() throws Exception {
        ExtensionFactory<HasAdaptiveExt> loader = ExtensionFactory.getExtensionFactory(HasAdaptiveExt.class);
        HasAdaptiveExt ext = loader.getAdaptiveExtension();
        assertTrue(ext instanceof HasAdaptiveExt_ManualAdaptive);
    }

    @Test
    public void test_getAdaptiveExtension_defaultAdaptiveKey() throws Exception {
        {
            SimpleExt ext = ExtensionFactory.getExtensionFactory(SimpleExt.class).getAdaptiveExtension();

            String echo = ext.echo(new SimpleParam(), "haha");
            assertEquals("Ext1Impl1-echo", echo);
        }

        {
            SimpleExt ext = ExtensionFactory.getExtensionFactory(SimpleExt.class).getAdaptiveExtension();

            SimpleParam map = new SimpleParam();
            map.setSimpleExt("impl2");

            String echo = ext.echo(map, "haha");
            assertEquals("Ext1Impl2-echo", echo);
        }
    }

    @Test
    public void test_getAdaptiveExtension_customizeAdaptiveKey() throws Exception {
        SimpleExt ext = ExtensionFactory.getExtensionFactory(SimpleExt.class).getAdaptiveExtension();

        SimpleParam map = new SimpleParam();
        map.setKey2("impl2");
        map.setProtocol("p1");

        String echo = ext.yell(map, "haha");
        assertEquals("Ext1Impl2-yell", echo);

        SimpleParam map2 = new SimpleParam();
        map2.setKey2("impl2");
        map2.setProtocol("p1");
        map2.setKey1("impl3");

        echo = ext.yell(map2, "haha");
        assertEquals("Ext1Impl3-yell", echo);
    }

    @Test
    public void test_getAdaptiveExtension_protocolKey() throws Exception {
        UseProtocolKeyExt ext = ExtensionFactory.getExtensionFactory(UseProtocolKeyExt.class).getAdaptiveExtension();

        {
            String echo = ext.echo(new SimpleParam(), "s");
            assertEquals("Ext3Impl1-echo", echo); // default value

            SimpleParam map = new SimpleParam();
            map.setProtocol("impl3");

            echo = ext.echo(map, "s");
            assertEquals("Ext3Impl3-echo", echo); // use 2nd key, protocol


            SimpleParam map2 = new SimpleParam();
            map2.setProtocol("impl3");
            map2.setKey1("impl2");
            echo = ext.echo(map2, "s");
            assertEquals("Ext3Impl2-echo", echo); // use 1st key, key1
        }

        {

            SimpleParam map = new SimpleParam();
            String yell = ext.yell(map, "s");
            assertEquals("Ext3Impl1-yell", yell); // default value

            SimpleParam url2 = new SimpleParam();
            url2.setKey2("impl2"); // use 2nd key, key2
            yell = ext.yell(url2, "s");
            assertEquals("Ext3Impl2-yell", yell);

            SimpleParam url3 = new SimpleParam();
            url3.setProtocol("impl3"); // use 1st key, protocol
            yell = ext.yell(url3, "d");
            assertEquals("Ext3Impl3-yell", yell);
        }
    }

    @Test
    public void test_getAdaptiveExtension_ExceptionWhenNoAdaptiveMethodOnInterface() throws Exception {
        try {
            ExtensionFactory.getExtensionFactory(NoAdaptiveMethodExt.class).getAdaptiveExtension();
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(),
                    allOf(containsString("Can't create adaptive extension interface org.neuronbit.xpi.common.extension.ext5.NoAdaptiveMethodExt"),
                            containsString("No adaptive method exist on extension org.neuronbit.xpi.common.extension.ext5.NoAdaptiveMethodExt, refuse to create the adaptive class")));
        }
        // report same error when get is invoked for multiple times
        try {
            ExtensionFactory.getExtensionFactory(NoAdaptiveMethodExt.class).getAdaptiveExtension();
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(),
                    allOf(containsString("Can't create adaptive extension interface org.neuronbit.xpi.common.extension.ext5.NoAdaptiveMethodExt"),
                            containsString("No adaptive method exist on extension org.neuronbit.xpi.common.extension.ext5.NoAdaptiveMethodExt, refuse to create the adaptive class")));
        }
    }

    @Test
    public void test_getAdaptiveExtension_ExceptionWhenNotAdaptiveMethod() throws Exception {
        SimpleExt ext = ExtensionFactory.getExtensionFactory(SimpleExt.class).getAdaptiveExtension();

        try {
            ext.bang(new SimpleParam(), 33);
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
            ExtensionFactory.getExtensionFactory(NoUrlParamExt.class).getAdaptiveExtension();
            fail();
        } catch (Exception expected) {
            assertThat(expected.getMessage(), containsString("Failed to create adaptive class for interface "));
            assertThat(expected.getMessage(), containsString(": not found url parameter or url attribute in parameters of method "));
        }
    }

    @Test
    public void test_urlHolder_getAdaptiveExtension() throws Exception {
        Ext2 ext = ExtensionFactory.getExtensionFactory(Ext2.class).getAdaptiveExtension();

        SimpleParam holder = new SimpleParam();
        holder.setExt2(  "impl1");

        String echo = ext.echo(holder, "haha");
        assertEquals("Ext2Impl1-echo", echo);
    }

    @Test
    public void test_urlHolder_getAdaptiveExtension_noExtension() throws Exception {
        Ext2 ext = ExtensionFactory.getExtensionFactory(Ext2.class).getAdaptiveExtension();

        SimpleParam holder = new SimpleParam();

        try {
            ext.echo(holder, "haha");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Failed to get extension"));
        }

        holder.setExt2("XXX");
        try {
            ext.echo(holder, "haha");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("No such extension"));
        }
    }

    @Test
    public void test_urlHolder_getAdaptiveExtension_UrlNpe() throws Exception {
        Ext2 ext = ExtensionFactory.getExtensionFactory(Ext2.class).getAdaptiveExtension();

        try {
            ext.echo(null, "haha");
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("Failed to get extension (org.neuronbit.xpi.common.extension.ext2.Ext2) name"));
        }

        try {
            ext.echo(new SimpleParam(), "haha");
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("Failed to get extension (org.neuronbit.xpi.common.extension.ext2.Ext2) name"));
        }
    }

    @Test
    public void test_urlHolder_getAdaptiveExtension_ExceptionWhenNotAdativeMethod() throws Exception {
        Ext2 ext = ExtensionFactory.getExtensionFactory(Ext2.class).getAdaptiveExtension();

        try {
            ext.bang(new SimpleParam(), 33);
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
        Ext2 ext = ExtensionFactory.getExtensionFactory(Ext2.class).getAdaptiveExtension();

        final SimpleParam simpleParam = new SimpleParam();
        try {
            ext.echo(simpleParam, "impl1");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Failed to get extension"));
        }

        simpleParam.setKey1( "impl1");
        try {
            ext.echo(simpleParam, "haha");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Failed to get extension (org.neuronbit.xpi.common.extension.ext2.Ext2) name from parameters"));
        }
    }

    @Test
    public void test_getAdaptiveExtension_inject() throws Exception {
        LogUtil.start();
        Ext6 ext = ExtensionFactory.getExtensionFactory(Ext6.class).getAdaptiveExtension();

        SimpleParamExt6 url = new SimpleParamExt6();
        url.setExt6("impl1");

        assertEquals("Ext6Impl1-echo-Ext1Impl1-echo", ext.echo(url, "ha"));

        Assertions.assertTrue(LogUtil.checkNoError(), "can not find error.");
        LogUtil.stop();

        SimpleParamExt6 map = new SimpleParamExt6();
        map.setProtocol("p1");
        map.setExt6("impl1");
        map.setSimpleExt("impl2");
        assertEquals("Ext6Impl1-echo-Ext1Impl2-echo", ext.echo(map, "ha"));

    }

    @Test
    public void test_getAdaptiveExtension_InjectNotExtFail() throws Exception {
        Ext6 ext = ExtensionFactory.getExtensionFactory(Ext6.class).getExtension("impl2");

        Ext6Impl2 impl = (Ext6Impl2) ext;
        assertNull(impl.getList());
    }
}
