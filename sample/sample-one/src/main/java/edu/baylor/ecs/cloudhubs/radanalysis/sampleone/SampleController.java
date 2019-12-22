package edu.baylor.ecs.cloudhubs.radanalysis.sampleone;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.RolesAllowed;

@RestController
@AllArgsConstructor
@RequestMapping("/sample-one")
public class SampleController {
    private final SampleService sampleService;

    @GetMapping("/p1")
    public SampleModel path1() {
        return sampleService.path1();
    }

    @GetMapping("/p2")
    public SampleModel path2() {
        return sampleService.path2();
    }

    @RolesAllowed("user")
    @GetMapping("/p3")
    public SampleModel path3() {
        return sampleService.path3();
    }

}
