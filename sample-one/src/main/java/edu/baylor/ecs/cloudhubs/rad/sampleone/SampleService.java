package edu.baylor.ecs.cloudhubs.rad.sampleone;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SampleService {
    private final RestTemplate restTemplate;
    private final String url = "http://localhost:8070/sample-two";

    public SampleService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public SampleModel path1() {
        return restTemplate.getForObject(url + "/p1", SampleModel.class);
    }

    public SampleModel path2() {
        return restTemplate.getForObject(url + "/p2", SampleModel.class);
    }

    public SampleModel path3() {
        return restTemplate.getForObject(url + "/p3", SampleModel.class);
    }
}