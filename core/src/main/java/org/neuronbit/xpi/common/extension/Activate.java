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

import java.lang.annotation.*;

/**
 * Activate. This annotation is useful for automatically activate certain extensions with the given criteria,
 * for examples: <code>@Activate</code> can be used to load certain <code>Filter</code> extension when there are
 * multiple implementations.
 * <ol>
 * <li>{@link Activate#group()} specifies group criteria. Framework SPI defines the valid group values.
 * <li>{@link Activate#value()} specifies parameter key in {@link ActivateCriteria} criteria.
 * </ol>
 * SPI provider can call {@link ExtensionFactory#getActivateExtension(ActivateCriteria, String, String)} to find out all activated
 * extensions with the given criteria.
 *
 * @see SPI
 * @see ActivateCriteria
 * @see ExtensionFactory
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Activate {
    /**
     * Activate the current extension when one of the groups matches. The group passed into
     * {@link ExtensionFactory#getActivateExtension(ActivateCriteria, String, String)} will be used for matching.
     *
     * @return group names to match
     * @see ExtensionFactory#getActivateExtension(ActivateCriteria, String, String)
     */
    String[] group() default {};

    /**
     * Activate the current extension when the specified keys appear in the URL's parameters.
     * <p>
     * For example, given <code>@Activate("cache, validation")</code>, the current extension will be return only when
     * there's either <code>cache</code> or <code>validation</code> key appeared in the URL's parameters.
     * </p>
     *
     * @return URL parameter keys
     * @see ExtensionFactory#getActivateExtension(ActivateCriteria, String)
     * @see ExtensionFactory#getActivateExtension(ActivateCriteria, String, String)
     */
    String[] value() default {};

    /**
     * Relative ordering info, optional
     *
     * @return extension list which should be put before the current one
     */
    String[] before() default {};

    /**
     * Relative ordering info, optional
     *
     * @return extension list which should be put after the current one
     */
    String[] after() default {};

    /**
     * Absolute ordering info, optional
     *
     * Ascending order, smaller values will be in the front o the list.
     *
     * @return absolute ordering info
     */
    int order() default 0;
}
