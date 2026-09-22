#!/usr/bin/env groovy

def call(Map stageParams) {

    def repo = stageParams.repo
    def credentialsId = stageParams.credentialsId

    sh 'python -m pip install --root-user-action=ignore ".[docs]"'
    sh 'pip install sphinx furo'

     try {
        sh 'make -C docs html'
    } catch (err) {
        sh 'sphinx-build -b html docs/ docs/_build/html'
    }

    sh 'pip install --root-user-action=ignore ghp-import'

    // Containers rodam como root sobre um workspace de outro uid; o git
    // recente recusa operar nele ("dubious ownership") sem essa exceção.
    sh 'git config --global --add safe.directory "$WORKSPACE"'

    // Traz o gh-pages remoto (se existir) pra virar o pai do novo commit do
    // ghp-import. Sem isso o ghp-import cria um commit órfão e o push vira
    // não-fast-forward, o que a proteção da branch gh-pages recusa.
    sh 'git fetch origin gh-pages:gh-pages || true'

    // ghp-import cria o commit na branch gh-pages local (com .nojekyll, pra
    // servir os assets do Sphinx sem o processamento do Jekyll do GitHub
    // Pages) usando as credenciais de leitura já presentes no checkout.
    sh "ghp-import -n -m \"Publica docs (${env.BRANCH_NAME} @ ${env.GIT_COMMIT})\" docs/_build/html"

    // O push exige permissão de escrita, por isso só aqui usamos o token.
    // $githubToken (sem interpolação Groovy) evita o aviso de secret exposto.
    withCredentials([string(credentialsId: credentialsId, variable: 'githubToken')]) {
        sh "git push https://x-access-token:\$githubToken@github.com/${repo}.git gh-pages:gh-pages"
    }

    def (org, repoName) = repo.split('/')
    echo "Documentação publicada em https://${org}.github.io/${repoName}/"
}
