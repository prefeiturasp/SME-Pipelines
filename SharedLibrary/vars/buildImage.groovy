#!/usr/bin/env groovy

def call(Map images) {

    def builds = [:]

    images.each { imageName, config ->

        builds[imageName] = {

            def fullImageName = "${env.registryUrl}/${env.BRANCH_NAME}/${imageName}"
            def dockerImage = docker.build(fullImageName, "-f ${config.dockerfilePath} .")

            if (config.sendRegistry == "yes") {
                docker.withRegistry("https://${env.registryUrl}", registryCredential) {
                    dockerImage.push()
                }
            }

            sh "docker rmi ${fullImageName}"
        }
    }

    return builds
}