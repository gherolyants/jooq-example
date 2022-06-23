import org.jooq.meta.Databases.database
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
    jooqGenerator("org.jooq:jooq-meta-extensions-liquibase:3.16.7")
    jooqGenerator("org.liquibase:liquibase-core:4.12.0")
    jooqGenerator(files("src/main/resources"))

    liquibaseRuntime("mysql:mysql-connector-java:8.0.29")
    liquibaseRuntime("org.liquibase:liquibase-core:4.12.0")

    implementation("org.springframework.boot:spring-boot-starter")

    implementation("org.jooq", "jooq","3.16.7")
    implementation("org.jooq", "jooq-meta","3.16.7")
    implementation("org.jooq", "jooq-codegen","3.16.7")

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
            "changeLogFile" to "src/main/resources/changelog.yaml",
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
//                            Property().apply {
//                                key = "rootPath"
//                                value = "../src/main/resources"
//                            },
                            Property().apply {
                                key = "scripts"
                                value = "changelog.yaml"
                            },
                            Property().apply {
                                key = "includeLiquibaseTables"
                                value = "false"
                            }
                        )
                    }
                    target.apply {
                        packageName = "org.gh"
                    }

                }
            }

        }
    }
}
//myConfigurationName(sourceSets.main) {
//    generator {
//        database {
//            name = 'org.jooq.meta.extensions.liquibase.LiquibaseDatabase'
//            properties {
//
//                // Specify the root path, e.g. a path in your Maven directory layout
//                property {
//                    key = 'rootPath'
//                    value = '${basedir}/src/main/resources'
//                }
//
//                // Specify the relative path location of your XML, YAML, or JSON script.
//                property {
//                    key = 'scripts'
//                    value = 'database.xml'
//                }
//            }
//        }
//    }
//}

//jooq {
//    configurations {
//        create("main") {  // name of the jOOQ configuration
//            jooqConfiguration.apply {
//                generator.apply {
//                    database.apply {
//                        name = "org.jooq.meta.extensions.liquibase.LiquibaseDatabase"
//                        properties = listOf(
//                            Property().apply {
//                                key = "scripts"
//                                value = "changelog.yaml"
//                            },
//                            Property().apply {
//                                key = "includeLiquibaseTables"
//                                value = "false"
//                            }
//                        )
//                    }
//                    generate.apply {
//                        isDeprecated = false
//                        isRecords = true
//                        isImmutablePojos = true
//                        isFluentSetters = true
//                    }
//                    target.apply {
//                        packageName = "org.gh"
//                    }
//                    strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
//                }
//            }
//        }
//    }
//}