import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.google.protobuf.gradle.*

plugins {
  java
  application
  //pmd
  jacoco
  id("com.github.johnrengelman.shadow") version "4.0.3"
  id("com.google.protobuf") version "0.8.8"
//  id("com.github.spotbugs") version "2.0.0"
}

repositories {
  mavenCentral()
  jcenter()
}

sourceSets["main"].java.srcDir("build/generated/source/proto/main/grpc")
sourceSets["main"].java.srcDir( "build/generated/source/proto/main/java")
sourceSets["test"].java.srcDir("build/generated/source/proto/test/grpc")
sourceSets["test"].java.srcDir("build/generated/source/proto/test/java")

val vertxVersion = "3.8.0"
val junitVersion = "5.3.2"
val hamcrestVersion = "1.3"

val logbackVersion = "1.2.3"
val owaspSecurityLoggingVersion = "1.1.6"
val javaUuidGeneratorVersion = "3.2.0"

val protocVersion = "3.9.1"
val grpcVersion = "1.20.0"

dependencies {
  implementation("io.vertx:vertx-core:$vertxVersion")
  implementation("io.vertx:vertx-config:$vertxVersion")
  implementation("io.vertx:vertx-config-yaml:$vertxVersion")
  implementation("io.vertx:vertx-config-kubernetes-configmap:$vertxVersion")
  implementation("io.vertx:vertx-service-discovery:$vertxVersion")

  implementation("io.vertx:vertx-grpc:$vertxVersion")
  implementation("com.google.protobuf:protobuf-java:$protocVersion")
  implementation("io.grpc:grpc-stub:$grpcVersion")
  implementation("io.grpc:grpc-protobuf:$grpcVersion")
  if (JavaVersion.current().isJava9Compatible) {
    // Workaround for @javax.annotation.Generated
    // see: https://github.com/grpc/grpc-java/issues/3633
    implementation("javax.annotation:javax.annotation-api:1.3.1")
  }

  // Logging
  implementation("ch.qos.logback:logback-classic:$logbackVersion")
  implementation("org.owasp:security-logging-logback:$owaspSecurityLoggingVersion")
  implementation("com.fasterxml.uuid:java-uuid-generator:$javaUuidGeneratorVersion")

  testImplementation("io.vertx:vertx-junit5:$vertxVersion")
  testImplementation("io.vertx:vertx-web-client:$vertxVersion")
  testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
  testImplementation("org.hamcrest:hamcrest-library:$hamcrestVersion")
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
}

application {
  mainClassName = "io.vertx.core.Launcher"
}

jacoco {
  toolVersion = "0.8.4"
  reportsDir = file("$buildDir/customJacocoReportDir")
}

pmd {
  isIgnoreFailures = true
  reportsDir = file("$buildDir/reports/pmd")
}

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:$protocVersion"
  }
  plugins {
    id("grpc") {
      artifact = "io.vertx:protoc-gen-grpc-java:$grpcVersion"
    }
  }
  generateProtoTasks {
    all().forEach {
      it.plugins {
        // Apply the "grpc" plugin whose spec is defined above, without options.
        id("grpc")
      }
    }
  }
}

val mainVerticleName = "io.luminara.quickstart.vertx.grpc.MainVerticle"
val watchForChange = "src/**/*.java"
val doOnChange = "${projectDir}/gradlew classes"

tasks {
  test {
    useJUnitPlatform()
  }

  getByName<JavaExec>("run") {
    args = listOf("run", mainVerticleName, "--redeploy=${watchForChange}", "--launcher-class=${application.mainClassName}", "--on-redeploy=${doOnChange}")
  }

  withType<ShadowJar> {
    classifier = "fat"
    manifest {
      attributes["Main-Verticle"] = mainVerticleName
    }
    mergeServiceFiles {
      include("META-INF/services/io.vertx.core.spi.VerticleFactory")
    }
  }
}
