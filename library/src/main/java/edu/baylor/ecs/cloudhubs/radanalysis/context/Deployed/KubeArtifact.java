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
public class KubeArtifact {
    private String serviceName;
    private String podName;
    private String imageName;
}