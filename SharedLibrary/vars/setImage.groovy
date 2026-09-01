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

        // Para test usamos a tag com o nome da branch (TAG2); para homolog/master
        // essa tag nao e publicada, entao usamos o commit (TAG1).
        def targetBranch = env.branchname?.toLowerCase()
        def branchesOnlyCommitAndLatest = ['homolog', 'master']
        def imageTag = (targetBranch in branchesOnlyCommitAndLatest) ? env.TAG1 : env.TAG2
        echo "imageTag: ${imageTag}"

        sh """
            [ -f "\$HOME/.kube/config" ] && rm -f "\$HOME/.kube/config"
            mkdir -p "\$HOME/.kube"
            cp "\$config" "\$HOME/.kube/config"
            
            export KUBECONFIG="\$HOME/.kube/config"
            kubectl set image deployment/${stageParams.deploymentName} \
                ${stageParams.containerName}=${fullImageName}:${imageTag} \
                -n ${stageParams.namespace}
        """
    }
}