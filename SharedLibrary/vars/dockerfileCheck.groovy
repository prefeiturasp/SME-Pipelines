#!/usr/bin/env groovy
def call(Map stageParams) {
    withCredentials([string(credentialsId: "${env.registryUrl}", variable: 'registryUrl')]) {
        docker.withRegistry("https://${registryUrl}", env.registryCredential) {
            sh "docker rmi ${stageParams.imageName} || true"
            
            echo "Executando Dockerfile check sintaxe"
            sh "docker build -f ${stageParams.dockerfilePath} --check ."
            
            echo "Executando Dockerfile check build - Com log detalhado"
            sh "docker build -f ${stageParams.dockerfilePath} -t ${stageParams.imageName} --progress=plain ."

            sh "docker rmi ${stageParams.imageName}"
        }
    }
}