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
        // essa tag nao e publicada, entao usamos o commit (TAG1). Quando existe
        // releaseTag (publicada pelo stage "Publica release", so em master),
        // ela tem prioridade sobre o commit.
        def targetBranch = env.branchname?.toLowerCase()
        def branchesOnlyCommitAndLatest = ['homolog', 'master']
        def releaseTag = env.RELEASE_TAG?.trim()
        def hasReleaseTag = releaseTag as boolean
        def imageTag = (targetBranch in branchesOnlyCommitAndLatest) ? (hasReleaseTag ? releaseTag : env.TAG1) : env.TAG2
        echo "releaseTag: ${releaseTag}"
        echo "imageTag: ${imageTag}"

        def newImage = "${fullImageName}:${imageTag}"

        sh """
            [ -f "\$HOME/.kube/config" ] && rm -f "\$HOME/.kube/config"
            mkdir -p "\$HOME/.kube"
            cp "\$config" "\$HOME/.kube/config"

            export KUBECONFIG="\$HOME/.kube/config"

            currentImage=\$(kubectl get deployment/${stageParams.deploymentName} \
                -n ${stageParams.namespace} \
                -o jsonpath='{.spec.template.spec.containers[?(@.name=="${stageParams.containerName}")].image}')
            newImage="${newImage}"

            echo "currentImage: \$currentImage"
            echo "newImage: \$newImage"

            if [ "\$currentImage" = "\$newImage" ]; then
                echo "Imagem sem alteracao, executando rollout restart..."
                kubectl rollout restart deployment/${stageParams.deploymentName} -n ${stageParams.namespace}
            else
                kubectl set image deployment/${stageParams.deploymentName} \
                    ${stageParams.containerName}=\$newImage \
                    -n ${stageParams.namespace}
            fi
        """
    }
}