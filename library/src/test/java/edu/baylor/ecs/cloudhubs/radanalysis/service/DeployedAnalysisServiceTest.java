package edu.baylor.ecs.cloudhubs.radanalysis.service;

import edu.baylor.ecs.cloudhubs.radanalysis.context.Deployed.CombinedRequestContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.Deployed.CombinedResponseContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.Deployed.DiscreteRequestContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.Deployed.DiscreteResponseContext;
import edu.baylor.ecs.seer.common.security.SeerSecurityConstraintViolation;
import edu.baylor.ecs.seer.common.security.ViolationType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeployedAnalysisServiceTest {

    @Test
    void generateCombinedResponseContext() {
        DeployedAnalysisService deployedAnalysisService = new DeployedAnalysisService();

        String roleHierarchy = "SuperAdmin \n SuperAdmin->Admin \n SuperAdmin->Reviewer \n Admin->User \n User->Guest \n Admin->Moderator";

        DiscreteRequestContext discreteRequestContextOne = new DiscreteRequestContext(
                null,
                "C:\\Baylor\\RA\\rad-analysis\\sample\\sample-one\\target\\sample-one-0.0.5.jar",
                "edu/baylor/ecs/cloudhubs/radanalysis/sampleone",
                roleHierarchy
        );
        DiscreteResponseContext discreteResponseContextOne = deployedAnalysisService.generateDiscreteResponseContext(discreteRequestContextOne);

        DiscreteRequestContext discreteRequestContextTwo = new DiscreteRequestContext(
                null,
                "C:\\Baylor\\RA\\rad-analysis\\sample\\sample-two\\target\\sample-two-0.0.5.jar",
                "edu/baylor/ecs/cloudhubs/radanalysis/sampletwo",
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