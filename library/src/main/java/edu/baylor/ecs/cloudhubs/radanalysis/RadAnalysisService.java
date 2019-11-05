package edu.baylor.ecs.cloudhubs.radanalysis;

import edu.baylor.ecs.cloudhubs.rad.context.RadRequestContext;
import edu.baylor.ecs.cloudhubs.rad.context.RadResponseContext;
import edu.baylor.ecs.cloudhubs.rad.service.RestDiscoveryService;
import edu.baylor.ecs.cloudhubs.radanalysis.context.RadAnalysisRequestContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.RadAnalysisResponseContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.SecurityContextWrapper;
import edu.baylor.ecs.seer.common.context.SeerContext;
import edu.baylor.ecs.seer.common.context.SeerMsContext;
import edu.baylor.ecs.seer.common.context.SeerRequestContext;
import edu.baylor.ecs.seer.lweaver.service.SeerContextService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RadAnalysisService {

    private final RestDiscoveryService restDiscoveryService;
    private final SeerContextService seerContextService;

    public RadAnalysisResponseContext generateRadAnalysisResponseContext(RadAnalysisRequestContext request) {
        RadAnalysisResponseContext responseContext = new RadAnalysisResponseContext();

        RadResponseContext radResponseContext = generateRadResponseContext(request);
        responseContext.setRestFlowContext(radResponseContext.getRestFlowContext());

        SeerContext seerContext = generateSeerContext(request);
        for (SeerMsContext msContext : seerContext.getMsContexts()) {
            SecurityContextWrapper securityContextWrapper = new SecurityContextWrapper(msContext.getModuleName(), msContext.getSecurity());
            responseContext.getSecurityContexts().add(securityContextWrapper);
        }

        return responseContext;
    }

    private RadResponseContext generateRadResponseContext(RadAnalysisRequestContext request) {
        return restDiscoveryService.generateRadResponseContext(new RadRequestContext(
                request.getPathToCompiledMicroservices(),
                request.getOrganizationPath(),
                request.getOutputPath()));
    }

    private SeerContext generateSeerContext(RadAnalysisRequestContext request) {
        // Initialize a new SeerContext with the request
        SeerContext context = new SeerContext();

        SeerRequestContext seerRequestContext = new SeerRequestContext();
        seerRequestContext.setPathToCompiledMicroservices(request.getPathToCompiledMicroservices());
        seerRequestContext.setOrganizationPath(request.getOrganizationPath());
        seerRequestContext.setSecurityAnalyzerInterface(request.getSecurityAnalyzerInterface());

        context.setRequest(seerRequestContext);

        // Generate the full SeerContext
        return seerContextService.populateSeerContext(context);
    }

}
