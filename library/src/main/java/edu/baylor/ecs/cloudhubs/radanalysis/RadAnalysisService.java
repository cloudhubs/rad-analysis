package edu.baylor.ecs.cloudhubs.radanalysis;

import edu.baylor.ecs.cloudhubs.rad.context.RadRequestContext;
import edu.baylor.ecs.cloudhubs.rad.context.RadResponseContext;
import edu.baylor.ecs.cloudhubs.rad.model.RestEntity;
import edu.baylor.ecs.cloudhubs.rad.model.RestFlow;
import edu.baylor.ecs.cloudhubs.rad.service.RestDiscoveryService;
import edu.baylor.ecs.cloudhubs.radanalysis.context.ApiSecurityContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.RadAnalysisRequestContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.RadAnalysisResponseContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.SecurityContextWrapper;
import edu.baylor.ecs.seer.common.SeerSecurityNode;
import edu.baylor.ecs.seer.common.context.SeerRequestContext;
import edu.baylor.ecs.seer.common.context.SeerSecurityContext;
import edu.baylor.ecs.seer.common.security.SecurityMethod;
import edu.baylor.ecs.seer.common.security.SecurityRootMethod;
import edu.baylor.ecs.seer.common.security.SeerSecurityConstraintViolation;
import edu.baylor.ecs.seer.common.security.SeerSecurityEntityAccessViolation;
import edu.baylor.ecs.seer.lweaver.service.ResourceService;
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
    private final SeerMsSecurityContextService securityContextService;

    public RadAnalysisResponseContext generateRadAnalysisResponseContext(RadAnalysisRequestContext request) {
        RadAnalysisResponseContext responseContext = new RadAnalysisResponseContext();

        RadResponseContext radResponseContext = generateRadResponseContext(request);
        responseContext.setRestFlowContext(radResponseContext.getRestFlowContext());

        // generate security contexts for microservices (original algorithm)
        // wrap resource paths
        List<SecurityContextWrapper> securityContexts = generateSeerSecurityContexts(request);
        responseContext.setSecurityContexts(securityContexts);

        // generate api security context
        ApiSecurityContext apiSecurityContext = generateApiSecurityContext(
                request,
                securityContexts,
                radResponseContext.getRestFlowContext().getRestFlows());

        responseContext.setApiSecurityContext(apiSecurityContext);

        return responseContext;
    }

    private RadResponseContext generateRadResponseContext(RadAnalysisRequestContext request) {
        return restDiscoveryService.generateRadResponseContext(convertToRadRequestContext(request));
    }

    private List<SecurityContextWrapper> generateSeerSecurityContexts(RadAnalysisRequestContext request) {
        List<SecurityContextWrapper> securityContexts = new ArrayList<>();

        List<String> resourcePaths = resourceService.getResourcePaths(request.getPathToCompiledMicroservices());
        for (String path : resourcePaths) {
            List<CtClass> ctClasses = resourceService.getCtClasses(path, request.getOrganizationPath());
            SeerRequestContext seerRequestContext = covertToSeerRequestContext(request);
            SeerSecurityContext securityContext = securityContextService.getMsSeerSecurityContext(ctClasses, seerRequestContext);
            securityContexts.add(new SecurityContextWrapper(path, securityContext));
        }

        return securityContexts;
    }

    private ApiSecurityContext generateApiSecurityContext(RadAnalysisRequestContext request, List<SecurityContextWrapper> securityContexts, List<RestFlow> restFlows) {
        // prepare seer api security context
        String roleHierarchy = request.getSecurityAnalyzerInterface();
        SeerSecurityNode root = securityContextService.createRoleTree(roleHierarchy);
        SeerSecurityContext seerApiSecurityContext = new SeerSecurityContext(roleHierarchy, root);

        // combine security root methods
        Set<SecurityRootMethod> combinedRootMethods = new HashSet<>();
        for (SecurityContextWrapper securityContext : securityContexts) {
            combinedRootMethods.addAll(securityContext.getSecurity().getSecurityRoots());
        }

        // get api security root methods
        Set<SecurityRootMethod> apiRootMethods = generateApiRootMethods(combinedRootMethods, restFlows);
        seerApiSecurityContext.setSecurityRoots(apiRootMethods);

        // get all api security methods
        List<SecurityMethod> allSecurityMethods = generateAllApiSecurityMethods(seerApiSecurityContext, combinedRootMethods);

        // find violations
        List<SecurityMethod> violatingMethods = allSecurityMethods
                .stream()
                .filter(x -> x.getRoles().size() > 1)
                .collect(Collectors.toList());

        Set<SeerSecurityConstraintViolation> roleViolations = securityContextService.findEndpointRoleViolations(seerApiSecurityContext, apiRootMethods);
        roleViolations.addAll(securityContextService.findFlowViolations(seerApiSecurityContext, violatingMethods));
        Set<SeerSecurityEntityAccessViolation> entityAccessViolations = securityContextService.findApiViolations(seerApiSecurityContext, apiRootMethods);

        // seerApiSecurityContext.setRoleViolations(roleViolations);
        // seerApiSecurityContext.setEntityAccessViolations(entityAccessViolations);

        return new ApiSecurityContext(allSecurityMethods, entityAccessViolations, roleViolations);
    }

    private List<SecurityMethod> generateAllApiSecurityMethods(SeerSecurityContext securityContext, Set<SecurityRootMethod> combinedRootMethods) {
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

        return allSecurityMethods;
    }

    private Set<SecurityRootMethod> generateApiRootMethods(Set<SecurityRootMethod> combinedRootMethods, List<RestFlow> restFlows) {
        // build controller to controller flow along with security roles
        // add children based on rest flow
        // keep only those related to api calls
        Set<SecurityRootMethod> apiRootMethods = new HashSet<>();
        for (SecurityRootMethod rootMethod : combinedRootMethods) {
            Set<String> newChildMethods = new HashSet<>();

            for (String child : rootMethod.getChildMethods()) {
                for (RestFlow restFlow : restFlows) {
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

        return apiRootMethods;
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
