package edu.baylor.ecs.cloudhubs.radanalysis.app;

import edu.baylor.ecs.cloudhubs.radanalysis.context.Deployed.CombinedRequestContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.Deployed.CombinedResponseContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.Deployed.DiscreteRequestContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.Deployed.DiscreteResponseContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.RadAnalysisRequestContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.RadAnalysisResponseContext;
import edu.baylor.ecs.cloudhubs.radanalysis.service.DeployedAnalysisService;
import edu.baylor.ecs.cloudhubs.radanalysis.service.RadAnalysisService;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@RestController
public class Controller {
    private final RadAnalysisService radAnalysisService;
    private final DeployedAnalysisService deployedAnalysisService;

    public Controller() {
        this.radAnalysisService = new RadAnalysisService();
        this.deployedAnalysisService = new DeployedAnalysisService();
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
        return deployedAnalysisService.generateDiscreteResponseContext(request);
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(path = "/combined", method = RequestMethod.POST, produces = "application/json; charset=UTF-8", consumes = {"text/plain", "application/*"})
    @ResponseBody
    public CombinedResponseContext getCombinedResponseContext(@RequestBody CombinedRequestContext request) {
        return deployedAnalysisService.generateCombinedResponseContext(request);
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
