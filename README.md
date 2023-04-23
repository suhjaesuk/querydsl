# querydsl
### 학습 목적
- JPQL 문을 사용하여 복잡한 쿼리문을 처리할 때, 런타임 시 오류를 확인할 수 있습니다.
- QueryDsl을 사용하면 컴파일 단계에서 오류를 확인하여 쉽게 에러에 대응할 수 있습니다.
- 간단한 쿼리문일 경우 SpringDataJpa를 사용합니다.
- 복잡한 쿼리문일 경우 QueryDsl을 써서 코드를 가독성 좋게 만듭니다.
### 사용
- 복잡한 쿼리문
- 동적 쿼리문
- 페이지 처리
### build.gradle 작성
build.gradle을 아래와 같이 작성 후 build합니다.

이후 아래 과정을 순서대로 하면 Q가 생성됩니다.
1. Gradle - Tasks - build - clean
2. Gradle - Tasks - other - compileJava

```
plugins {
	id 'java'
	id 'org.springframework.boot' version '2.7.11'
	id 'io.spring.dependency-management' version '1.0.15.RELEASE'
}

group = 'study'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '17'

configurations {
	compileOnly {
		extendsFrom annotationProcessor
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
	implementation 'org.springframework.boot:spring-boot-starter-web'

	/*
	6줄이 모두 querydsl 의존 추가 라이브러리
	 */
	implementation "com.querydsl:querydsl-jpa"
	implementation "com.querydsl:querydsl-core"
	implementation "com.querydsl:querydsl-collections"
	annotationProcessor "com.querydsl:querydsl-apt:${dependencyManagement.importedProperties['querydsl.version']}:jpa" // querydsl JPAAnnotationProcessor 사용 지정
	annotationProcessor "jakarta.annotation:jakarta.annotation-api" // java.lang.NoClassDefFoundError (javax.annotation.Generated) 대응 코드
	annotationProcessor "jakarta.persistence:jakarta.persistence-api" // java.lang.NoClassDefFoundError (javax.annotation.Entity) 대응 코드

	compileOnly 'org.projectlombok:lombok'
	runtimeOnly 'com.h2database:h2'
	annotationProcessor 'org.projectlombok:lombok'
	testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('test') {
	useJUnitPlatform()
}

/* 아래까지 쭉 querydsl 설정 */
def generated = 'src/main/generated'

/* querydsl QClass 파일 생성 위치를 지정 */
tasks.withType(JavaCompile) {
	options.getGeneratedSourceOutputDirectory().set(file(generated))
}

/* java source set 에 querydsl QClass 위치 추가 */
sourceSets {
	main.java.srcDirs += [ generated ]
}

/* gradle clean 시에 QClass 디렉토리 삭제 */
clean {
	delete file(generated)
}
```
