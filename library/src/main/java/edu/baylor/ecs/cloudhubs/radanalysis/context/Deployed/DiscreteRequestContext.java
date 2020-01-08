package edu.baylor.ecs.cloudhubs.radanalysis.context.Deployed;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class DiscreteRequestContext {
    private String dockerImage; // extract JAR if docker image specified
    private String jarPath;
    private String organizationPath;
    private String serviceDNS; // use service name instead of localhost for endpoints
    private String securityAnalyzerInterface;
}