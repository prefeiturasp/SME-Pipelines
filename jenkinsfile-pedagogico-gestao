pipeline {
    environment {
      branchname =  env.BRANCH_NAME.toLowerCase()
      kubeconfig = getKubeconf(env.branchname)
      registryCredential = 'jenkins_registry'
      namespace = "${env.branchname == 'release-r2' ? 'sondagem-hom2' : env.branchname == 'release' ? 'sondagem-hom' : env.branchname == 'dev' ? 'sondagem-dev' : 'sme-pedagogico-gestao'}"
    }
  
    agent { kubernetes { 
              label 'dotnet-3-sondagem'
              defaultContainer 'dotnet-3-sondagem'
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

        stage('BuildProjeto') {
          agent { kubernetes { 
              label 'dotnet-3-sondagem'
              defaultContainer 'dotnet-3-sondagem'
            }
	}
          steps {
            checkout scm
            sh "echo executando build"
            sh 'dotnet build'
          }
        }
      
        stage('AnaliseCodigo') {
	        when { branch 'release' }
          agent { kubernetes { 
              label 'dotnet-3-sondagem'
              defaultContainer 'dotnet-3-sondagem'
            }
	}
          steps {
              checkout scm
              withSonarQubeEnv('sonarqube-local'){
                sh 'dotnet-sonarscanner begin /k:"SME-Pedagogico-Gestao"'
                sh 'dotnet build'
                sh 'dotnet-sonarscanner end'
            }
          }
        }

        stage('Build') {
          when { anyOf { branch 'master'; branch 'main'; branch "story/*"; branch 'dev'; branch 'release'; branch 'release-r2'; } }
          agent { kubernetes { 
              label 'builder'
              defaultContainer 'builder'
            }
          } 
          steps {
            checkout scm
            script {
              imagename = "registry.sme.prefeitura.sp.gov.br/${env.branchname}/sme-sondagem-backend"        
              dockerImage = docker.build(imagename, "-f Dockerfile .")
              docker.withRegistry( 'https://registry.sme.prefeitura.sp.gov.br', registryCredential ) {
              dockerImage.push()
              }
              sh "docker rmi $imagename"
            }
          }
        }
	    
        stage('Deploy'){
            when { anyOf {  branch 'master'; branch 'main'; branch 'dev'; branch 'release'; branch 'release-r2'; } }
            agent { kubernetes { 
              label 'builder'
              defaultContainer 'builder'
            }
          }        
            steps {
                script{
                    if ( env.branchname == 'main' ||  env.branchname == 'master' || env.branchname == 'homolog' || env.branchname == 'release' ) {
                        sendTelegram("🤩 [Deploy ${env.branchname}] Job Name: ${JOB_NAME} \nBuild: ${BUILD_DISPLAY_NAME} \nMe aprove! \nLog: \n${env.BUILD_URL}")
                         withCredentials([string(credentialsId: 'aprovadores-sgp', variable: 'aprovadores')]) {
                            timeout(time: 24, unit: "HOURS") {
                                input message: 'Deseja realizar o deploy?', ok: 'SIM', submitter: "${aprovadores}"
                            }
                        }
                    }
                    withCredentials([file(credentialsId: "${kubeconfig}", variable: 'config')]){
                      sh('cp $config '+"$home"+'/.kube/config')
                      sh "kubectl -n ${namespace} rollout restart deploy"
                      sh('rm -f '+"$home"+'/.kube/config')
                    }
                }
            }           
        }

      stage('Flyway') {
        agent { kubernetes { 
              label 'flyway'
              defaultContainer 'flyway'
            }
          }
        steps{
          withCredentials([string(credentialsId: "flyway_pedagogicogestao_${branchname}", variable: 'url')]) {
            checkout scm
            sh 'flyway -url=$url -locations="filesystem:scripts" -outOfOrder=true migrate'
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
    if("master".equals(branchName)) { return "config_prd"; }
    else if ("release".equals(branchName)) { return "config_release"; }
    else if ("release-r2".equals(branchName)) { return "config_release"; }	
    else if ("dev".equals(branchName)) { return "config_release"; }
}
