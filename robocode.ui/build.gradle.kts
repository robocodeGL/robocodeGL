plugins {
    id("net.sf.robocode.java-conventions")
    `java-library`
}

val joglVersion = "2.3.2"

dependencies {
    implementation(project(":robocode.api"))
    implementation(project(":robocode.core"))
    implementation(project(":robocode.battle"))
    implementation(project(":robocode.gl2"))
    implementation("org.picocontainer:picocontainer:2.14.2")
    implementation("org.jogamp.gluegen:gluegen-rt-main:${joglVersion}")
    implementation("org.jogamp.jogl:jogl-all-main:${joglVersion}")
    runtimeOnly(project(":robocode.sound"))
}

description = "Robocode UI"

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