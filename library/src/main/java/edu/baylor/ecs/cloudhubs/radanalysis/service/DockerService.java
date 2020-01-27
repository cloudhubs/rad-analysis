package edu.baylor.ecs.cloudhubs.radanalysis.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class DockerService {

    public void runExtractScript(String dockerImage) throws InterruptedException, IOException {
        List<String> cmdList = new ArrayList<>();
        cmdList.add("sh");
        cmdList.add("/extract.sh");
        cmdList.add(dockerImage);

        ProcessBuilder pb = new ProcessBuilder(cmdList);
        Process p = pb.start();
        p.waitFor();

        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
    }
}
