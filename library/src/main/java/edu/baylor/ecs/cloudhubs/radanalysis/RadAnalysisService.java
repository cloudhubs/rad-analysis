package edu.baylor.ecs.cloudhubs.radanalysis;

import edu.baylor.ecs.cloudhubs.rad.context.RadRequestContext;
import edu.baylor.ecs.cloudhubs.rad.context.RadResponseContext;
import edu.baylor.ecs.cloudhubs.rad.model.RestEntity;
import edu.baylor.ecs.cloudhubs.rad.model.RestFlow;
import edu.baylor.ecs.cloudhubs.rad.service.RestDiscoveryService;
import edu.baylor.ecs.cloudhubs.radanalysis.context.RadAnalysisRequestContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.RadAnalysisResponseContext;
import edu.baylor.ecs.seer.common.SeerSecurityNode;
import edu.baylor.ecs.seer.common.context.SeerRequestContext;
import edu.baylor.ecs.seer.common.context.SeerSecurityContext;
import edu.baylor.ecs.seer.common.security.SecurityMethod;
import edu.baylor.ecs.seer.common.security.SecurityRootMethod;
import edu.baylor.ecs.seer.common.security.SeerSecurityConstraintViolation;
import edu.baylor.ecs.seer.common.security.SeerSecurityEntityAccessViolation;
import edu.baylor.ecs.seer.lweaver.service.ResourceService;
import edu.baylor.ecs.seer.lweaver.service.SeerContextService;
import edu.baylor.ecs.seer.lweaver.service.SeerMsSecurityContextService;
import javassist.CtClass;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RadAnalysisService {
    private final ResourceService resourceService;
    private final RestDiscoveryService restDiscoveryService;
    private final SeerContextService seerContextService;
    private final SeerMsSecurityContextService securityContextService;

    public RadAnalysisResponseContext generateRadAnalysisResponseContext(RadAnalysisRequestContext request) {
        RadAnalysisResponseContext responseContext = new RadAnalysisResponseContext();

        RadResponseContext radResponseContext = generateRadResponseContext(request);
        responseContext.setRestFlowContext(radResponseContext.getRestFlowContext());

        List<SeerSecurityContext> securityContexts = generateSeerSecurityContext(request);

        // combine security root methods
        Set<SecurityRootMethod> combinedRootMethods = new HashSet<>();
        for (SeerSecurityContext securityContext : securityContexts) {
            combinedRootMethods.addAll(securityContext.getSecurityRoots());
        }

        // build controller to controller flow along with security roles
        // add children based on rest flow
        // keep only those related to api calls
        Set<SecurityRootMethod> apiRootMethods = new HashSet<>();
        for (SecurityRootMethod rootMethod : combinedRootMethods) {
            Set<String> newChildMethods = new HashSet<>();

            for (String child : rootMethod.getChildMethods()) {
                for (RestFlow restFlow : radResponseContext.getRestFlowContext().getRestFlows()) {
                    if (child.contains(restFlow.getClassName()) && child.contains(restFlow.getMethodName())) {
                        // add all servers as child
                        for (RestEntity server : restFlow.getServers()) {
                            newChildMethods.add(server.getClassName() + "." + server.getMethodName());
                        }
                    }
                }
            }

            if (newChildMethods.size() > 0) { // match found
                rootMethod.setChildMethods(newChildMethods);
                apiRootMethods.add(rootMethod);
            }
        }

        // TODO test all security methods
        String roleHierarchy = request.getSecurityAnalyzerInterface();
        SeerSecurityNode root = securityContextService.createRoleTree(roleHierarchy);
        SeerSecurityContext securityContext = new SeerSecurityContext(roleHierarchy, root);

        securityContext.setSecurityRoots(apiRootMethods);
        Map<String, SecurityMethod> map = securityContextService.buildMap(securityContext);

        List<SecurityMethod> allSecurityMethods = new ArrayList<>();
        for (Map.Entry entry : map.entrySet()) {
            allSecurityMethods.add((SecurityMethod) entry.getValue());
        }

        // add own roles of child methods that are root methods
        for (SecurityMethod securityMethod : allSecurityMethods) {
            for (SecurityRootMethod rootMethod : combinedRootMethods) {
                if (rootMethod.getMethodName().contains(securityMethod.getMethodName())) {
                    securityMethod.getRoles().addAll(rootMethod.getRoles());
                }
            }
        }

        responseContext.setAllSecurityMethods(allSecurityMethods);

//        SeerSecurityContext newSecurityContext = securityContextService.getRadSecurityContext(request.getSecurityAnalyzerInterface(), combinedRootMethods, apiRootMethods);
//
//        responseContext.setApiSecurityRootMethods(apiRootMethods);
//        responseContext.setEntityAccessViolations(newSecurityContext.getEntityAccessViolations());
//        responseContext.setConstraintViolations(newSecurityContext.getRoleViolations());

        List<SecurityMethod> violatingMethods = allSecurityMethods
                .stream()
                .filter(x -> x.getRoles().size() > 1)
                .collect(Collectors.toList());

        Set<SeerSecurityConstraintViolation> roleViolations = securityContextService.findEndpointRoleViolations(securityContext, apiRootMethods);
        roleViolations.addAll(securityContextService.findFlowViolations(securityContext, violatingMethods));
        Set<SeerSecurityEntityAccessViolation> entityAccessViolations = securityContextService.findApiViolations(securityContext, apiRootMethods);

        securityContext.setRoleViolations(roleViolations);
        securityContext.setEntityAccessViolations(entityAccessViolations);

        responseContext.setEntityAccessViolations(securityContext.getEntityAccessViolations());
        responseContext.setConstraintViolations(securityContext.getRoleViolations());

        return responseContext;
    }

    private RadResponseContext generateRadResponseContext(RadAnalysisRequestContext request) {
        return restDiscoveryService.generateRadResponseContext(convertToRadRequestContext(request));
    }

    private List<SeerSecurityContext> generateSeerSecurityContext(RadAnalysisRequestContext request) {
        List<SeerSecurityContext> securityContexts = new ArrayList<>();

        List<String> resourcePaths = resourceService.getResourcePaths(request.getPathToCompiledMicroservices());
        for (String path : resourcePaths) {
            List<CtClass> ctClasses = resourceService.getCtClasses(path, request.getOrganizationPath());
            SeerRequestContext seerRequestContext = covertToSeerRequestContext(request);
            SeerSecurityContext securityContext = securityContextService.getMsSeerSecurityContext(ctClasses, seerRequestContext);
            securityContexts.add(securityContext);
        }

        return securityContexts;
    }

    private SeerRequestContext covertToSeerRequestContext(RadAnalysisRequestContext request) {
        SeerRequestContext seerRequestContext = new SeerRequestContext();
        seerRequestContext.setPathToCompiledMicroservices(request.getPathToCompiledMicroservices());
        seerRequestContext.setOrganizationPath(request.getOrganizationPath());
        seerRequestContext.setSecurityAnalyzerInterface(request.getSecurityAnalyzerInterface());
        return seerRequestContext;
    }

    private RadRequestContext convertToRadRequestContext(RadAnalysisRequestContext request) {
        return new RadRequestContext(
                request.getPathToCompiledMicroservices(),
                request.getOrganizationPath(),
                request.getOutputPath());
    }

}
