/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal;

import org.gradle.StartParameter;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class FeaturePreviews {
    public enum Feature {
        IMPROVED_POM_SUPPORT(true),
        GRADLE_METADATA(true);

        public static Feature withName(String name) {
            try {
                return valueOf(name);
            } catch (IllegalArgumentException e) {
                // Re-wording to exception message to get rid of the fqcn it contains
                throw new IllegalArgumentException("There is no feature named " + name);
            }
        }

        private final boolean active;

        Feature(boolean active) {
            this.active = active;
        }

        public boolean isActive() {
            return active;
        }
    }

    private final Set<Feature> activeFeatures;
    private final EnumSet<Feature> enabledFeatures = EnumSet.noneOf(Feature.class);

    public FeaturePreviews(StartParameter startParameter) {
        EnumSet<Feature> tmpActiveSet = EnumSet.noneOf(Feature.class);
        for (Feature feature : Feature.values()) {
            if (feature.isActive()) {
                tmpActiveSet.add(feature);
            }
        }
        activeFeatures = Collections.unmodifiableSet(tmpActiveSet);

        if (startParameter.isAdvancedPomSupport()) {
            enabledFeatures.add(Feature.IMPROVED_POM_SUPPORT);
        }
        if (startParameter.isGradleMetadata()) {
            enabledFeatures.add(Feature.GRADLE_METADATA);
        }
    }

    public void enableFeature(Feature feature) {
        if (feature.isActive()) {
            enabledFeatures.add(feature);
        }
    }

    public boolean isFeatureEnabled(Feature feature) {
        return feature.isActive() && enabledFeatures.contains(feature);
    }

    public void enableFeature(String name) {
        enableFeature(Feature.withName(name));
    }

    public boolean isFeatureEnabled(String name) {
        return isFeatureEnabled(Feature.withName(name));
    }

    /**
     * Returns an {@code EnumSet} containing all {@link Feature} that are active.
     *
     * @return the active {@code Feature}
     */
    public Set<Feature> getActiveFeatures() {
        return activeFeatures;
    }
}
