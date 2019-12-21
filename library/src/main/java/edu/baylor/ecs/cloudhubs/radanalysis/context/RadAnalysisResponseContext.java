package edu.baylor.ecs.cloudhubs.radanalysis.context;

import edu.baylor.ecs.cloudhubs.rad.context.RestFlowContext;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RadAnalysisResponseContext {
    List<SecurityContextWrapper> securityContexts = new ArrayList<>();
    RestFlowContext restFlowContext;
    ApiSecurityContext apiSecurityContext;
}
