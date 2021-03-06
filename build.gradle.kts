import org.jooq.meta.jaxb.Property

plugins {
    id("java")
    id("org.springframework.boot") version "2.6.3"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("nu.studer.jooq") version "7.1.1"
    id("org.liquibase.gradle") version "2.0.4"
}

group = "org.gh"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    jooqGenerator("mysql:mysql-connector-java:8.0.29")
    jooqGenerator("jakarta.xml.bind:jakarta.xml.bind-api:3.0.1")
    jooqGenerator("org.jooq:jooq-meta-extensions-liquibase:3.17.0")
    jooqGenerator("org.liquibase:liquibase-core:4.12.0")
    jooqGenerator(files("src/main/resources"))

    liquibaseRuntime("mysql:mysql-connector-java:8.0.29")
    liquibaseRuntime("org.liquibase:liquibase-core:4.12.0")

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-jooq")

    implementation("org.jooq", "jooq","3.17.0")
    implementation("org.jooq", "jooq-meta","3.17.0")
    implementation("org.jooq", "jooq-codegen","3.17.0")

    implementation("mysql:mysql-connector-java:8.0.29")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

liquibase {
    activities.register("main") {
        val dbUrl = project.extra.properties["db_url"]
        val dbUser = project.extra.properties["db_user"]
        val dbPassword = project.extra.properties["db_password"]

        this.arguments = mapOf(
            "logLevel" to "info",
            "changeLogFile" to "src/main/resources/changelog.yml",
            "url" to dbUrl,
            "username" to dbUser,
            "password" to dbPassword,
            "driver" to "com.mysql.cj.jdbc.Driver"
        )
    }
    runList = "main"
}

jooq {
    configurations {
        create("main") {
            jooqConfiguration.apply {
                generator.apply {
                    database.apply {
                        name = "org.jooq.meta.extensions.liquibase.LiquibaseDatabase"
                        properties = listOf(
                            Property().apply {
                                key = "dialect"
                                value = "MYSQL"
                            },
                            Property().apply {
                                key = "scripts"
                                value = "changelog.yml"
                            },
                            Property().apply {
                                key = "includeLiquibaseTables"
                                value = "false"
                            }
                        )
                    }
                    target.apply {
                        packageName = "org.gh.jooq"
                    }
                }
            }

        }
    }
}
