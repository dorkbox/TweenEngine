/*
 * Copyright 2021 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.time.Instant

///////////////////////////////
//////    PUBLISH TO SONATYPE / MAVEN CENTRAL
////// TESTING : (to local maven repo) <'publish and release' - 'publishToMavenLocal'>
////// RELEASE : (to sonatype/maven central), <'publish and release' - 'publishToSonatypeAndRelease'>
///////////////////////////////

gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS_FULL   // always show the stacktrace!
gradle.startParameter.warningMode = WarningMode.All

plugins {
    id("com.dorkbox.GradleUtils") version "1.16"
    id("com.dorkbox.Licensing") version "2.5.5"
    id("com.dorkbox.VersionUpdate") version "2.1"
    id("com.dorkbox.GradlePublish") version "1.10"
//    id("com.dorkbox.GradleModuleInfo") version "1.0"

//    id("com.dorkbox.CrossCompile") version "1.1"

    kotlin("jvm") version "1.4.31"
}


object Extras {
    // set for the project
    const val description = "High performance and lightweight Animation/Tween framework for Java 8+"
    const val group = "com.dorkbox"
    const val version = "8.3"

    // set as project.ext
    const val name = "TweenEngine"
    const val id = "TweenEngine"
    const val vendor = "Dorkbox LLC"
    const val vendorUrl = "https://dorkbox.com"
    const val url = "https://git.dorkbox.com/dorkbox/TweenEngine"
    val buildDate = Instant.now().toString()
}

///////////////////////////////
/////  assign 'Extras'
///////////////////////////////
GradleUtils.load("$projectDir/../../gradle.properties", Extras)
GradleUtils.fixIntellijPaths()
GradleUtils.defaultResolutionStrategy()
// NOTE: Only support java 8 as the lowest target now. We use Multi-Release Jars to provide additional functionality as needed
GradleUtils.compileConfiguration(JavaVersion.VERSION_11)

licensing {
    license(License.APACHE_2) {
        author(Extras.vendor)
        author("Aurelien Ribon")
        url(Extras.url)
        note(Extras.description)
    }
}


sourceSets {
    main {
        java {
            setSrcDirs(listOf("src"))

            // want to include java files for the source. 'setSrcDirs' resets includes...
            include("**/*.java")
        }
    }

    test {
        java {
            setSrcDirs(listOf("test"))

            // only want to include java files for the source. 'setSrcDirs' resets includes...
            include("**/*.java")

            srcDir(sourceSets["main"].allJava)
        }

        resources {
            setSrcDirs(listOf("test"))
            include("**/*.png", "**/*.jpg")

            srcDir(sourceSets["main"].resources)
        }

        compileClasspath += sourceSets.main.get().runtimeClasspath
    }
}

repositories {
    mavenLocal() // this must be first!
    jcenter()
}


///////////////////////////////
//////    Task defaults
///////////////////////////////

tasks.jar.get().apply {
    manifest {
        // https://docs.oracle.com/javase/tutorial/deployment/jar/packageman.html
        attributes["Name"] = Extras.name

        attributes["Specification-Title"] = Extras.name
        attributes["Specification-Version"] = Extras.version
        attributes["Specification-Vendor"] = Extras.vendor

        attributes["Implementation-Title"] = "${Extras.group}.${Extras.id}"
        attributes["Implementation-Version"] = Extras.buildDate
        attributes["Implementation-Vendor"] = Extras.vendor

        attributes["Automatic-Module-Name"] = Extras.id
    }
}

dependencies {
    implementation("com.dorkbox:ObjectPool:3.2")

    testImplementation("com.dorkbox:Utilities:1.9")
    testImplementation("com.dorkbox:SwingActiveRender:1.1")
}


publishToSonatype {
    groupId = Extras.group
    artifactId = Extras.id
    version = Extras.version

    name = Extras.name
    description = Extras.description
    url = Extras.url

    vendor = Extras.vendor
    vendorUrl = Extras.vendorUrl

    issueManagement {
        url = "${Extras.url}/issues"
        nickname = "Gitea Issues"
    }

    developer {
        id = "dorkbox"
        name = Extras.vendor
        email = "email@dorkbox.com"
    }
}
