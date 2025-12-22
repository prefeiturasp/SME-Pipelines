#!/usr/bin/env groovy

def call(Map stageParams) {

    sh """
        [ -f "\$HOME/.kube/config" ] && rm -f "\$HOME/.kube/config"

        mkdir -p "\$HOME/.kube"
        cp "\$config" "\$HOME/.kube/config"
        export KUBECONFIG="\$HOME/.kube/config"

        kubectl rollout restart deployment/${stageParams.deploymentName} -n ${stageParams.namespace}
    """
}