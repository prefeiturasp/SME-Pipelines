#!/usr/bin/env groovy

def call(Map config) {

    withDotNet(sdk: config.dotnetVersion) {
        env.failedStage = env.STAGE_NAME
        def project = config.projectPath.tokenize('/').last()
        def stashName = project.replace('.', '-')

        retry(2) {
            if (config.testTool == "dotnet-test") {
                dotnetTest(
                    project: config.projectPath,
                    properties: [
                        CollectCoverage: 'true',
                        CoverletOutputFormat: 'opencover'
                    ],
                    collect: 'Code Coverage',
                    noBuild: false,
                    continueOnError: false
                )
                stash includes: "${config.projectPath}/coverage.opencover.xml", name: stashName, allowEmpty: true
            }

            if (config.testTool == "dotnet-coverage") {
                sh """
                    dotnet tool install --global dotnet-coverage
                    export PATH="\$PATH:/home/jenkins/.dotnet/tools"
                    cd ${config.projectPath}
                    dotnet-coverage collect "dotnet test" -f xml -o "coverage.xml"
                """
                stash includes: "${config.projectPath}/coverage.xml", name: stashName, allowEmpty: true
            }
        }
    }
}