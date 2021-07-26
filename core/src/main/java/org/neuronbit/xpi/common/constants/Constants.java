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

package org.neuronbit.xpi.common.constants;

import java.util.regex.Pattern;

public interface Constants {
    char COMMA_SEPARATOR_CHAR = ',';

    String COMMA_SEPARATOR = ",";

    Pattern COMMA_SPLIT_PATTERN = Pattern.compile("\\s*[,]+\\s*");

    String REMOVE_VALUE_PREFIX = "-";

    String UNDERLINE_SEPARATOR = "_";

    String SEPARATOR_REGEX = "_|-";

    String HIDE_KEY_PREFIX = ".";

    String DOT_REGEX = "\\.";

    String DEFAULT_KEY = "default";

    String ANYHOST_VALUE = "0.0.0.0";

    String LOCALHOST_VALUE = "127.0.0.1";

    String GROUP_KEY = "group";

    String INTERFACE_KEY = "interface";

    String VERSION_KEY = "version";

    /**
     * Pseudo URL prefix for loading from the class path: "classpath:".
     */
    String CLASSPATH_URL_PREFIX = "classpath:";
}
