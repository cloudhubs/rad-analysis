package edu.baylor.ecs.cloudhubs.radanalysis.app;

import edu.baylor.ecs.cloudhubs.radanalysis.service.KubeService;
import io.kubernetes.client.ApiException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class RadAnalysisApplication {

    public static void main(String[] args) throws IOException, ApiException {
        KubeService.watchServices();
        SpringApplication.run(RadAnalysisApplication.class, args);
    }

}
