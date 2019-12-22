package edu.baylor.ecs.cloudhubs.radanalysis.sampleone;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sample-one")
public class SampleController {
    @GetMapping("/p1")
    public SampleModel path1() {
        return new SampleModel();
    }

    @GetMapping("/p2")
    public SampleModel path2() {
        return new SampleModel();
    }

    @GetMapping("/p3")
    public SampleModel path3() {
        return new SampleModel();
    }

}
