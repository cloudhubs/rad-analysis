package edu.baylor.ecs.cloudhubs.radanalysis.app;

import edu.baylor.ecs.cloudhubs.radanalysis.context.Deployed.*;
import edu.baylor.ecs.cloudhubs.radanalysis.context.RadAnalysisRequestContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.RadAnalysisResponseContext;
import edu.baylor.ecs.cloudhubs.radanalysis.service.DeployedAnalysisService;
import edu.baylor.ecs.cloudhubs.radanalysis.service.KubeService;
import edu.baylor.ecs.cloudhubs.radanalysis.service.RadAnalysisService;
import io.kubernetes.client.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class Controller {
    private final RadAnalysisService radAnalysisService;
    private final DeployedAnalysisService deployedAnalysisService;
    private final KubeService kubeService;

    public Controller() {
        this.radAnalysisService = new RadAnalysisService();
        this.deployedAnalysisService = new DeployedAnalysisService();
        this.kubeService = new KubeService();
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
    @RequestMapping(path = "/artifacts", method = RequestMethod.GET, produces = "application/json; charset=UTF-8", consumes = {"text/plain", "application/*"})
    @ResponseBody
    public List<KubeArtifact> getDeployedArtifacts() throws ApiException, IOException {
        return kubeService.getDeployedArtifacts();
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(path = "/analyse", method = RequestMethod.GET, produces = "application/json; charset=UTF-8", consumes = {"text/plain", "application/*"})
    @ResponseBody
    public CombinedResponseContext getAnalysis() throws IOException, ApiException, InterruptedException {
        String securityInterface = "SuperAdmin \n SuperAdmin->Admin \n SuperAdmin->Reviewer \n Admin->User \n User->Guest \n Admin->Moderator";
        String organizationPath = "edu/baylor/ecs";

        List<DiscreteResponseContext> discreteResponseContexts = new ArrayList<>();
        List<KubeArtifact> kubeArtifacts = getDeployedArtifacts();

        for (KubeArtifact artifact : kubeArtifacts) {
            DiscreteRequestContext discreteRequestContext = new DiscreteRequestContext(
                    artifact.getImageName(),
                    null,
                    organizationPath,
                    artifact.getServiceName(),
                    securityInterface
            );

            discreteResponseContexts.add(getDiscreteResponseContext(discreteRequestContext));
        }

        return deployedAnalysisService.generateCombinedResponseContext(new CombinedRequestContext(securityInterface, discreteResponseContexts));
    }

}
