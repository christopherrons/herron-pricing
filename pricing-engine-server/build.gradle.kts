dependencies {
    // External Libs
    implementation(libs.spring.boot.starter.web)
    implementation(libs.tyrus.standalone.client)
    implementation(libs.javax.json.api)
    implementation(libs.javax.json)
    implementation(libs.javafaker)
    implementation(libs.spring.kafka)

    // Internal Libs
    implementation(libs.common.api)
    implementation(libs.common)

    // External Test Libs
    testImplementation(testlibs.junit.jupiter.api)
    testImplementation(testlibs.junit.jupiter.engine)
    testImplementation(testlibs.spring.boot.starter.test)
}

tasks.test {
    useJUnitPlatform()
}