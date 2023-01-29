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
    id("com.dorkbox.GradleUtils") version "3.11"
    id("com.dorkbox.Licensing") version "2.20"
    id("com.dorkbox.VersionUpdate") version "2.6"
    id("com.dorkbox.GradlePublish") version "1.17"

    kotlin("jvm") version "1.8.0"
}


object Extras {
    // set for the project
    const val description = "High performance and lightweight Animation/Tween framework for Java 8+"
    const val group = "com.dorkbox"
    const val version = "8.3.1"

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
GradleUtils.defaults()
GradleUtils.compileConfiguration(JavaVersion.VERSION_1_8)
GradleUtils.jpms(JavaVersion.VERSION_1_9)

licensing {
    license(License.APACHE_2) {
        author(Extras.vendor)
        author("Aurelien Ribon")
        url(Extras.url)
        note(Extras.description)

        extra("Easing Functions", License.CC0) {
            copyright(2017)
            author("Michael Pohoreski")
            url("https://github.com/Michaelangel007/easing/blob/master/js/core/easing.js")
        }
    }
}


sourceSets {
    test {
        resources {
            setSrcDirs(listOf("test"))
            include("**/*.png", "**/*.jpg")
        }
    }
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
    }
}

dependencies {
    api("com.dorkbox:ObjectPool:4.2")
    api("com.dorkbox:Updates:1.1")

    testImplementation("com.dorkbox:Utilities:1.39")
    testImplementation("com.dorkbox:SwingActiveRender:1.2")
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
