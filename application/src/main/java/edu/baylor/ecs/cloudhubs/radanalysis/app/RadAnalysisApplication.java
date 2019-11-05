package edu.baylor.ecs.cloudhubs.radanalysis.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"edu.baylor.ecs.cloudhubs.radanalysis", "edu.baylor.ecs.cloudhubs.rad", "edu.baylor.ecs.seer.lweaver.service"})
public class RadAnalysisApplication {

    public static void main(String[] args) {
        SpringApplication.run(RadAnalysisApplication.class, args);
    }

}
