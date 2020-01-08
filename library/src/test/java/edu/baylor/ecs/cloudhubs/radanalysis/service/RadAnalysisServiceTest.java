package edu.baylor.ecs.cloudhubs.radanalysis.service;

import edu.baylor.ecs.cloudhubs.radanalysis.context.RadAnalysisRequestContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.RadAnalysisResponseContext;
import edu.baylor.ecs.seer.common.security.SeerSecurityConstraintViolation;
import edu.baylor.ecs.seer.common.security.ViolationType;
import org.junit.jupiter.api.Test;

class RadAnalysisServiceTest {

    @Test
    void generateRadAnalysisResponseContext() {
        RadAnalysisService radAnalysisService = new RadAnalysisService();
        RadAnalysisRequestContext requestContext = new RadAnalysisRequestContext(
                "..",
                "edu/baylor/ecs/cloudhubs/radanalysis/sample",
                null,
                "SuperAdmin \n SuperAdmin->Admin \n SuperAdmin->Reviewer \n Admin->User \n User->Guest \n Admin->Moderator"
        );
        RadAnalysisResponseContext responseContext = radAnalysisService.generateRadAnalysisResponseContext(requestContext);

        int countUnrestrictedAccess = 0;
        int countHierarchy = 0;

        for (SeerSecurityConstraintViolation violation : responseContext.getApiSecurityContext().getConstraintViolations()) {
            if (violation.getType() == ViolationType.UNRESTRICTED_ACCESS) {
                countUnrestrictedAccess++;
            } else if (violation.getType() == ViolationType.HIERARCHY) {
                countHierarchy++;
            }
        }

        // TODO
        // assertEquals(countUnrestrictedAccess, 2);
        // assertEquals(countHierarchy, 1);
    }
}