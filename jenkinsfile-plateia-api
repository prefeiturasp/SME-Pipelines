pipeline {
    environment {
      branchname =  env.BRANCH_NAME.toLowerCase()
      kubeconfig = getKubeconf(env.branchname)
      registryCredential = 'jenkins_registry'
      namespace = "${env.branchname == 'develop' ? 'plateia-api-dev' : env.branchname == 'homolog' ? 'plateia-api-hom' : env.branchname == 'homolog-r2' ? 'plateia-api-hom2' : 'sme-plateia-api' }"
    }

    agent { kubernetes { 
              label 'builder'
              defaultContainer 'builder'
            }
          }

    options {
      buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '10'))
      disableConcurrentBuilds()
      skipDefaultCheckout()
    }

    stages {

        stage('CheckOut') {
            steps { checkout scm }
        }


        stage('Build') {
          when { anyOf { branch 'master'; branch 'main'; branch 'develop'; branch 'release'; branch 'homolog';  } }
          steps {
            script {
              imagename1 = "registry.sme.prefeitura.sp.gov.br/${env.branchname}/sme-plateiapy-api"
              dockerImage1 = docker.build(imagename1, "-f Dockerfile .")
              docker.withRegistry( 'https://registry.sme.prefeitura.sp.gov.br', registryCredential ) {
              dockerImage1.push()
              }
              sh "docker rmi $imagename1"
            }
          }
        }

        stage('Deploy'){
            when { anyOf {  branch 'master'; branch 'main'; branch 'develop'; branch 'release'; branch 'homolog';  } }
            steps {
                script{
                    withCredentials([file(credentialsId: "${kubeconfig}", variable: 'config')]){
                            sh('cp $config '+"$home"+'/.kube/config')
                            sh 'kubectl -n ${namespace} rollout restart deploy'
                            sh('rm -f '+"$home"+'/.kube/config')
                    }
                }
            }
        }
    }

  post {
    always { cleanWs notFailBuild: true }
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
