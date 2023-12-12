pipeline {
    environment {
      branchname =  env.BRANCH_NAME.toLowerCase()
      kubeconfig = getKubeconf(env.branchname)
      registryCredential = 'jenkins_registry'
      namespace = "${env.branchname == 'develop' ? 'serap-itens-dev' : env.branchname == 'release' ? 'serap-itens-hom' : env.branchname == 'release-r2' ? 'serap-itens-hom2' : 'sme-serap-itens' }"
    }
  
    agent { kubernetes { 
              label 'dotnet5-rc'
              defaultContainer 'dotnet5-rc'
            }
          }

    options {
      buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '20'))
      disableConcurrentBuilds()
      skipDefaultCheckout()
    }
  
    stages {

        stage('CheckOut') {            
            steps { checkout scm }            
        }
        
        stage('BuildProjeto') {
          steps {
            sh "echo executando build"
            sh 'dotnet build'
          }
        }

        stage('Sonar') {
	      when { anyOf { branch 'release'; } }
          steps {
              withSonarQubeEnv('sonarqube-local'){
                sh 'echo "[ INFO ] Iniciando analise Sonar..." && dotnet-sonarscanner begin /k:"SME-Prova-Serap-Itens-api"'
                sh 'dotnet build'
                sh 'dotnet-sonarscanner end'

            }
          }
        }

        

        stage('Docker Build') {
          when { anyOf { branch 'master'; branch 'main'; branch 'develop'; branch 'release'; branch 'release-r2'; } }
          agent { kubernetes { 
              label 'builder'
              defaultContainer 'builder'
            }
          }
          steps {
            checkout scm
            script {
              imagename1 = "registry.sme.prefeitura.sp.gov.br/${env.branchname}/serap-itens-api"
              dockerImage1 = docker.build(imagename1, "-f src/SME.SERAp.Prova.Item.Api/Dockerfile .")
              docker.withRegistry( 'https://registry.sme.prefeitura.sp.gov.br', registryCredential ) {
              dockerImage1.push()
              }
              sh "docker rmi $imagename1"
              //sh "docker rmi $imagename2"
            }
          }
        }
	    
        stage('Deploy'){
            when { anyOf {  branch 'master'; branch 'main'; branch 'develop'; branch 'release';  } }
            agent { kubernetes { 
              label 'builder'
              defaultContainer 'builder'
            }
          }        
            steps {
                script{
                    if ( env.branchname == 'main' ||  env.branchname == 'master' || env.branchname == 'homolog' || env.branchname == 'release' ) {
                        sendTelegram("🤩 [Deploy ${env.branchname}] Job Name: ${JOB_NAME} \nBuild: ${BUILD_DISPLAY_NAME} \nMe aprove! \nLog: \n${env.BUILD_URL}")
                        withCredentials([string(credentialsId: 'aprovadores-prova-serap-itens', variable: 'aprovadores')]) {
                                timeout(time: 24, unit: "HOURS") {
                                    input message: 'Deseja realizar o deploy?', ok: 'SIM', submitter: "${aprovadores}"
                                }
                            }
                        withCredentials([file(credentialsId: "${kubeconfig}", variable: 'config')]){
                            sh('cp $config '+"$home"+'/.kube/config')
                            sh 'kubectl rollout restart deployment/serap-itens-api -n ${namespace}'
                            sh('rm -f '+"$home"+'/.kube/config')
                        }
                    }
                    else{
                        withCredentials([file(credentialsId: "${kubeconfig}", variable: 'config')]){
                            sh('cp $config '+"$home"+'/.kube/config')
                            sh 'kubectl rollout restart deployment/serap-itens-api -n ${namespace}'
                            sh('rm -f '+"$home"+'/.kube/config')
                        }
                    }
                }
            }           
        }

        stage('Flyway') {
          when { anyOf {  branch 'master'; branch 'main'; branch 'develop'; branch 'release';  } }
          agent { kubernetes { 
              label 'builder'
              defaultContainer 'builder'
            }
          }
          steps{
            withCredentials([string(credentialsId: "flyway_serapitens_${branchname}", variable: 'url')]) {
            checkout scm
            sh 'docker run --rm -v $(pwd)/scripts:/opt/scripts registry.sme.prefeitura.sp.gov.br/devops/flyway:5.2.4 -url=$url -locations="filesystem:/opt/scripts" -outOfOrder=true migrate'
            }
          }		
        }

    }

  post {
    always {cleanWs()}
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
}
