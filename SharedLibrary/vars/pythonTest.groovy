#!/usr/bin/env groovy

def call(Map stageParams) {

    def requirementsDir = stageParams.requirementsDir
    def testCommand = stageParams.testCommand
    def withMigration = stageParams.withMigration
    def credentialEnv = stageParams.credentialEnv

    retry(2) {
        sh 'pip install --root-user-action=ignore --upgrade pip'
        sh 'pip install --root-user-action=ignore --no-cache-dir -r ${requirementsDir}'
        sh 'pip install --root-user-action=ignore coverage'
        withCredentials([file(credentialsId: credentialEnv, variable: 'env')]) {
            sh '''
                touch .env
                cp "$env" ".env"
                export DJANGO_READ_DOT_ENV_FILE=True

                if [ ${withMigration} == "true" ]; then
                    python manage.py migrate
                fi

                ${testCommand}
                coverage xml
            '''
        }
        
        stash name: 'coverage', includes: 'coverage.xml'
    }
}