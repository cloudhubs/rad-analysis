package edu.baylor.ecs.cloudhubs.radanalysis.context.Deployed;

import edu.baylor.ecs.cloudhubs.rad.context.RestFlowContext;
import edu.baylor.ecs.cloudhubs.radanalysis.context.ApiSecurityContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CombinedResponseContext {
    RestFlowContext restFlowContext;
    ApiSecurityContext apiSecurityContext;
}
