plugins {
	java
	id("org.springframework.boot") version "3.4.4"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-web")
	compileOnly("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	runtimeOnly("org.postgresql:postgresql")
	implementation ("org.springframework.boot:spring-boot-starter-validation")
	implementation("com.amazonaws:aws-java-sdk-s3:1.12.772")
	implementation("org.springframework.boot:spring-boot-starter-jdbc")

	// netty
	implementation("org.springframework.boot:spring-boot-starter-reactor-netty")
	developmentOnly("io.netty:netty-all:4.1.100.Final")

	// jjwt
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	implementation("io.jsonwebtoken:jjwt-impl:0.12.6")
	implementation("io.jsonwebtoken:jjwt-jackson:0.12.6")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("com.google.api-client:google-api-client:2.2.0")

	// redis
	implementation("org.springframework.boot:spring-boot-starter-data-redis")

	// aws s3
	implementation(platform("software.amazon.awssdk:bom:2.24.0"))
	implementation("software.amazon.awssdk:s3:2.31.53")
	implementation("com.vladmihalcea:hibernate-types-60:2.21.1")
	implementation("io.github.cdimascio:dotenv-java:2.2.0")

	// kafka
	implementation("org.springframework.kafka:spring-kafka")

	// fcm
	implementation("com.squareup.okhttp3:okhttp:4.12.0") // 최신 안정 버전
	implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")

	// swagger
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")

	// QueryDSL
	implementation("com.querydsl:querydsl-jpa:5.0.0:jakarta")
	implementation("com.querydsl:querydsl-apt:5.0.0:jakarta")
	annotationProcessor("com.querydsl:querydsl-apt:5.0.0:jakarta")
	annotationProcessor("jakarta.annotation:jakarta.annotation-api")
	annotationProcessor("jakarta.persistence:jakarta.persistence-api")

	// PDF 생성용 OpenPDF
	implementation("com.github.librepdf:openpdf:1.3.30")

	// iText 7 핵심 PDF 생성 라이브러리
	implementation("com.itextpdf:kernel:7.2.5")
	implementation("com.itextpdf:layout:7.2.5")
	implementation("com.itextpdf:io:7.2.5")

	// Apache POI
	implementation("org.apache.poi:poi-ooxml:5.2.5")

	// flyway
	implementation("org.flywaydb:flyway-core:9.22.0")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// Querydsl 설정
val generated = "src/main/generated"

// querydsl QClass 파일 생성 위치를 지정
tasks.withType<JavaCompile> {
	options.release.set(21)
	options.generatedSourceOutputDirectory.set(file(generated))
}

// java source set 에 querydsl QClass 위치 추가
sourceSets {
	named("main") {
		java {
			"src/main/java"
			srcDir(generated)
		}
	}
}

// gradle clean 시에 QClass 디렉토리 삭제
tasks.named<Delete>("clean") {
	delete(file(generated))
}
