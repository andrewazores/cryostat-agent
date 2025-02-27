/*
 * Copyright The Cryostat Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.cryostat.agent.util;

import java.io.InputStream;

import io.cryostat.agent.Agent;

public class ResourcesUtil {

    private ResourcesUtil() {}

    public static ClassLoader getClassLoader() {
        return getClassLoader(Agent.class);
    }

    public static ClassLoader getClassLoader(Class<?> klazz) {
        ClassLoader cl = klazz.getClassLoader();
        if (cl == null) {
            cl = Thread.currentThread().getContextClassLoader();
        }
        if (cl == null) {
            cl = ClassLoader.getSystemClassLoader();
        }
        return cl;
    }

    public static InputStream getResourceAsStream(String location) {
        return getClassLoader().getResourceAsStream(location);
    }
}
