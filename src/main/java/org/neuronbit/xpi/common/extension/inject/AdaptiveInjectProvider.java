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
package org.neuronbit.xpi.common.extension.inject;

import org.neuronbit.xpi.common.extension.Adaptive;
import org.neuronbit.xpi.common.extension.ExtensionFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AdaptiveExtensionFactory
 */
@Adaptive
public class AdaptiveInjectProvider implements InjectProvider {

    private final List<InjectProvider> factories;

    public AdaptiveInjectProvider() {
        ExtensionFactory<InjectProvider> loader = ExtensionFactory.getExtensionFactory(InjectProvider.class);
        List<InjectProvider> list = new ArrayList<>();
        for (String name : loader.getSupportedExtensions()) {
            list.add(loader.getExtension(name));
        }
        factories = Collections.unmodifiableList(list);
    }

    @Override
    public <T> T getInstance(Class<T> type, String name) {
        for (InjectProvider factory : factories) {
            T extension = factory.getInstance(type, name);
            if (extension != null) {
                return extension;
            }
        }
        return null;
    }

}
