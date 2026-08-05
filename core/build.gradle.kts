plugins {
    id("org.jetbrains.kotlin.jvm")
    `maven-publish`
}

group = "com.github.Dicecan.NetEaseDecryptorSDK"
version = "2.0.0"

dependencies {
    implementation(kotlin("stdlib"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "core"
            version = project.version.toString()
        }
    }
}
