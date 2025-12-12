#!/usr/bin/env groovy
def call(Map stageParams) {

    echo "Executando Dockerfile check sintaxe"
    sh "docker build -f ${stageParams.dockerfilePath} --check ."
    
    echo "Executando Dockerfile check build"
    docker.build(stageParams.imageName, "-f ${stageParams.dockerfilePath} .")

    sh "docker rmi ${stageParams.imageName}"
}