#!/usr/bin/env groovy

def call(Map stageParams) {

    def valuesFile = stageParams.valuesFile
    def replicas = stageParams.replicas
    def ambiente = stageParams.ambiente

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
                --set replicaCount=${replicas} \
                --values ${valuesFile} \
                --install \
                --history-max 3 \
                --timeout 1m0s \
                --atomic \
                #--debug \
                --wait
        """
    }
}