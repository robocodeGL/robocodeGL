plugins {
    id("net.sf.robocode.java-conventions")
    `java-library`
}

val joglVersion = "2.3.2"

dependencies {
    implementation(project(":robocode.api"))
    implementation(project(":robocode.core"))
    implementation(project(":robocode.battle"))
    implementation(project(":robocode.sound"))
    implementation("org.jogamp.gluegen:gluegen-rt-main:${joglVersion}")
    implementation("org.jogamp.jogl:jogl-all-main:${joglVersion}")
    implementation("org.picocontainer:picocontainer:2.14.2")
    implementation("org.apache.commons:commons-imaging:1.0-alpha1")
    testImplementation("junit:junit:4.13.1")
}

description = "Robocode GL2"

java {
    withJavadocJar()
    withSourcesJar()
}

tasks {
    register("copyVersion", Copy::class) {
        from("../") {
            include("versions.md")
        }
        into("build/resources/main/")
    }
    processResources{
        dependsOn("copyVersion")

    }
    javadoc {
        source = sourceSets["main"].java
        include("net/sf/robocode/core/Module.java")
    }
}