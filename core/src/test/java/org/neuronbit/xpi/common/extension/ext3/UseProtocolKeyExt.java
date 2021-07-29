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
package org.neuronbit.xpi.common.extension.ext3;

import org.neuronbit.xpi.common.extension.Adaptive;
import org.neuronbit.xpi.common.extension.SPI;
import org.neuronbit.xpi.common.extension.ext1.SimpleParam;

@SPI("impl1")
public interface UseProtocolKeyExt {
    // protocol key is the second
    @Adaptive({"key1", "protocol"})
    String echo(SimpleParam url, String s);

    // protocol key is the first
    @Adaptive({"protocol", "key2"})
    String yell(SimpleParam url, String s);
}
