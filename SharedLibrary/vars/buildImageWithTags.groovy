#!/usr/bin/env groovy

def call(Map stageParams) {

    def fullImageName = ""
    def commitHash = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
    env.TAG = "${commitHash}"

    withCredentials([string(credentialsId: "${env.registryUrl}", variable: 'registryUrl')]) {
        
        fullImageName = "${registryUrl}/${env.project}/${env.BRANCH_NAME}/${stageParams.imageName}"
        def dockerImage = docker.build(fullImageName, "-f ${stageParams.dockerfilePath} .")

        if (stageParams.sendRegistry == "yes") {
            docker.withRegistry("https://${registryUrl}", env.registryCredential) {
                dockerImage.push("${TAG}")
                dockerImage.push()
            }
        }
    }

    sh "docker rmi ${fullImageName}"
    sh "docker rmi ${fullImageName}:${TAG}"
}