package edu.baylor.ecs.cloudhubs.radanalysis.context;

import edu.baylor.ecs.cloudhubs.rad.context.SeerRestFlowContext;
import edu.baylor.ecs.seer.common.context.SeerContext;
import edu.baylor.ecs.seer.common.context.SeerSecurityContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RadAnalysisResponseContext {
    SeerContext seerContext;
    SeerRestFlowContext seerRestFlowContext;
}
