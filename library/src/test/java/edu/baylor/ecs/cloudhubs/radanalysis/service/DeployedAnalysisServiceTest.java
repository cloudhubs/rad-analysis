package edu.baylor.ecs.cloudhubs.radanalysis.service;

import edu.baylor.ecs.cloudhubs.radanalysis.context.Deployed.CombinedRequestContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.Deployed.CombinedResponseContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.Deployed.DiscreteRequestContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.Deployed.DiscreteResponseContext;
import edu.baylor.ecs.seer.common.security.SeerSecurityConstraintViolation;
import edu.baylor.ecs.seer.common.security.ViolationType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class DeployedAnalysisServiceTest {

    @Test
    void generateCombinedResponseContext() throws IOException, InterruptedException {
        DeployedAnalysisService deployedAnalysisService = new DeployedAnalysisService();

        String roleHierarchy = "SuperAdmin \n SuperAdmin->Admin \n SuperAdmin->Reviewer \n Admin->User \n User->Guest \n Admin->Moderator";

        String curPath = Paths.get("..").toAbsolutePath().normalize().toString();

        DiscreteRequestContext discreteRequestContextOne = new DiscreteRequestContext(
                null,
                curPath + "/sample/sample-one/target/sample-one-0.0.5.jar",
                "edu/baylor/ecs/cloudhubs/radanalysis/sampleone",
                "sample-one",
                roleHierarchy
        );
        DiscreteResponseContext discreteResponseContextOne = deployedAnalysisService.generateDiscreteResponseContext(discreteRequestContextOne);

        DiscreteRequestContext discreteRequestContextTwo = new DiscreteRequestContext(
                null,
                curPath + "/sample/sample-two/target/sample-two-0.0.5.jar",
                "edu/baylor/ecs/cloudhubs/radanalysis/sampletwo",
                "sample-two",
                roleHierarchy
        );
        DiscreteResponseContext discreteResponseContextTwo = deployedAnalysisService.generateDiscreteResponseContext(discreteRequestContextTwo);

        List<DiscreteResponseContext> restContexts = new ArrayList<>();
        restContexts.add(discreteResponseContextOne);
        restContexts.add(discreteResponseContextTwo);

        CombinedRequestContext combinedRequestContext = new CombinedRequestContext(
                roleHierarchy,
                restContexts
        );

        CombinedResponseContext combinedResponseContext = deployedAnalysisService.generateCombinedResponseContext(combinedRequestContext);

        int countUnrestrictedAccess = 0;
        int countHierarchy = 0;

        for (SeerSecurityConstraintViolation violation : combinedResponseContext.getApiSecurityContext().getConstraintViolations()) {
            if (violation.getType() == ViolationType.UNRESTRICTED_ACCESS) {
                countUnrestrictedAccess++;
            } else if (violation.getType() == ViolationType.HIERARCHY) {
                countHierarchy++;
            }
        }

        assertEquals(countUnrestrictedAccess, 2);
        assertEquals(countHierarchy, 1);
    }
}