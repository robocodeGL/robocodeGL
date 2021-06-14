plugins {
    id("net.sf.robocode.java-conventions")
    `java-library`
}

dependencies {
    implementation(project(":robocode.api"))
    implementation(project(":robocode.core"))
    implementation(project(":robocode.battle"))
    implementation(project(":robocode.sound"))
    implementation(project(":robocode.ui"))
    implementation(project(":robocode.ui.editor"))
    implementation(project(":robocode.samples"))
    implementation("org.picocontainer:picocontainer:2.14.2")
}

description = "Robocode Run"

java {
    withJavadocJar()
    withSourcesJar()
}

tasks {
    javadoc {
        source = sourceSets["main"].java
        include("net/sf/robocode/ui/Module.java")
    }
    jar {
        dependsOn("javadoc")
    }
}
