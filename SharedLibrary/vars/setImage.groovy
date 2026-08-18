#!/usr/bin/env groovy

def call(Map stageParams) {
    
    withCredentials([
        file(credentialsId: "${env.kubeconfig}", variable: 'config'),
        string(credentialsId: "${env.registryUrl}", variable: 'registryUrl')
    ]){
        
        def fullImageName = ""
        if (stageParams.imageName?.trim() && env.project?.trim()) {
            fullImageName = "${registryUrl}/${env.project}/${env.branchname}/${stageParams.imageName}"
        } else {
            fullImageName = "${registryUrl}/${env.branchname}/${stageParams.imageName}"
        }
        
        sh """
            [ -f "\$HOME/.kube/config" ] && rm -f "\$HOME/.kube/config"
            mkdir -p "\$HOME/.kube"
            cp "\$config" "\$HOME/.kube/config"
            
            export KUBECONFIG="\$HOME/.kube/config"
            kubectl set image deployment/${stageParams.deploymentName} \
                ${stageParams.containerName}=${fullImageName}:${env.TAG1} \
                -n ${stageParams.namespace}
        """
    }
}