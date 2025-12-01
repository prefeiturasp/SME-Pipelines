#!/usr/bin/env groovy

def call(Map images) {

    def builds = [:]

    images.each { imageName, config ->

        builds[imageName] = {
            imageBuildName = "registry.sme.prefeitura.sp.gov.br/${env.branchname}/${imageName}"
            dockerImage = docker.build(imageBuildName, "-f ${config.dockerfilePath} .")
            docker.withRegistry( 'https://registry.sme.prefeitura.sp.gov.br', registryCredential ) {
                dockerImage.push()
            }
            sh "docker rmi $imageBuildName"
        }
    }
}