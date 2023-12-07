pipeline {
    environment {
      branchname =  env.BRANCH_NAME.toLowerCase()
      kubeconfig = getKubeconf(env.branchname)
      registryCredential = 'jenkins_registry'
      namespace = "${env.branchname == 'develop' ? 'cadastroinfantil-dev' : env.branchname == 'homolog' ? 'cadastroinfantil-hom' : env.branchname == 'homolog-r2' ? 'cadastroinfantil-hom2' : 'sme-cadastro-infantil' }"
    }
  
    agent { kubernetes { 
                  label 'builder'
                  defaultContainer 'builder'
                }
              } 

    options {
      buildDiscarder(logRotator(numToKeepStr: '5', artifactNumToKeepStr: '5'))
      disableConcurrentBuilds()
      skipDefaultCheckout()
    }
  
    stages {

        stage('CheckOut') {            
            steps { checkout scm }            
        }

        stage('AnaliseCodigo') {
	      when { branch 'homolog' }
             agent { kubernetes { 
                  label 'python36'
                  defaultContainer 'builder'
                }
              } 
          steps {
              withSonarQubeEnv('sonarqube-local'){
                sh 'echo "[ INFO ] Iniciando analise Sonar..." && sonar-scanner \
                -Dsonar.projectKey=SME-CadastroInfantil-FrontEnd \
                -Dsonar.sources=.'
            }
          }
        }

        stage('Build') {
          when { anyOf { branch 'master'; branch 'main'; branch "story/*"; branch 'development'; branch 'develop'; branch 'release'; branch 'homolog';  } } 
          steps {
            script {
              imagename1 = "registry.sme.prefeitura.sp.gov.br/${env.branchname}/cadastro-infantil-backend"
              dockerImage1 = docker.build(imagename1, "-f Dockerfile .")
              docker.withRegistry( 'https://registry.sme.prefeitura.sp.gov.br', registryCredential ) {
              dockerImage1.push()
              }
              sh "docker rmi $imagename1"
            }
          }
        }
	    
        stage('Deploy'){
            when { anyOf {  branch 'master'; branch 'main'; branch 'development'; branch 'develop'; branch 'release'; branch 'homolog';  } }        
            steps {
                script{
                    if ( env.branchname == 'main' ||  env.branchname == 'master' ) {
                        sendTelegram("🤩 [Deploy ${env.branchname}] Job Name: ${JOB_NAME} \nBuild: ${BUILD_DISPLAY_NAME} \nMe aprove! \nLog: \n${env.BUILD_URL}")
			withCredentials([string(credentialsId: 'aprovadores-sigpae', variable: 'aprovadores')]) {
                          timeout(time: 24, unit: "HOURS") {
                              input message: 'Deseja realizar o deploy?', ok: 'SIM', submitter: "${aprovadores}"
                          }
			}
                    }
                    if ( env.branchname == 'homolog' || env.branchname == 'release' ) {
                        withCredentials([file(credentialsId: "${kubeconfig}", variable: 'config')]){
			    sh('rm -f '+"$home"+'/.kube/config')
                            sh('cp $config '+"$home"+'/.kube/config')
                            sh 'kubectl rollout restart deployment/cadastro-infantil-backend -n sme-cadastro-infantil'
                            sh 'kubectl rollout restart deployment/cadastro-infantil-celery -n sme-cadastro-infantil'
                            sh 'kubectl rollout restart deployment/cadastro-infantil-flower -n sme-cadastro-infantil'
                            sh('rm -f '+"$home"+'/.kube/config')
                        }
                    }
                    withCredentials([file(credentialsId: "${kubeconfig}", variable: 'config')]){
			    sh('rm -f '+"$home"+'/.kube/config')
                            sh('cp $config '+"$home"+'/.kube/config')
                            sh "kubectl rollout restart deployment/cadastro-infantil-backend -n ${namespace}"
                            sh "kubectl rollout restart deployment/cadastro-infantil-celery -n ${namespace}"
                            sh "kubectl rollout restart deployment/cadastro-infantil-flower -n ${namespace}"
                            sh('rm -f '+"$home"+'/.kube/config')
                    }
                }
            }           
        }    
    }

  post {
    success { sendTelegram("🚀 Job Name: ${JOB_NAME} \nBuild: ${BUILD_DISPLAY_NAME} \nStatus: Success \nLog: \n${env.BUILD_URL}console") }
    unstable { sendTelegram("💣 Job Name: ${JOB_NAME} \nBuild: ${BUILD_DISPLAY_NAME} \nStatus: Unstable \nLog: \n${env.BUILD_URL}console") }
    failure { sendTelegram("💥 Job Name: ${JOB_NAME} \nBuild: ${BUILD_DISPLAY_NAME} \nStatus: Failure \nLog: \n${env.BUILD_URL}console") }
    aborted { sendTelegram ("😥 Job Name: ${JOB_NAME} \nBuild: ${BUILD_DISPLAY_NAME} \nStatus: Aborted \nLog: \n${env.BUILD_URL}console") }
  }
}
def sendTelegram(message) {
    def encodedMessage = URLEncoder.encode(message, "UTF-8")
    withCredentials([string(credentialsId: 'telegramToken', variable: 'TOKEN'),
    string(credentialsId: 'telegramChatId', variable: 'CHAT_ID')]) {
        response = httpRequest (consoleLogResponseBody: true,
                contentType: 'APPLICATION_JSON',
                httpMode: 'GET',
                url: 'https://api.telegram.org/bot'+"$TOKEN"+'/sendMessage?text='+encodedMessage+'&chat_id='+"$CHAT_ID"+'&disable_web_page_preview=true',
                validResponseCodes: '200')
        return response
    }
}
def getKubeconf(branchName) {
    if("main".equals(branchName)) { return "config_prd"; }
    else if ("master".equals(branchName)) { return "config_prd"; }
    else if ("homolog".equals(branchName)) { return "config_release"; }
    else if ("release".equals(branchName)) { return "config_release"; }
    else if ("develop".equals(branchName)) { return "config_release"; }
    else if ("development".equals(branchName)) { return "config_release"; }  
}
