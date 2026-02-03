#!/usr/bin/env groovy

def call(Map stageParams) {

    withCredentials([
        file(credentialsId: "${env.kubeconfig}", variable: 'config'),
        ]){
        
        sh """
            [ -f "\$HOME/.kube/config" ] && rm -f "\$HOME/.kube/config"

            mkdir -p "\$HOME/.kube"
            cp "\$config" "\$HOME/.kube/config"
            export KUBECONFIG="\$HOME/.kube/config"

            cd "${helmDir}"

            helm upgrade ${project} . \
                --namespace ${ambiente}-${project} \
                --set replicaCount=${stageParams.replicas} \
                --values ${stageParams.valuesFile} \
                --install \
                --history-max 3 \
                --timeout 1m0s \
                --atomic \
                #--debug \
                --wait
        """
    }
}