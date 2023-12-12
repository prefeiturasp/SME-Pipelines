pipeline {
    environment {
      branchname =  env.BRANCH_NAME.toLowerCase()
      kubeconfig = getKubeconf(env.branchname)
      registryCredential = 'jenkins_registry'
      namespace = "${env.branchname == 'pre-prod' ? 'sme-novosgp-d1' : env.branchname == 'development' ? 'novosgp-dev' : 'sme-novosgp' }"  
    }
  
    agent {
      kubernetes { label 'builder' }
    }

    options {
      buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '20'))
      disableConcurrentBuilds()
      skipDefaultCheckout()
    }
  
    stages {

          stage('Build') {
            agent { kubernetes { 
                  label 'builder'
                  defaultContainer 'builder'
                }
              }
          when { anyOf { branch 'master'; branch 'main'; branch 'release'; } } 
          steps {
            checkout scm 
            script {
              imagename1 = "registry.sme.prefeitura.sp.gov.br/${env.branchname}/sgp-docs"
              dockerImage1 = docker.build(imagename1, "-f Dockerfile .")
              docker.withRegistry( 'https://registry.sme.prefeitura.sp.gov.br', registryCredential ) {
              dockerImage1.push()
              }
              sh "docker rmi $imagename1"
            }
          }
        }

        stage('Deploy'){
             agent { kubernetes { 
                  label 'builder'
                  defaultContainer 'builder'
                }
              }
            when { anyOf {  branch 'master'; branch 'main'; branch 'release'; } }        
            steps {
                script{
                    withCredentials([file(credentialsId: "${kubeconfig}", variable: 'config')]){
                       sh 'if [ -d "$home/.kube/config" ]; then rm -f "$home/.kube/config"; fi'
                       sh('cp $config '+"$home"+'/.kube/config')
                       sh "kubectl rollout restart deployment/sme-sgp-docs -n ${namespace}"						
                       sh('rm -f '+"$home"+'/.kube/config')
                    }
                }
            }           
        }    
    }
}

def getKubeconf(branchName) {
    if("main".equals(branchName)) { return "config_prd"; }
    else if ("master".equals(branchName)) { return "config_prd"; }
    else if ("homolog".equals(branchName)) { return "config_release"; }
    else if ("release".equals(branchName)) { return "config_release"; }
    else if ("release2".equals(branchName)) { return "config_release"; }
    else if ("development".equals(branchName)) { return "config_release"; }
    else if ("develop".equals(branchName)) { return "config_release"; }
}
