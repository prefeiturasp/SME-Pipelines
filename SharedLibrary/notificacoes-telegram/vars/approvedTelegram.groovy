#!/usr/bin/env groovy

def call(String chatIdCredential, String approvedBy) {

    def messageTemplate = (
        "<b>✅ REALIZADA APROVAÇÃO PARA O JOB:</b> <a href='${env.JOB_URL}'>${env.JOB_NAME}</a>\n\n" +
        "<b>Build Number:</b> <a href='${env.BUILD_URL}'>${env.BUILD_DISPLAY_NAME}</a>" +
        "<b>Aprovado por:</b> ${approvedBy}"
    )

    def encodedMessage = URLEncoder.encode(messageTemplate, "UTF-8")

    withCredentials([
        string(credentialsId: 'telegramTokenGeral', variable: 'TOKEN'),
        string(credentialsId: chatIdCredential, variable: 'CHAT_ID')
    ]) {

        httpRequest(
            consoleLogResponseBody: true,
            contentType: 'APPLICATION_JSON',
            httpMode: 'GET',
            url: "https://api.telegram.org/bot${TOKEN}/sendMessage?text=${encodedMessage}&chat_id=${CHAT_ID}&parse_mode=HTML&disable_web_page_preview=true",
            validResponseCodes: '200'
        )
    }
}