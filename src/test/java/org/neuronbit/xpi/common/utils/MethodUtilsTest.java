/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neuronbit.xpi.common.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.neuronbit.xpi.common.utils.MethodUtils.findMethod;
import static org.neuronbit.xpi.common.utils.MethodUtils.getMethods;

public class MethodUtilsTest {

    @Test
    public void testGetMethod() {
        Method getMethod = null;
        for (Method method : MethodTestClazz.class.getMethods()) {
            if (MethodUtils.isGetter(method)) {
                getMethod = method;
            }
        }
        Assertions.assertNotNull(getMethod);
        Assertions.assertEquals("getValue", getMethod.getName());
    }

    @Test
    public void testSetMethod() {
        Method setMethod = null;
        for (Method method : MethodTestClazz.class.getMethods()) {
            if (MethodUtils.isSetter(method)) {
                setMethod = method;
            }
        }
        Assertions.assertNotNull(setMethod);
        Assertions.assertEquals("setValue", setMethod.getName());
    }

    @Test
    public void testIsDeprecated() throws Exception {
        Assertions.assertTrue(MethodUtils.isDeprecated(MethodTestClazz.class.getMethod("deprecatedMethod")));
        Assertions.assertFalse(MethodUtils.isDeprecated(MethodTestClazz.class.getMethod("getValue")));
    }

    @Test
    public void testIsMetaMethod() {
        boolean containMetaMethod = false;
        for (Method method : MethodTestClazz.class.getMethods()) {
            if (MethodUtils.isMetaMethod(method)) {
                containMetaMethod = true;
            }
        }
        Assertions.assertTrue(containMetaMethod);
    }

    @Test
    public void testGetMethods() throws NoSuchMethodException {
        Assertions.assertTrue(MethodUtils.getDeclaredMethods(MethodTestClazz.class, MethodUtils.excludedDeclaredClass(String.class)).size() > 0);
        Assertions.assertTrue(MethodUtils.getMethods(MethodTestClazz.class).size() > 0);
        Assertions.assertTrue(MethodUtils.getAllDeclaredMethods(MethodTestClazz.class).size() > 0);
        Assertions.assertTrue(MethodUtils.getAllMethods(MethodTestClazz.class).size() > 0);
        Assertions.assertNotNull(MethodUtils.findMethod(MethodTestClazz.class, "getValue"));

        MethodTestClazz methodTestClazz = new MethodTestClazz();
        MethodUtils.invokeMethod(methodTestClazz, "setValue", "Test");
        Assertions.assertEquals(methodTestClazz.getValue(), "Test");

        Assertions.assertTrue(MethodUtils.overrides(MethodOverrideClazz.class.getMethod("get"),
                MethodTestClazz.class.getMethod("get")));
        Assertions.assertEquals(MethodUtils.findNearestOverriddenMethod(MethodOverrideClazz.class.getMethod("get")),
                MethodTestClazz.class.getMethod("get"));
        Assertions.assertEquals(MethodUtils.findOverriddenMethod(MethodOverrideClazz.class.getMethod("get"), MethodOverrideClazz.class),
                MethodTestClazz.class.getMethod("get"));

    }

    public class MethodTestClazz {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public MethodTestClazz get() {
            return this;
        }

        @Deprecated
        public Boolean deprecatedMethod() {
            return true;
        }
    }

    public class MethodOverrideClazz extends MethodTestClazz {
        @Override
        public MethodTestClazz get() {
            return this;
        }
    }

}
