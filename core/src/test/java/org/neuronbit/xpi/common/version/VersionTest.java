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
package org.neuronbit.xpi.common.version;


import org.neuronbit.xpi.common.Version;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VersionTest {

    @Test
    public void testGetIntVersion() {
        Assertions.assertEquals(2060100, Version.getIntVersion("2.6.1"));
        Assertions.assertEquals(2060101, Version.getIntVersion("2.6.1.1"));
        Assertions.assertEquals(2070001, Version.getIntVersion("2.7.0.1"));
        Assertions.assertEquals(2070000, Version.getIntVersion("2.7.0"));
        Assertions.assertEquals(2070000, Version.getIntVersion("2.7.0.RC1"));
        Assertions.assertEquals(2070000, Version.getIntVersion("2.7.0-SNAPSHOT"));
        Assertions.assertEquals(3000000, Version.getIntVersion("3.0.0-SNAPSHOT"));
        Assertions.assertEquals(3010000, Version.getIntVersion("3.1.0"));
    }

    @Test
    public void testCompare() {
        Assertions.assertEquals(0, Version.compare("3.0.0", "3.0.0"));
        Assertions.assertEquals(0, Version.compare("3.0.0-SNAPSHOT", "3.0.0"));
        Assertions.assertEquals(1, Version.compare("3.0.0.1", "3.0.0"));
        Assertions.assertEquals(1, Version.compare("3.1.0", "3.0.0"));
        Assertions.assertEquals(1, Version.compare("3.1.2.3", "3.0.0"));
        Assertions.assertEquals(-1, Version.compare("2.9.9.9", "3.0.0"));
        Assertions.assertEquals(-1, Version.compare("2.6.3.1", "3.0.0"));
    }
}
