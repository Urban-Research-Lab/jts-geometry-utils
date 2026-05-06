plugins {
    id("java")
    id("maven-publish")
}

group = "ru.itmo.idu"
version = "4.1.0"

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://repo.osgeo.org/repository/release/")
    }
    maven {
        url = uri("https://repo.boundlessgeo.com/main")
    }

}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
    withJavadocJar()
}

configurations.all {
    // com.vividsolutions.jts is migrated to org.locationtech.jts but some older libs do not know about it
    exclude(group = "com.vividsolutions")
    exclude(module = "jai_core")
}

dependencies {
    compileOnly(group = "org.projectlombok", name = "lombok", version = "1.18.42")
    annotationProcessor(group = "org.projectlombok", name = "lombok", version = "1.18.42")

    implementation(group = "org.slf4j", name = "slf4j-api", version = "2.0.17")
    implementation(group = "org.apache.commons", name = "commons-lang3", version = "3.20.0")
    implementation(group = "org.apache.commons", name = "commons-collections4", version = "4.5.0")

    implementation(group = "org.locationtech.jts", name = "jts-core", version = "1.20.0")
    implementation(group = "org.locationtech.jts.io", name = "jts-io-common", version = "1.20.0")

    implementation(group = "com.google.code.gson", name = "gson", version = "2.13.2")

    implementation(group = "org.geotools", name = "gt-main", version = "34.0")
    implementation(group = "org.geotools", name = "gt-api", version = "34.0")
    implementation(group = "org.geotools", name = "gt-epsg-hsql", version = "34.0")
    implementation(group = "org.geotools", name = "gt-geojson", version = "34.0")

    testRuntimeOnly(group = "org.junit.platform", name = "junit-platform-launcher", version = "1.14.2")
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = "6.0.2")
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Urban-Research-Lab/jts-geometry-utils")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.javadoc {
    options.encoding = "UTF-8"

}