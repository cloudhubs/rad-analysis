package edu.baylor.ecs.cloudhubs.radanalysis.service;

import com.google.common.reflect.TypeToken;
import com.squareup.okhttp.Call;
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
import io.kubernetes.client.util.Watch;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
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

    public static void watchServices() throws IOException, ApiException {
        log.info("Starting k8s service watcher");

        ApiClient client = Config.defaultClient();
        client.getHttpClient().setReadTimeout(0, TimeUnit.SECONDS); // infinite timeout
        Configuration.setDefaultApiClient(client);

        CoreV1Api api = new CoreV1Api();
        String namespace = "default";

        Call serviceWatchCall = api.listNamespacedServiceCall(namespace, null, null, null, null, null, 5, null, null, Boolean.TRUE, null, null);
        Type serviceWatchType = new TypeToken<Watch.Response<V1Service>>() {
        }.getType();

        try (Watch<V1Service> watch = Watch.createWatch(client, serviceWatchCall, serviceWatchType)) {
            for (Watch.Response<V1Service> item : watch) {
                log.info(String.format("%s : Service %s", item.type, item.object.getMetadata().getName()));
            }
        }
    }
}
