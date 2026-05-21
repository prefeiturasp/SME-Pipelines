#!/usr/bin/env groovy

def call(Map stageParams) {

    def requirementsDir = stageParams.requirementsDir
    def testCommand     = stageParams.testCommand
    def withMigration   = stageParams.withMigration
    def credentialEnv   = stageParams.credentialEnv
    def fileManage      = stageParams.fileManage ?: 'manage.py'

    sh 'pip install --root-user-action=ignore --upgrade pip'
    sh "pip install --root-user-action=ignore --no-cache-dir -r ${requirementsDir}"
    sh 'pip install --root-user-action=ignore coverage'

    def runTests = {
        if (withMigration == "true") {
            sh "export DJANGO_READ_DOT_ENV_FILE=True && python ${fileManage} migrate"
        }

        sh "${testCommand}"
        sh 'coverage xml'
    }

    if (credentialEnv?.trim()) {
        withCredentials([file(credentialsId: credentialEnv, variable: 'env')]) {
            sh 'touch .env'
            sh 'cp "$env" ".env"'
            sh 'export DJANGO_READ_DOT_ENV_FILE=True'

            runTests()
        }
    } else {
        echo "credentialEnv vazio — executando sem withCredentials"
        runTests()
    }

    stash name: 'coverage', includes: 'coverage.xml'
}