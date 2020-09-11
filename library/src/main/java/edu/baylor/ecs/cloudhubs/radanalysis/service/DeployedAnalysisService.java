package edu.baylor.ecs.cloudhubs.radanalysis.service;

import edu.baylor.ecs.cloudhubs.rad.context.RequestContext;
import edu.baylor.ecs.cloudhubs.rad.context.RestEntityContext;
import edu.baylor.ecs.cloudhubs.rad.context.RestFlowContext;
import edu.baylor.ecs.cloudhubs.rad.model.RestEntity;
import edu.baylor.ecs.cloudhubs.rad.model.RestFlow;
import edu.baylor.ecs.cloudhubs.rad.service.ResourceService;
import edu.baylor.ecs.cloudhubs.rad.service.RestDiscoveryService;
import edu.baylor.ecs.cloudhubs.rad.service.RestFlowService;
import edu.baylor.ecs.cloudhubs.radanalysis.context.ApiSecurityContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.Deployed.CombinedRequestContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.Deployed.CombinedResponseContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.Deployed.DiscreteRequestContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.Deployed.DiscreteResponseContext;
import edu.baylor.ecs.seer.common.SeerSecurityNode;
import edu.baylor.ecs.seer.common.context.SeerRequestContext;
import edu.baylor.ecs.seer.common.context.SeerSecurityContext;
import edu.baylor.ecs.seer.common.security.SecurityMethod;
import edu.baylor.ecs.seer.common.security.SecurityRootMethod;
import edu.baylor.ecs.seer.common.security.SeerSecurityConstraintViolation;
import edu.baylor.ecs.seer.common.security.SeerSecurityEntityAccessViolation;
import edu.baylor.ecs.seer.lweaver.service.SeerMsSecurityContextService;
import javassist.CtClass;
import org.apache.commons.lang.SerializationUtils;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DeployedAnalysisService {
    private final ResourceService resourceService;
    private final RestDiscoveryService restDiscoveryService;
    private final RestFlowService restFlowService;
    private final DockerService dockerService;
    private final SeerMsSecurityContextService securityContextService;

    public DeployedAnalysisService() {
        this.resourceService = new ResourceService(new DefaultResourceLoader());
        this.restDiscoveryService = new RestDiscoveryService();
        this.restFlowService = new RestFlowService();
        this.dockerService = new DockerService();
        this.securityContextService = new SeerMsSecurityContextService();
    }

    public static Map<String, DiscreteResponseContext> cacheForDockerImage = new HashMap<>();

    public DiscreteResponseContext generateDiscreteResponseContext(DiscreteRequestContext request) throws IOException, InterruptedException {
        prepareJarPath(request);

        RestEntityContext restEntityContext = restDiscoveryService.generateRestEntityContext(new RequestContext(
                request.getJarPath(),
                request.getOrganizationPath(),
                null
        ), request.getServiceDNS());

        List<CtClass> ctClasses = resourceService.getCtClasses(request.getJarPath(), request.getOrganizationPath());
        SeerRequestContext seerRequestContext = convertToSeerRequestContext(request);
        SeerSecurityContext securityContext = securityContextService.getMsSeerSecurityContext(ctClasses, seerRequestContext);

        DiscreteResponseContext response = new DiscreteResponseContext(
                request.getJarPath(),
                restEntityContext.getRestEntities(),
                securityContext
        );

        cacheForDockerImage.put(request.getDockerImage(), response);
        return response;
    }

    public CombinedResponseContext generateCombinedResponseContext(CombinedRequestContext request) {
        // create list of rest entity contexts and security contexts
        List<RestEntityContext> restEntityContexts = new ArrayList<>();
        List<SeerSecurityContext> securityContexts = new ArrayList<>();

        for (DiscreteResponseContext discreteResponseContext : request.getRestContexts()) {
            restEntityContexts.add(new RestEntityContext(
                    discreteResponseContext.getResourcePath(),
                    discreteResponseContext.getRestEntities()
            ));

            securityContexts.add(discreteResponseContext.getSecurity());
        }

        // get rest flows
        RestFlowContext restFlowContext = restFlowService.getRestFlowContext(restEntityContexts);

        // generate api security context
        ApiSecurityContext apiSecurityContext = generateApiSecurityContext(
                request.getSecurityAnalyzerInterface(),
                securityContexts,
                restFlowContext.getRestFlows());

        return new CombinedResponseContext(
                restFlowContext,
                apiSecurityContext
        );
    }

    private void prepareJarPath(DiscreteRequestContext request) throws IOException, InterruptedException {
        // extract jar and specify jarPath
        if (request.getDockerImage() != null) {
            dockerService.runExtractScript(request.getDockerImage());
            request.setJarPath("/target/target.jar");
        }

        // check if jarPath exists
        if (!new File(request.getJarPath()).exists()) {
            throw new IOException("JAR path does not exists");
        }
    }

    private ApiSecurityContext generateApiSecurityContext(String roleHierarchy, List<SeerSecurityContext> securityContexts, List<RestFlow> restFlows) {
        // prepare seer api security context
        SeerSecurityNode root = securityContextService.createRoleTree(roleHierarchy);
        SeerSecurityContext seerApiSecurityContext = new SeerSecurityContext(roleHierarchy, root);

        // combine security root methods
        Set<SecurityRootMethod> combinedRootMethods = new HashSet<>();
        for (SeerSecurityContext securityContext : securityContexts) {
            combinedRootMethods.addAll(securityContext.getSecurityRoots());
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
                // don't modify original root method
                SecurityRootMethod newRootMethod = (SecurityRootMethod) SerializationUtils.clone(rootMethod);
                newRootMethod.setChildMethods(newChildMethods);
                apiRootMethods.add(newRootMethod);
            }
        }

        return apiRootMethods;
    }

    private SeerRequestContext convertToSeerRequestContext(DiscreteRequestContext request) {
        SeerRequestContext seerRequestContext = new SeerRequestContext();
        seerRequestContext.setPathToCompiledMicroservices(request.getJarPath());
        seerRequestContext.setOrganizationPath(request.getOrganizationPath());
        seerRequestContext.setSecurityAnalyzerInterface(request.getSecurityAnalyzerInterface());
        return seerRequestContext;
    }

}
