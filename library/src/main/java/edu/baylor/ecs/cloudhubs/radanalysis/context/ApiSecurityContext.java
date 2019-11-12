package edu.baylor.ecs.cloudhubs.radanalysis.context;

import edu.baylor.ecs.seer.common.security.SecurityMethod;
import edu.baylor.ecs.seer.common.security.SeerSecurityConstraintViolation;
import edu.baylor.ecs.seer.common.security.SeerSecurityEntityAccessViolation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Getter
@Setter
public class ApiSecurityContext {
    private List<SecurityMethod> allSecurityMethods;
    Set<SeerSecurityEntityAccessViolation> entityAccessViolations;
    Set<SeerSecurityConstraintViolation> constraintViolations;
}
