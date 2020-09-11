package edu.baylor.ecs.cloudhubs.radanalysis.app;

import edu.baylor.ecs.cloudhubs.radanalysis.service.KubeService;
import io.kubernetes.client.ApiException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class RadAnalysisApplication {

    public static void main(String[] args) throws IOException, ApiException {
        // mvn spring-boot:run -Dspring-boot.run.arguments='--k8s'
        if (args.length > 0 && args[0].equals("--k8s")) {
            KubeService.watchServices();
        }
        SpringApplication.run(RadAnalysisApplication.class, args);
    }

}
