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

    // Quando getSourceBranch() nao encontra a branch de origem (ex.: mensagem de
    // commit fora do padrao), TAG2 cai no fallback e fica igual a TAG1. Nesse caso
    // as operacoes de TAG2 seriam repetidas com o mesmo valor de TAG1, e o segundo
    // "docker rmi" falha porque a imagem ja foi removida.
    def hasDistinctBranchTag = applyBranchTag && env.TAG2 != env.TAG1

    // Tag de release (ex.: v1.0.0), publicada pelo stage "Publica release" a
    // partir da version do pyproject.toml. So existe para builds de master.
    def releaseTag = env.RELEASE_TAG?.trim()
    def hasReleaseTag = releaseTag as boolean

    echo "TAG1: ${env.TAG1}"
    echo "TAG2: ${env.TAG2}"
    echo "applyBranchTag: ${applyBranchTag}"
    echo "hasDistinctBranchTag: ${hasDistinctBranchTag}"
    echo "releaseTag: ${releaseTag}"


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
            if (hasDistinctBranchTag) {
                sh "docker tag ${fullImageName} ${fullImageName}:${TAG2}"
            }
            if (hasReleaseTag) {
                sh "docker tag ${fullImageName} ${fullImageName}:${releaseTag}"
            }

            if (stageParams.sendRegistry == "yes") {
                sh "docker push ${fullImageName}:${TAG1}"
                if (hasDistinctBranchTag) {
                    sh "docker push ${fullImageName}:${TAG2}"
                }
                if (hasReleaseTag) {
                    sh "docker push ${fullImageName}:${releaseTag}"
                }
                sh "docker push ${fullImageName}"
            }
        }
    }

    sh "docker rmi ${fullImageName} || true"
    sh "docker rmi ${fullImageName}:${TAG1} || true"
    if (hasDistinctBranchTag) {
        sh "docker rmi ${fullImageName}:${TAG2} || true"
    }
    if (hasReleaseTag) {
        sh "docker rmi ${fullImageName}:${releaseTag} || true"
    }
}