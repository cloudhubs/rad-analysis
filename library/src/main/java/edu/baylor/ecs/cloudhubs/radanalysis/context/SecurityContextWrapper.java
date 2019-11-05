package edu.baylor.ecs.cloudhubs.radanalysis.context;

import edu.baylor.ecs.seer.common.context.SeerSecurityContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class SecurityContextWrapper {
    private String moduleName;
    private SeerSecurityContext security;
}
