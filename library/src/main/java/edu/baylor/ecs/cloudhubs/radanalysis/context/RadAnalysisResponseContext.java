package edu.baylor.ecs.cloudhubs.radanalysis.context;

import edu.baylor.ecs.cloudhubs.rad.context.SeerRestFlowContext;
import edu.baylor.ecs.seer.common.security.SecurityRootMethod;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class RadAnalysisResponseContext {
    List<SecurityContextWrapper> securityContexts = new ArrayList<>();
    Set<SecurityRootMethod> apiSecurityRootMethods = new HashSet<>();
    SeerRestFlowContext restFlowContext;
}
