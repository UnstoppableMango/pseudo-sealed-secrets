package dev.unmango;

import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

@ControllerConfiguration
public class SealedSecretReconciler implements Reconciler<Secret> {

    public static final String TARGET_ANNOTATION = "pseudo-sealed-secrets.unmango.dev/target";

    @Override
    public UpdateControl<Secret> reconcile(Secret resource, Context<Secret> context) {
        var annotations = resource.getMetadata().getAnnotations();
        if (annotations == null || !annotations.containsKey(TARGET_ANNOTATION)) {
            return UpdateControl.noUpdate();
        }
        // TODO: load private key, decrypt data entries, create target Secret
        return UpdateControl.noUpdate();
    }
}
