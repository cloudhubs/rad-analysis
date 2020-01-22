package edu.baylor.ecs.cloudhubs.radanalysis.app;

import edu.baylor.ecs.cloudhubs.radanalysis.context.Deployed.*;
import edu.baylor.ecs.cloudhubs.radanalysis.context.RadAnalysisRequestContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.RadAnalysisResponseContext;
import edu.baylor.ecs.cloudhubs.radanalysis.service.DeployedAnalysisService;
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
public class Controller {
    private final RadAnalysisService radAnalysisService;
    private final DeployedAnalysisService deployedAnalysisService;
    private final RestTemplate restTemplate;

    public Controller() {
        this.radAnalysisService = new RadAnalysisService();
        this.deployedAnalysisService = new DeployedAnalysisService();
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
        // extract jar and specify jarPath
        if (request.getDockerImage() != null) {
            runExtractScript(request.getDockerImage());
            request.setJarPath("/target/target.jar");
        }

        // check if jarPath exists
        if (!new File(request.getJarPath()).exists()) {
            throw new IOException("JAR path does not exists");
        }

        return deployedAnalysisService.generateDiscreteResponseContext(request);
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(path = "/combined", method = RequestMethod.POST, produces = "application/json; charset=UTF-8", consumes = {"text/plain", "application/*"})
    @ResponseBody
    public CombinedResponseContext getCombinedResponseContext(@RequestBody CombinedRequestContext request) {
        return deployedAnalysisService.generateCombinedResponseContext(request);
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(path = "/analyse", method = RequestMethod.POST, produces = "application/json; charset=UTF-8", consumes = {"text/plain", "application/*"})
    @ResponseBody
    public CombinedResponseContext getAnalysis(@RequestBody AnalysisRequestContext request) {
        List<DiscreteResponseContext> discreteResponseContexts = new ArrayList<>();

        String securityInterface = "SuperAdmin \n SuperAdmin->Admin \n SuperAdmin->Reviewer \n Admin->User \n User->Guest \n Admin->Moderator";

        for (DiscreteArtifact artifact : request.getDiscreteArtifacts()) {
            DiscreteRequestContext discreteRequestContext = new DiscreteRequestContext(
                    artifact.getImageName(),
                    null,
                    "edu/baylor/ecs",
                    artifact.getServiceName(),
                    securityInterface
            );


            DiscreteResponseContext discreteResponseContext = restTemplate.postForObject(
                    "192.168.64.2:31553/discrete", request, DiscreteResponseContext.class);
            discreteResponseContexts.add(discreteResponseContext);
        }


        return deployedAnalysisService.generateCombinedResponseContext(new CombinedRequestContext(securityInterface, discreteResponseContexts));
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(path = "/artifacts", method = RequestMethod.GET, produces = "application/json; charset=UTF-8", consumes = {"text/plain", "application/*"})
    @ResponseBody
    public AnalysisRequestContext getDeployedArtifacts() throws ApiException, IOException {
        List<DiscreteArtifact> discreteArtifacts = new ArrayList<>();

        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);

        CoreV1Api api = new CoreV1Api();
        String namespace = "default";

        V1ServiceList svcList = api.listNamespacedService(
                namespace, null, null, null, null,
                null, null, null, null, null);


        for (V1Service service : svcList.getItems()) {
            String labelSelector = getLabelSelector(service.getSpec().getSelector());

            V1PodList podList = api.listNamespacedPod(namespace, null, null, null, null,
                    labelSelector, null, null, null, null);

            for (V1Pod pod : podList.getItems()) {
                discreteArtifacts.add(new DiscreteArtifact(
                        service.getMetadata().getName(),
                        pod.getSpec().getContainers().get(0).getName()));
            }
        }

        return new AnalysisRequestContext(discreteArtifacts);
    }

    private String getLabelSelector(Map<String, String> selectors) {
        if (selectors == null) return null;

        String labelSelector = selectors.keySet().stream()
                .map(key -> key + "=" + selectors.get(key))
                .collect(Collectors.joining(","));
        return labelSelector;
    }

    private void runExtractScript(String dockerImage) throws InterruptedException, IOException {
        List<String> cmdList = new ArrayList<>();
        cmdList.add("sh");
        cmdList.add("/extract.sh");
        cmdList.add(dockerImage);

        ProcessBuilder pb = new ProcessBuilder(cmdList);
        Process p = pb.start();
        p.waitFor();

        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
    }
}
