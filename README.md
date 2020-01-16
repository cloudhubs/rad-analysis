# Rest Security Analysis

This project detects role based access control (RBAC) violations that can happen during the REST communication between microservices. It takes a set of compiled microservice artifacts and a role hierarchy as input and generates a list of RBAC violations.

To get started clone the Github [repository](https://github.com/cloudhubs/rad-analysis).

```
$ git clone https://github.com/cloudhubs/rad-analysis.git
```

## Major Dependencies

- [RAD](https://github.com/cloudhubs/rad)
- [Local weaver](https://bitbucket.org/cilab/local-weaver)
- [Spring boot](https://spring.io/projects/spring-boot)
- [Lombok](https://projectlombok.org/)

## Core components and library uses

1. **RAD:** The rad library detects the REST communication between microservices from a set of bytecode artifacts.

2. **Local weaver (security service):** The security service of local weaver library process the role hierarchy and propagates appropriate roles from controller methods to child methods.

3. **Rad Analysis Service:** Detects the RBAC violations for controller to controller REST communication between a pair of microservices.

## Run the Application

### Prepare the `Local weaver` library

```
$ git clone https://{username}@bitbucket.org/cilab/local-weaver.git
$ cd local-weaver
$ git checkout rest
$ mvn clean install -DskipTests
```

### Prepare the test bed 

We will use [CIL-TMS](https://bitbucket.org/cilab/cil-tms/src/master/) (`rad` branch) as our test bed.

```
$ git clone https://{username}@bitbucket.org/cilab/cil-tms.git
$ cd cil-tms
$ git checkout rad-analysis
```

Package each microservice.

```
$ ./buildAll.sh
```

### Compile and run the application

```
$ git clone https://github.com/cloudhubs/rad-analysis.git
$ cd rad-analysis
$ mvn clean install -DskipTests
$ java -jar application/target/rad-analysis-0.0.5.jar
```

### Sample request and response

```
curl --request POST \
  --url http://localhost:8080/ \
  --header 'content-type: application/json' \
  --data '{
    "pathToCompiledMicroservices":"C:\\baylor\\cil-tms",
	  "organizationPath":"edu/baylor/ecs",
		"securityAnalyzerInterface": "SuperAdmin \n SuperAdmin->Admin \n SuperAdmin->Reviewer \n Admin->User \n User->Guest \n Admin->Moderator"
}'
```

```yaml
{
	"securityContexts": [{
			"resourcePath": "C:\\baylor\\cil-tms\\tms-cms\\target\\cms-0.0.1-SNAPSHOT.jar",
			"security": {
				"securityRoleSpecificationSource": "SuperAdmin \n SuperAdmin->Admin \n SuperAdmin->Reviewer \n Admin->User \n User->Guest \n Admin->Moderator",
				"root": {
					"data": "SuperAdmin",
					"children": [{
							"data": "Admin",
							"children": [{
									"data": "User",
									"children": [{
										"data": "Guest",
										"children": []
									}]
								},
								{
									"data": "Moderator",
									"children": []
								}
							]
						},
						{
							"data": "Reviewer",
							"children": []
						}
					]
				},
				"roleViolations": [],
				"entityAccessViolations": [],
				"securityRoots": [{
						"methodName": "edu.baylor.ecs.cms.controller.CategoryInfoController.getCategoryInfo()",
						"childMethods": [
							"edu.baylor.ecs.qms.controller.CategoryInfoController.findAllCategoryInfos"
						],
						"roles": [
							"user"
						],
						"httpType": "NONE",
						"parameters": [],
						"returnType": "java.util.List<java.lang.Object>"
					},
				    ...
				]
			}
		},
		...
	],
	"restFlowContext": {
		"restFlows": [{
				"resourcePath": "C:\\baylor\\cil-tms\\tms-cms\\target\\cms-0.0.1-SNAPSHOT.jar",
				"className": "edu.baylor.ecs.cms.service.EmsService",
				"methodName": "createExam",
				"servers": [{
					"url": "http://localhost:10002/exam",
					"applicationName": null,
					"ribbonServerName": null,
					"resourcePath": "C:\\baylor\\cil-tms\\tms-ems\\target\\ems-0.1.0.jar",
					"className": "edu.baylor.ecs.ems.controller.ExamController",
					"methodName": "createExam",
					"returnType": "edu.baylor.ecs.ems.model.Exam",
					"path": "/exam",
					"pathParams": null,
					"formParams": null,
					"queryParams": null,
					"headerParams": null,
					"cookieParams": null,
					"matrixParams": null,
					"httpMethod": "POST",
					"consumeType": null,
					"produceType": "application/json; charset=UTF-8",
					"client": false
				}]
			},
			...
		]
	},
	"apiSecurityContext": {
		"allSecurityMethods": [{
				"methodName": "edu.baylor.ecs.cms.controller.ExamController.getExamDetail(java.lang.Integer)",
				"childMethods": [
					"edu.baylor.ecs.ems.controller.ExamController.listAllQuestionsForExam"
				],
				"roles": [
					"user"
				]
			},
			...
		],
		"entityAccessViolations": [],
		"constraintViolations": [{
				"type": "UNRELATED",
				"method": "edu.baylor.ecs.qms.controller.ConfigurationController.findAllConfigurations",
				"roles": [
					"moderator",
					"user"
				]
			},
			{
				"type": "HIERARCHY",
				"method": "edu.baylor.ecs.qms.controller.ConfigurationController.createConfiguration",
				"roles": [
					"admin",
					"user"
				]
			}
		]
	}
}
```

## Integrate as library

### Compile the library

```
$ git clone https://github.com/cloudhubs/rad.git
$ cd rad
$ mvn clean install -DskipTests
```

### Add dependency to your project

```xml
<dependency>
    <groupId>edu.baylor.ecs.cloudhubs</groupId>
    <artifactId>rad</artifactId>
    <version>0.0.5</version>
</dependency>
```

### Code example

```java
@Autowired
private final RadAnalysisService radAnalysisService;
   
public RadAnalysisResponseContext getRadResponseContext(@RequestBody RadAnalysisRequestContext request) {
        return radAnalysisService.generateRadAnalysisResponseContext(request);
}
```

## Core Contexts and Models

```java
public class RadAnalysisRequestContext {
    private String pathToCompiledMicroservices;
    private String organizationPath;
    private String outputPath;
    private String securityAnalyzerInterface;
}
```

```java
public class RadAnalysisResponseContext {
    List<SecurityContextWrapper> securityContexts = new ArrayList<>();
    SeerRestFlowContext restFlowContext;
    ApiSecurityContext apiSecurityContext;
}
```

```java
public class ApiSecurityContext {
    private List<SecurityMethod> allSecurityMethods;
    Set<SeerSecurityEntityAccessViolation> entityAccessViolations;
    Set<SeerSecurityConstraintViolation> constraintViolations;
}
```

```java
public class SecurityContextWrapper {
    private String resourcePath;
    private SeerSecurityContext security;
}
```

