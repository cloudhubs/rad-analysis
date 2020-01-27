package edu.baylor.ecs.cloudhubs.radanalysis.app;

import edu.baylor.ecs.cloudhubs.radanalysis.context.Deployed.*;
import edu.baylor.ecs.cloudhubs.radanalysis.context.RadAnalysisRequestContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.RadAnalysisResponseContext;
import edu.baylor.ecs.cloudhubs.radanalysis.service.DeployedAnalysisService;
import edu.baylor.ecs.cloudhubs.radanalysis.service.KubeService;
import edu.baylor.ecs.cloudhubs.radanalysis.service.RadAnalysisService;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodList;
import io.kubernetes.client.models.V1Service;
import io.kubernetes.client.models.V1ServiceList;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class Controller {
    private final RadAnalysisService radAnalysisService;
    private final DeployedAnalysisService deployedAnalysisService;
    private final KubeService kubeService;
    private final RestTemplate restTemplate;

    public Controller() {
        this.radAnalysisService = new RadAnalysisService();
        this.deployedAnalysisService = new DeployedAnalysisService();
        this.kubeService = new KubeService();
        this.restTemplate = new RestTemplate();
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(path = "/", method = RequestMethod.POST, produces = "application/json; charset=UTF-8", consumes = {"text/plain", "application/*"})
    @ResponseBody
    public RadAnalysisResponseContext getRadResponseContext(@RequestBody RadAnalysisRequestContext request) {
        return radAnalysisService.generateRadAnalysisResponseContext(request);
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(path = "/discrete", method = RequestMethod.POST, produces = "application/json; charset=UTF-8", consumes = {"text/plain", "application/*"})
    @ResponseBody
    public DiscreteResponseContext getDiscreteResponseContext(@RequestBody DiscreteRequestContext request) throws IOException, InterruptedException {
        return deployedAnalysisService.generateDiscreteResponseContext(request);
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(path = "/combined", method = RequestMethod.POST, produces = "application/json; charset=UTF-8", consumes = {"text/plain", "application/*"})
    @ResponseBody
    public CombinedResponseContext getCombinedResponseContext(@RequestBody CombinedRequestContext request) {
        return deployedAnalysisService.generateCombinedResponseContext(request);
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(path = "/analyse", method = RequestMethod.GET, produces = "application/json; charset=UTF-8", consumes = {"text/plain", "application/*"})
    @ResponseBody
    public CombinedResponseContext getAnalysis() throws IOException, ApiException {
        List<DiscreteResponseContext> discreteResponseContexts = new ArrayList<>();

        String securityInterface = "SuperAdmin \n SuperAdmin->Admin \n SuperAdmin->Reviewer \n Admin->User \n User->Guest \n Admin->Moderator";
        List<KubeArtifact> kubeArtifacts = getDeployedArtifacts();

        for (KubeArtifact artifact : kubeArtifacts) {
            DiscreteRequestContext discreteRequestContext = new DiscreteRequestContext(
                    artifact.getImageName(),
                    null,
                    "edu/baylor/ecs",
                    artifact.getServiceName(),
                    securityInterface
            );

            log.info(String.valueOf(artifact));

            DiscreteResponseContext discreteResponseContext = restTemplate.postForObject(
                    "http://192.168.64.2:31553/discrete", discreteRequestContext, DiscreteResponseContext.class);


            log.info(String.valueOf(discreteResponseContext));

            discreteResponseContexts.add(discreteResponseContext);
        }

        return deployedAnalysisService.generateCombinedResponseContext(new CombinedRequestContext(securityInterface, discreteResponseContexts));
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(path = "/artifacts", method = RequestMethod.GET, produces = "application/json; charset=UTF-8", consumes = {"text/plain", "application/*"})
    @ResponseBody
    public List<KubeArtifact> getDeployedArtifacts() throws ApiException, IOException {
        return kubeService.getDeployedArtifacts();
    }
}
