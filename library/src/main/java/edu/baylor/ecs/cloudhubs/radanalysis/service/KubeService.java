package edu.baylor.ecs.cloudhubs.radanalysis.service;

import edu.baylor.ecs.cloudhubs.radanalysis.context.Deployed.KubeArtifact;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodList;
import io.kubernetes.client.models.V1Service;
import io.kubernetes.client.models.V1ServiceList;
import io.kubernetes.client.util.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KubeService {

    public List<KubeArtifact> getDeployedArtifacts() throws ApiException, IOException {
        List<KubeArtifact> kubeArtifacts = new ArrayList<>();

        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);

        CoreV1Api api = new CoreV1Api();
        String namespace = "default";

        V1ServiceList svcList = api.listNamespacedService(
                namespace, null, null, null, null,
                null, null, null, null, null);


        for (V1Service service : svcList.getItems()) {
            // ignore builtin k8s service
            if (service.getMetadata().getName().equals("kubernetes") || service.getMetadata().getName().equals("rad-analysis")) {
                continue;
            }

            String labelSelector = getLabelSelector(service.getSpec().getSelector());

            V1PodList podList = api.listNamespacedPod(namespace, null, null, null, null,
                    labelSelector, null, null, null, null);

            for (V1Pod pod : podList.getItems()) {
                kubeArtifacts.add(new KubeArtifact(
                        service.getMetadata().getName(),
                        pod.getMetadata().getName(),
                        pod.getSpec().getContainers().get(0).getImage())
                );
            }
        }

        return kubeArtifacts;
    }

    private String getLabelSelector(Map<String, String> selectors) {
        if (selectors == null) return null;

        return selectors.keySet().stream()
                .map(key -> key + "=" + selectors.get(key))
                .collect(Collectors.joining(","));
    }
}
