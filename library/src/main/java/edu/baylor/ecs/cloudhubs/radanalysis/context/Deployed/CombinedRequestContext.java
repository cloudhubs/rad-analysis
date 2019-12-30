package edu.baylor.ecs.cloudhubs.radanalysis.context.Deployed;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class CombinedRequestContext {
    private String securityAnalyzerInterface;
    private List<DiscreteResponseContext> restContexts;
}