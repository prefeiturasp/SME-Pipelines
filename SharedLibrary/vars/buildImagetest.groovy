#!/usr/bin/env groovy

def call(Map stageParams) {

    def fullImageName = ""

    withCredentials([string(credentialsId: "${env.registryUrl}", variable: 'registryUrl')]) {
        docker.withRegistry("https://${registryUrl}", env.registryCredential) {
            
            if (env.project?.trim()) {
                fullImageName = "${registryUrl}/${env.project}/${env.branchname}/${stageParams.imageName}"
            } else {
                fullImageName = "${registryUrl}/${env.branchname}/${stageParams.imageName}"
            }
            
            def dockerImage = docker.build(fullImageName, "-f ${stageParams.dockerfilePath} .")

            if (stageParams.sendRegistry == "yes") {
                dockerImage.push()
            }
        }
    }

    sh "docker rmi ${fullImageName}"
}