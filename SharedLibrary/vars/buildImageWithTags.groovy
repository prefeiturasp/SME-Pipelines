#!/usr/bin/env groovy

def call(Map stageParams) {

    def imageName = stageParams.imageName
    
    def fullImageName = ""
    def commitHash = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
    env.TAG = "${commitHash}"

    withCredentials([string(credentialsId: "${env.registryUrl}", variable: 'registryUrl')]) {
        docker.withRegistry("https://${registryUrl}", env.registryCredential) {
            
            if (imageName?.trim()) {
                fullImageName = "${registryUrl}/${env.project}/${env.branchname}/${stageParams.imageName}"
            } else {
                fullImageName = "${registryUrl}/${env.project}/${env.branchname}"
            }
            
            def dockerImage = docker.build(fullImageName, "-f ${stageParams.dockerfilePath} .")

            if (stageParams.sendRegistry == "yes") {
                dockerImage.push("${TAG}")
                dockerImage.push()
            }
        }
    }

    sh "docker rmi ${fullImageName}"
    sh "docker rmi ${fullImageName}:${TAG}"
}