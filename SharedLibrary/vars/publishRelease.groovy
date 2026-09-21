#!/usr/bin/env groovy

def call(Map stageParams) {

    def repo = stageParams.repo
    def credentialsId = stageParams.credentialsId

    def version = sh(
        script: "grep -m1 -E '^version[[:space:]]*=' pyproject.toml | sed -E 's/version[[:space:]]*=[[:space:]]*\"([^\"]+)\"/\\1/'",
        returnStdout: true
    ).trim()

    if (!version) {
        error "Não foi possível localizar 'version' em pyproject.toml"
    }

    env.RELEASE_TAG = "v${version}"

    withCredentials([string(credentialsId: credentialsId, variable: 'githubToken')]) {

        def checkStatus = sh(
            script: """
                curl -s -o /dev/null -w '%{http_code}' \
                    -H "Authorization: token ${githubToken}" \
                    https://api.github.com/repos/${repo}/releases/tags/${env.RELEASE_TAG}
            """,
            returnStdout: true
        ).trim()

        if (checkStatus == "200") {
            error "Release ${env.RELEASE_TAG} já existe em ${repo}. Atualize o campo version em pyproject.toml antes de publicar."
        }

        def createStatus = sh(
            script: """
                curl -s -o /dev/null -w '%{http_code}' -X POST \
                    -H "Authorization: token ${githubToken}" \
                    -H "Accept: application/vnd.github+json" \
                    https://api.github.com/repos/${repo}/releases \
                    -d '{"tag_name":"${env.RELEASE_TAG}","target_commitish":"${env.BRANCH_NAME}","name":"${env.RELEASE_TAG}","generate_release_notes":true}'
            """,
            returnStdout: true
        ).trim()

        if (createStatus != "201") {
            error "Falha ao publicar release ${env.RELEASE_TAG} em ${repo} (HTTP ${createStatus})"
        }
    }

    echo "Release ${env.RELEASE_TAG} publicada em ${repo}"
}
