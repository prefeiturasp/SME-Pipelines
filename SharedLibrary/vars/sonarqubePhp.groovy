#!/usr/bin/env groovy

def call(Map stageParams) {

    def scannerHome = tool 'sonarscanner-7.2.0';
    def coverageExclusions = stageParams.coverageExclusions
    def sonarExclusions = stageParams.sonarExclusions
    def dockerfilePath = stageParams.dockerfilePath
    def sources = stageParams.sources

    withSonarQubeEnv('sonarqube-sme'){
        
        unstash "coverage"
        
        if (!env.BRANCH_NAME.startsWith('PR-')) {
            sh"""
                ${scannerHome}/bin/sonar-scanner \
                    -Dsonar.projectKey=${SONAR_PROJECT} \
                    -Dsonar.branch.name=${branchname} \
                    -Dsonar.php.coverage.reportPaths=coverage.xml=coverage.xml \
                    -Dsonar.exclusions="${sonarExclusions}" \
                    -Dsonar.coverage.exclusions="${coverageExclusions}"  \
                    -Dsonar.docker.file.patterns=${dockerfilePath} \
                    -Dsonar.sources=${sources}
            """
        } else {
            sh "git fetch origin refs/heads/${env.CHANGE_TARGET}:refs/remotes/origin/${env.CHANGE_TARGET}"
            sh "git show-ref | grep ${env.CHANGE_TARGET}"
            
            sh"""
                ${scannerHome}/bin/sonar-scanner \
                    -Dsonar.projectKey=${SONAR_PROJECT} \
                    -Dsonar.pullrequest.branch=${env.CHANGE_BRANCH} \
                    -Dsonar.pullrequest.base=${env.CHANGE_TARGET} \
                    -Dsonar.pullrequest.key=${env.CHANGE_ID} \
                    -Dsonar.php.coverage.reportPaths=coverage.xml=coverage.xml \
                    -Dsonar.exclusions="${sonarExclusions}" \
                    -Dsonar.coverage.exclusions="${coverageExclusions}"  \
                    -Dsonar.docker.file.patterns=${dockerfilePath} \
                    -Dsonar.sources=${sources}
            """
        }
    }
}