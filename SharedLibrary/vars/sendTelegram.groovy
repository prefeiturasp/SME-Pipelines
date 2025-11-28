#!/usr/bin/env groovy

def call(String message, String chatIdCredential) {

    def commitHash = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
    def repoUrl = sh(script: "git config --get remote.origin.url", returnStdout: true).trim()
    def author = sh(script: "git log -1 --pretty=format:'%an'", returnStdout: true).trim()

    repoUrl = repoUrl
        .replace("git@github.com:", "https://github.com/")
        .replace(".git", "")

    def commitUrl = "${repoUrl}/commit/${commitHash}"

    def messageTemplate = (
        "<b>Job Name:</b> <a href='${env.JOB_URL}'>${env.JOB_NAME}</a>\n\n" +
        "<b>Status:</b> ${message}\n" +
        "<b>Build Number:</b> <a href='${env.BUILD_URL}'>${env.BUILD_DISPLAY_NAME}</a>\n" +
        "<b>Commit:</b> <a href='${commitUrl}'>${commitHash}</a>\n" +
        "<b>Commit Author:</b> ${author}\n" +
        "<b>Log:</b> <a href='${env.BUILD_URL}pipeline-overview'>Pipeline overview</a>"
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