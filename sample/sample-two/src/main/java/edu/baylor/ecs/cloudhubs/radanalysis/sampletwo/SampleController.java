package edu.baylor.ecs.cloudhubs.radanalysis.sampletwo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.RolesAllowed;

@RestController
@RequestMapping("/sample-two")
public class SampleController {
    @RolesAllowed("admin")
    @GetMapping("/p1")
    public SampleModel path1() {
        return new SampleModel(1L, "path01");
    }

    @RolesAllowed("admin")
    @GetMapping("/p2")
    public SampleModel path2() {
        return new SampleModel(2L, "path02");
    }

    @RolesAllowed("admin")
    @GetMapping("/p3")
    public SampleModel path3() {
        return new SampleModel(3L, "path03");
    }

}
