package edu.baylor.ecs.cloudhubs.radanalysis.context.Deployed;

import edu.baylor.ecs.cloudhubs.rad.model.RestEntity;
import edu.baylor.ecs.seer.common.context.SeerSecurityContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DiscreteResponseContext {
    private String resourcePath;
    private List<RestEntity> restEntities;
    private SeerSecurityContext security;
}
