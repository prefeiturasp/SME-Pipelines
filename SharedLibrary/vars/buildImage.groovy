#!/usr/bin/env groovy

def call(Map stageParams) {

    def fullImageName = ""
    withCredentials([string(credentialsId: "${env.registryUrl}", variable: 'registryUrl')]) {
        fullImageName = "${registryUrl}/${env.BRANCH_NAME}/${stageParams.imageName}"
        def dockerImage = docker.build(fullImageName, "-f ${stageParams.dockerfilePath} .")

        if (config.sendRegistry == "yes") {
            docker.withRegistry("https://${registryUrl}", env.registryCredential) {
                dockerImage.push()
            }
        }
    }

    sh "docker rmi ${fullImageName}"
}