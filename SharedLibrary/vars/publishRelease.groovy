#!/usr/bin/env groovy

def call(Map stageParams) {

    def repo = stageParams.repo
    def credentialsId = stageParams.credentialsId

    // Detecta automaticamente o arquivo de versão do projeto
    def versionFile
    if (fileExists('pyproject.toml')) {
        versionFile = 'pyproject.toml'
    } else if (fileExists('package.json')) {
        versionFile = 'package.json'
    } else {
        error "Nenhum arquivo de versão encontrado (pyproject.toml ou package.json)"
    }

    def versionScript
    if (versionFile == 'package.json') {
        // "version": "0.1.0"
        versionScript = "grep -m1 -E '^[[:space:]]*\"version\"[[:space:]]*:' '${versionFile}' | sed -E 's/.*\"version\"[[:space:]]*:[[:space:]]*\"([^\"]+)\".*/\\1/'"
    } else {
        // version = "0.1.0"
        versionScript = "grep -m1 -E '^version[[:space:]]*=' '${versionFile}' | sed -E 's/version[[:space:]]*=[[:space:]]*\"([^\"]+)\".*/\\1/'"
    }

    def version = sh(script: versionScript, returnStdout: true).trim()

    if (!version) {
        error "Não foi possível localizar 'version' em ${versionFile}"
    }

    echo "Versão ${version} lida de ${versionFile}"

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
            echo "Release ${env.RELEASE_TAG} já existe em ${repo}, seguindo sem publicar de novo."
            return
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

        echo "Release ${env.RELEASE_TAG} publicada em ${repo}"
    }
}
