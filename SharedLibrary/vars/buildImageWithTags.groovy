#!/usr/bin/env groovy

def call(Map stageParams) {

    def imageName = stageParams.imageName
    
    def fullImageName = ""
    def commitHash = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
    env.SOURCE_BRANCH = getSourceBranch() ?: ''
    
    // Nos merges test > homolog > master o getSourceBranch() retorna a branch
    // anterior da cadeia (test/homolog), gerando tags erradas tipo :test ou :homolog.
    // Para homolog e master aplicamos somente commit + latest.
    def targetBranch = env.branchname?.toLowerCase()
    def branchesOnlyCommitAndLatest = ['homolog', 'master']
    def applyBranchTag = !(targetBranch in branchesOnlyCommitAndLatest)

    env.TAG1 = "${commitHash}"
    def branchTag = env.SOURCE_BRANCH?.toLowerCase()?.replaceAll("\\s", "-")?.replace("/", "-")?.replaceAll("[^a-z0-9_.-]", "")?.take(128)
    env.TAG2 = branchTag ?: env.TAG1

    echo "TAG1: ${env.TAG1}"
    echo "TAG2: ${env.TAG2}"
    echo "applyBranchTag: ${applyBranchTag}"


    withCredentials([string(credentialsId: "${env.registryUrl}", variable: 'registryUrl')]) {
        docker.withRegistry("https://${registryUrl}", env.registryCredential) {
            
            
            if (imageName?.trim() && env.project?.trim()) {
                fullImageName = "${registryUrl}/${env.project}/${env.branchname}/${stageParams.imageName}"
            } else {
                fullImageName = "${registryUrl}/${env.project}/${env.branchname}"
            }
            
            sh """
                docker build \
                    --cache-from ${fullImageName} \
                    -t ${fullImageName} \
                    -f ${stageParams.dockerfilePath} .
            """

            sh "docker tag ${fullImageName} ${fullImageName}:${TAG1}"
            if (applyBranchTag) {
                sh "docker tag ${fullImageName} ${fullImageName}:${TAG2}"
            }

            if (stageParams.sendRegistry == "yes") {
                sh "docker push ${fullImageName}:${TAG1}"
                if (applyBranchTag) {
                    sh "docker push ${fullImageName}:${TAG2}"
                }
                sh "docker push ${fullImageName}"
            }
        }
    }

    sh "docker rmi ${fullImageName}"
    sh "docker rmi ${fullImageName}:${TAG1}"
    if (applyBranchTag) {
        sh "docker rmi ${fullImageName}:${TAG2}"
    }
}