#!/usr/bin/env groovy

def call(Map deployments) {

    def restarts = [:]

    deployments.each { deploymentName, config ->

        restarts[deploymentName] = {

            sh """
                [ -f "\$HOME/.kube/config" ] && rm -f "\$HOME/.kube/config"

                mkdir -p "\$HOME/.kube"
                cp "\$config" "\$HOME/.kube/config"
                export KUBECONFIG="\$HOME/.kube/config"

                kubectl rollout restart deployment/${deploymentName} -n ${config.namespace}
            """
        }
    }

    return restarts
}