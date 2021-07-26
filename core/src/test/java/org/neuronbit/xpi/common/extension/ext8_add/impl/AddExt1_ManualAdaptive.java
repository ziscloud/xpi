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
package org.neuronbit.xpi.common.extension.ext8_add.impl;

import org.neuronbit.xpi.common.ActivateCriteria;
import org.neuronbit.xpi.common.extension.Adaptive;
import org.neuronbit.xpi.common.extension.ExtensionFactory;
import org.neuronbit.xpi.common.extension.ext8_add.AddExt1;

@Adaptive
public class AddExt1_ManualAdaptive implements AddExt1 {
    public String echo(ActivateCriteria url, String s) {
        AddExt1 addExt1 = ExtensionFactory.getExtensionFactory(AddExt1.class).getExtension(url.getParameter("add.ext1"));
        return addExt1.echo(url, s);
    }
}