/*
 * Copyright 2020 the original author or authors.
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

package org.gradle.instantexecution

import org.gradle.test.fixtures.server.http.HttpServer
import org.gradle.test.fixtures.server.http.MavenHttpRepository
import org.junit.Rule

class InstantExecutionUnsupportedDependencyResolutionFeaturesIntegrationTest extends AbstractInstantExecutionIntegrationTest {
    @Rule
    HttpServer server = new HttpServer()
    def remoteRepo = new MavenHttpRepository(server, mavenRepo)

    @Override
    def setup() {
        // So that dependency resolution results from previous tests do not interfere
        executer.requireOwnGradleUserHomeDir()
    }

    def "reports resolution of remote dynamic dependencies as unsupported"() {
        given:
        server.start()

        remoteRepo.module("thing", "lib", "1.2").publish()
        def v3 = remoteRepo.module("thing", "lib", "1.3").publish()

        buildFile << """
            configurations { implementation }

            repositories { maven { url = '${remoteRepo.uri}' } }

            dependencies {
                implementation 'thing:lib:1.+'
            }

            task resolve1 {
                inputs.files configurations.implementation
                doFirst { }
            }
            task resolve2 {
                inputs.files configurations.implementation
                doFirst { }
            }
        """
        def fixture = newInstantExecutionFixture()

        remoteRepo.getModuleMetaData("thing", "lib").expectGet()
        v3.pom.expectGet()
        v3.artifact.expectGet()

        when:
        instantFails("resolve1")

        then:
        fixture.assertStateStored() // Cache is well formed even though there problems
        problems.assertFailureHasProblems(failure) {
            withProblem("Gradle runtime: support for dynamic dependency versions (thing:lib:1.+) is not yet implemented with the configuration cache.")
            problemsWithStackTraceCount = 0
        }

        when:
        instantRun("resolve1")

        then:
        fixture.assertStateLoaded()
        // TODO - should notify user that the feature is not supported

        when: // run again with different tasks, to verify behaviour when version list is cached
        instantFails("resolve2")

        then:
        fixture.assertStateStored() // Cache is well formed even though there problems
        problems.assertFailureHasProblems(failure) {
            withProblem("Gradle runtime: support for dynamic dependency versions (thing:lib:1.+) is not yet implemented with the configuration cache.")
            problemsWithStackTraceCount = 0
        }

        when:
        instantRun("resolve2")

        then:
        fixture.assertStateLoaded()
        // TODO - should notify user that the feature is not supported
    }

    def "reports resolution of local dynamic dependencies as unsupported"() {
        given:
        mavenRepo.module("thing", "lib", "1.2").publish()
        def v3 = mavenRepo.module("thing", "lib", "1.3").publish()

        buildFile << """
            configurations { implementation }

            repositories { maven { url = '${mavenRepo.uri}' } }

            dependencies {
                implementation 'thing:lib:1.+'
            }

            task resolve1 {
                inputs.files configurations.implementation
                doFirst { }
            }
            task resolve2 {
                inputs.files configurations.implementation
                doFirst { }
            }
        """
        def fixture = newInstantExecutionFixture()

        when:
        instantFails("resolve1")

        then:
        fixture.assertStateStored() // Cache is well formed even though there problems
        problems.assertFailureHasProblems(failure) {
            withProblem("Gradle runtime: support for dynamic dependency versions (thing:lib:1.+) is not yet implemented with the configuration cache.")
            problemsWithStackTraceCount = 0
        }

        when:
        instantRun("resolve1")

        then:
        fixture.assertStateLoaded()
        // TODO - should notify user that the feature is not supported
    }
}
