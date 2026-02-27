#!/usr/bin/env groovy

def call(Map stageParams) {

    def nodeVersion = stageParams.nodeVersion
    def scannerHome = tool 'sonarscanner-7.2.0';
    def coverageExclusions = stageParams.coverageExclusions
    def sonarExclusions = stageParams.sonarExclusions
    def dockerfilePath = stageParams.dockerfilePath

    nodejs(cacheLocationStrategy: workspace(), nodeJSInstallationName: nodeVersion) {
        withSonarQubeEnv('sonarqube-sme'){
            
            try {
                unstash "coverage"
            } catch (e) {
                echo "Nenhum coverage encontrado para coverage-TesteIntegracao"
            }
            
            if (!env.BRANCH_NAME.startsWith('PR-')) {
                retry(1) {
                    sh"""
                        ${scannerHome}/bin/sonar-scanner \
                            -Dsonar.projectKey=${SONAR_PROJECT} \
                            -Dsonar.branch.name=${branchname} \
                            -Dsonar.javascript.lcov.reportPaths=coverage/lcov.info \
                            -Dsonar.exclusions="${sonarExclusions}" \
                            -Dsonar.coverage.exclusions="${coverageExclusions}"  \
                            -Dsonar.docker.file.patterns=${dockerfilePath} \
                            -Dsonar.scm.provider=git \
                            -Dsonar.sources=.
                    """
                }
            } else {
                retry(1) {
                    sh "git fetch origin refs/heads/${env.CHANGE_TARGET}:refs/remotes/origin/${env.CHANGE_TARGET}"
                    sh "git show-ref | grep ${env.CHANGE_TARGET}"

                    sh"""
                        ${scannerHome}/bin/sonar-scanner \
                            -Dsonar.projectKey=${SONAR_PROJECT} \
                            -Dsonar.pullrequest.branch=${env.CHANGE_BRANCH} \
                            -Dsonar.pullrequest.base=${env.CHANGE_TARGET} \
                            -Dsonar.pullrequest.key=${env.CHANGE_ID} \
                            -Dsonar.javascript.lcov.reportPaths=coverage/lcov.info \
                            -Dsonar.typescript.tsconfigPath=tsconfig.json \
                            -Dsonar.exclusions="${sonarExclusions}" \
                            -Dsonar.coverage.exclusions="${coverageExclusions}"  \
                            -Dsonar.docker.file.patterns=${dockerfilePath} \
                            -Dsonar.scm.provider=git \
                            -Dsonar.sources=.
                    """
                }
            }
        }
    }
}