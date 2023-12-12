pipeline {
    environment {
      branchname =  env.BRANCH_NAME.toLowerCase()
      kubeconfig = getKubeconf(env.branchname)
      registryCredential = 'jenkins_registry'
      namespace = "${env.branchname == 'pre-prod' ? 'sme-ces-d1' : env.branchname == 'development' ? 'ces-dev' : env.branchname == 'develop' ? 'ces-dev' : env.branchname == 'homolog' ? 'ces-hom' : 'sme-ces' }"
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

        stage('AnaliseCodigo') {
          when { branch 'homolog' }
          steps {
              withSonarQubeEnv('sonarqube-local'){
                sh 'echo "[ INFO ] Iniciando analise Sonar..." && sonar-scanner \
                -Dsonar.projectKey=SME-BensFisicos-BackEnd \
                -Dsonar.sources=.'
            }
          }
        }        

        stage('Build') {
        
          when { anyOf { branch 'master'; branch 'main'; branch "story/*"; branch 'development'; branch 'develop'; branch 'release'; branch 'homolog';  } } 
          steps {
            script {
              imagename = "registry.sme.prefeitura.sp.gov.br/${env.branchname}/sme-ces-back"
              dockerImage1 = docker.build(imagename, "-f Dockerfile .")
              docker.withRegistry( 'https://registry.sme.prefeitura.sp.gov.br', registryCredential ) {
              dockerImage1.push()
              }
              sh "docker rmi $imagename"
            }
          }
        }
        
        stage('Deploy'){
            when { anyOf {  branch 'master'; branch 'main'; branch 'develop'; branch 'development'; branch 'release'; branch 'homolog';  } }        
            steps {
                script{                        
                    withCredentials([file(credentialsId: "${kubeconfig}", variable: 'config')]){
                        sh('cp $config '+"$home"+'/.kube/config')
                        sh 'kubectl rollout restart deployment/ces-backend -n ${namespace}'                            
                    }                    
                }
            }           
        } 

    }
}

//test
def getKubeconf(branchName) {
    if("main".equals(branchName)) { return "config_prd"; }
    else if ("master".equals(branchName)) { return "config_prd"; }
    else if ("pre-prod".equals(branchName)) { return "config_prd"; }
    else if ("homolog".equals(branchName)) { return "config_release"; }
    else if ("release".equals(branchName)) { return "config_release"; }
    else if ("release-r2".equals(branchName)) { return "config_hom"; }
    else if ("development".equals(branchName)) { return "config_release"; }
    else if ("develop".equals(branchName)) { return "config_release"; }
}
