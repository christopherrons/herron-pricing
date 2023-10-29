dependencies {
    // External Libs
    implementation(libs.spring.boot.starter.web)
    implementation(libs.tyrus.standalone.client)
    implementation(libs.javax.json.api)
    implementation(libs.javax.json)
    implementation(libs.javafaker)
    implementation(libs.spring.kafka)
    implementation(libs.commons.math)

    // Internal Libs
    implementation(libs.common.api)
    implementation(libs.common)
    implementation(libs.integration.api)
    implementation(libs.integrations)
    implementation(libs.quantlib.api)
    implementation(libs.quantlib)

    // External Test Libs
    testImplementation(testlibs.junit.jupiter.api)
    testImplementation(testlibs.junit.jupiter.engine)
    testImplementation(testlibs.spring.boot.starter.test)
}

tasks.test {
    useJUnitPlatform()
}