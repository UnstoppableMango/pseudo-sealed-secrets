/*
 * Copyright Java Operator SDK Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.unmango;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.operator.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Runner {

    private static final Logger log = LoggerFactory.getLogger(Runner.class);

    public static void main(String[] args) {
        KubernetesClient client = new KubernetesClientBuilder().build();
        if (args.length > 0 && args[0].equals("bootstrap")) {
            runBootstrap(client);
        } else {
            runOperator(client);
        }
    }

    private static void runBootstrap(KubernetesClient client) {
        String namespace = client.getConfiguration().getNamespace();
        log.info("Bootstrapping keypair in namespace {}", namespace);
        new KeypairBootstrapper(client).bootstrap(namespace);
    }

    private static void runOperator(KubernetesClient client) {
        String namespace = client.getConfiguration().getNamespace();

        var sealService = new SealService(client, namespace);
        var apiServer = new ApiServer(sealService);
        try {
            apiServer.start();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to start API server", e);
        }

        Operator operator = new Operator();
        operator.register(new SealedSecretReconciler());
        operator.start();
        log.info("Operator started.");
    }
}
