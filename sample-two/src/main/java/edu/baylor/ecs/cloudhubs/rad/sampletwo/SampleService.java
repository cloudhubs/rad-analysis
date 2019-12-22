package edu.baylor.ecs.cloudhubs.rad.sampletwo;

import org.springframework.stereotype.Service;

@Service
public class SampleService {

    public SampleModel path1() {
        return new SampleModel();
    }

    public SampleModel path2() {
        return new SampleModel();
    }

    public SampleModel path3() {
        return new SampleModel();
    }
}
