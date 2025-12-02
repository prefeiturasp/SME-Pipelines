#!/usr/bin/env groovy
def call(Map images) {

    def checks = [:]

    images.each { imageName, config ->

        checks[imageName] = {

            def dockerfilePath = config.dockerfilePath

            echo "Executando Dockerfile check sintaxe"
            sh "docker build -f ${config.dockerfilePath} --check ."
            
            echo "Executando Dockerfile check build"
            docker.build(imageName, "-f ${config.dockerfilePath} .")

            sh "docker rmi ${imageName}"
        }
    }

    return checks
}