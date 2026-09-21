#!/usr/bin/env groovy

def call(Map stageParams) {

    def repo = stageParams.repo
    def credentialsId = stageParams.credentialsId

    sh 'python -m pip install --root-user-action=ignore ".[docs]"'
    sh 'make -C docs html'

    sh 'pip install --root-user-action=ignore ghp-import'

    // Containers rodam como root sobre um workspace de outro uid; o git
    // recente recusa operar nele ("dubious ownership") sem essa exceção.
    sh 'git config --global --add safe.directory "$WORKSPACE"'

    // ghp-import cria o commit na branch gh-pages local (com .nojekyll, pra
    // servir os assets do Sphinx sem o processamento do Jekyll do GitHub
    // Pages) usando as credenciais de leitura já presentes no checkout.
    sh "ghp-import -n -m \"Publica docs (${env.BRANCH_NAME} @ ${env.GIT_COMMIT})\" docs/_build/html"

    // O push exige permissão de escrita, por isso só aqui usamos o token.
    withCredentials([string(credentialsId: credentialsId, variable: 'githubToken')]) {
        sh "git push https://x-access-token:${githubToken}@github.com/${repo}.git gh-pages:gh-pages --force"
    }

    def (org, repoName) = repo.split('/')
    echo "Documentação publicada em https://${org}.github.io/${repoName}/"
}
