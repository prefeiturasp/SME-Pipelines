pipeline {
    environment {
      branchname =  env.BRANCH_NAME.toLowerCase()
      registryCredential = 'jenkins_registry'
    }
  
	  agent { kubernetes { 
              label 'builder'
              defaultContainer 'builder'
            }
          }

    options {
      buildDiscarder(logRotator(numToKeepStr: '15', artifactNumToKeepStr: '15'))
      disableConcurrentBuilds()
      skipDefaultCheckout()
    }
  
    stages {

        stage('CheckOut') {            
            steps { checkout scm }            
        }

        stage('Build') {
          when { anyOf { branch 'main'; branch 'homolog'; branch 'php-fpm-hom'; } } 
          steps {
            script {
              imagename1 = "registry.sme.prefeitura.sp.gov.br/wordpress/${env.branchname}/visitasmonitoradas"
              dockerImage1 = docker.build(imagename1, "-f Dockerfile .")
              docker.withRegistry( 'https://registry.sme.prefeitura.sp.gov.br', registryCredential ) {
              dockerImage1.push()
              }
              sh "docker rmi $imagename1"
            }
          }
        }
        
        stage('Deploy Prod'){
            when { anyOf {  branch 'main'; } }        
            steps {
                script{
                    if ( env.branchname == 'main' ) {
                        withCredentials([string(credentialsId: 'aprovadores-wordpress', variable: 'aprovadores')]) {
                            timeout(time: 24, unit: "HOURS") {
                                input message: 'Deseja realizar o deploy?', ok: 'SIM', submitter: "${aprovadores}"
                            }
                        }
                    }                    
                    withCredentials([file(credentialsId: 'config_wordpress', variable: 'config')]){
                        sh('if [ -f '+"$home"+'/.kube/config ];then rm -f '+"$home"+'/.kube/config; fi')
                        sh('cp $config '+"$home"+'/.kube/config')
                        sh 'kubectl rollout restart deployment/prod-visitasmonitoradas -n prod-visitasmonitoradas'
                        sh('if [ -f '+"$home"+'/.kube/config ];then rm -f '+"$home"+'/.kube/config; fi')
                   }
                }
            }           
        }

        stage('Deploy Hom'){
            when { anyOf {  branch 'homolog'; branch 'php-fpm-hom'; } }        
            steps {
                script{                 
                    withCredentials([file(credentialsId: 'config_wordpress', variable: 'config')]){
                        sh('if [ -f '+"$home"+'/.kube/config ];then rm -f '+"$home"+'/.kube/config; fi')
                        sh('cp $config '+"$home"+'/.kube/config')
                        sh 'kubectl rollout restart deployment/hom-visitasmonitoradas -n hom-visitasmonitoradas'
                        sh('if [ -f '+"$home"+'/.kube/config ];then rm -f '+"$home"+'/.kube/config; fi')
                   }
                }
            }           
        }     
    }

  post {
    always { sh('if [ -f '+"$home"+'/.kube/config ];then rm -f '+"$home"+'/.kube/config; fi') }
  }
}
