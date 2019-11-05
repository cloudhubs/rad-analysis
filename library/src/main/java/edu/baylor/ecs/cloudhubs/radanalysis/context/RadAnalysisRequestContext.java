package edu.baylor.ecs.cloudhubs.radanalysis.context;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class RadAnalysisRequestContext {
    private String pathToCompiledMicroservices;
    private String organizationPath;
    private String outputPath;
    private String securityAnalyzerInterface;
}