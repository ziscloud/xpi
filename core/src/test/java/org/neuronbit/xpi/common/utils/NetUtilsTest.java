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
package org.neuronbit.xpi.common.utils;

import org.junit.jupiter.api.Test;

import java.net.Inet6Address;
import java.net.InetAddress;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NetUtilsTest {

    @Test
    public void testIsValidAddress() throws Exception {
        assertFalse(NetUtils.isValidV4Address((InetAddress) null));
        InetAddress address = mock(InetAddress.class);
        when(address.isLoopbackAddress()).thenReturn(true);
        assertFalse(NetUtils.isValidV4Address(address));
        address = mock(InetAddress.class);
        when(address.getHostAddress()).thenReturn("localhost");
        assertFalse(NetUtils.isValidV4Address(address));
        address = mock(InetAddress.class);
        when(address.getHostAddress()).thenReturn("0.0.0.0");
        assertFalse(NetUtils.isValidV4Address(address));
        address = mock(InetAddress.class);
        when(address.getHostAddress()).thenReturn("127.0.0.1");
        assertFalse(NetUtils.isValidV4Address(address));
        address = mock(InetAddress.class);
        when(address.getHostAddress()).thenReturn("1.2.3.4");
        assertTrue(NetUtils.isValidV4Address(address));
    }

    @Test
    public void testGetLocalHost() throws Exception {
        assertNotNull(NetUtils.getLocalHost());
    }

    @Test
    public void testIsValidV6Address() {
        String saved = System.getProperty("java.net.preferIPv6Addresses", "false");
        System.setProperty("java.net.preferIPv6Addresses", "true");

        InetAddress address = NetUtils.getLocalAddress();
        boolean isPreferIPV6Address = NetUtils.isPreferIPV6Address();

        // Restore system property to previous value before executing test
        System.setProperty("java.net.preferIPv6Addresses", saved);

        assumeTrue(address instanceof Inet6Address);
        assertThat(isPreferIPV6Address, equalTo(true));
    }

    @Test
    public void testNormalizeV6Address() {
        Inet6Address address = mock(Inet6Address.class);
        when(address.getHostAddress()).thenReturn("fe80:0:0:0:894:aeec:f37d:23e1%en0");
        when(address.getScopeId()).thenReturn(5);
        InetAddress normalized = NetUtils.normalizeV6Address(address);
        assertThat(normalized.getHostAddress(), equalTo("fe80:0:0:0:894:aeec:f37d:23e1%5"));
    }
}
