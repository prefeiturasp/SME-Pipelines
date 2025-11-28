#!/usr/bin/env groovy

def call(Map stageParams) {

    def dockerfilePath = stageParams.dockerfilePath
    def imageName = stageParams.imageName

    echo "Executando Dockerfile check sintaxe"
    sh "docker build -f ${dockerfilePath} --check ."
    
    echo "Executando Dockerfile check build"
    dockerTest = docker.build(imageName, "-f ${dockerfilePath} .")
    sh "docker rmi ${imageName}"
}