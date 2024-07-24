rootProject.name = "pricing-engine"
include("pricing-engine-server")
include("pricing-engine-deploy")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("spring.boot.starter.web", "org.springframework.boot:spring-boot-starter-web:3.0.1")
            library("spring.boot.starter.parent", "org.springframework.boot:spring-boot-starter-parent:3.0.1")
            library("spring.kafka", "org.springframework.kafka:spring-kafka:3.0.1")
            library("commons.math", "org.apache.commons:commons-math3:3.2")
            library("common.api", "com.herron.exchange:common-api:1.0.0-SNAPSHOT")
            library("common", "com.herron.exchange:common:1.0.0-SNAPSHOT")
            library("integration.api", "com.herron.exchange:integration-api:1.0.0-SNAPSHOT")
            library("integrations", "com.herron.exchange:integrations:1.0.0-SNAPSHOT")
            library("quantlib.api", "com.herron.exchange:quantlib-api:1.0.0-SNAPSHOT")
            library("quantlib", "com.herron.exchange:quantlib:1.0.0-SNAPSHOT")
        }

        create("testlibs") {
            library("spring.boot.starter.test", "org.springframework.boot:spring-boot-starter-test:3.0.1")
            library("junit.jupiter.api", "org.junit.jupiter:junit-jupiter-api:5.8.1")
            library("junit.jupiter.engine", "org.junit.jupiter:junit-jupiter-engine:5.8.1")
            library("spring.kafka.test", "org.springframework.kafka:spring-kafka:3.0.1")
        }
    }
}

