#!/usr/bin/env groovy

def call(Map stageParams) {
    
    withCredentials([
        file(credentialsId: "${env.kubeconfig}", variable: 'config'),
        string(credentialsId: "${env.registryUrl}", variable: 'registryUrl')
    ]){
        config()

        if (TAG?.trim()) {
            setImage()
        } else {
            rollout()
        }
    }

    def config = {
        sh """
            [ -f "\$HOME/.kube/config" ] && rm -f "\$HOME/.kube/config"
            mkdir -p "\$HOME/.kube"
            cp "\$config" "\$HOME/.kube/config"
        """
    }
    
    def rollout = {
        sh """
            export KUBECONFIG="\$HOME/.kube/config"
            kubectl rollout restart deployment/${stageParams.deploymentName} -n ${stageParams.namespace}
        """
    }

    def setImage = {
        sh """
            export KUBECONFIG="\$HOME/.kube/config"
            kubectl set image deployment/${stageParams.deploymentName} \
                ${stageParams.deploymentName}=${env.registryUrl}/${project}/${branchname}:${TAG} \
                -n ${stageParams.namespace}
        """
    }
}