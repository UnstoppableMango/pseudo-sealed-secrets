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

import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.operator.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class Runner {

    private static final Logger log = LoggerFactory.getLogger(Runner.class);
    private static final Path SA_NAMESPACE_FILE =
            Path.of("/var/run/secrets/kubernetes.io/serviceaccount/namespace");

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("bootstrap")) {
            runBootstrap();
        } else {
            runOperator();
        }
    }

    private static void runBootstrap() {
        var client = new KubernetesClientBuilder().build();
        String namespace = resolveNamespace();
        log.info("Bootstrapping keypair in namespace {}", namespace);
        new KeypairBootstrapper(client).bootstrap(namespace);
    }

    private static void runOperator() {
        Operator operator = new Operator();
        operator.register(new PseudoSealedSecretsReconciler());
        operator.start();
        log.info("Operator started.");
    }

    static String resolveNamespace() {
        String env = System.getenv("POD_NAMESPACE");
        if (env != null && !env.isBlank()) return env;

        if (Files.exists(SA_NAMESPACE_FILE)) {
            try {
                return Files.readString(SA_NAMESPACE_FILE).trim();
            } catch (IOException e) {
                log.warn("Could not read namespace from {}", SA_NAMESPACE_FILE, e);
            }
        }

        return "default";
    }
}
