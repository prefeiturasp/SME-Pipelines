#!/usr/bin/env groovy

def call(Map config) {

    withDotNet(sdk: config.dotnetVersion) {
        env.failedStage = env.STAGE_NAME
        retry(2) {
            if (config.testTool == "dotnet-test") {
                dotnetTest(
                    project: config.project,
                    properties: [
                        CollectCoverage: 'true',
                        CoverletOutputFormat: 'opencover'
                    ],
                    collect: 'Code Coverage',
                    noBuild: false,
                    continueOnError: false
                )
                stash includes: "**/coverage.opencover.xml", name: config.stashName, allowEmpty: true
            }

            if (config.testTool == "dotnet-coverage") {
                sh """
                    dotnet tool install --global dotnet-coverage
                    export PATH="\$PATH:/home/jenkins/.dotnet/tools"
                    dotnet-coverage collect "dotnet test --filter Category!=Integration ${config.project}" -f xml -o "coverage.xml"
                """
                stash includes: '**/coverage.xml', name: config.stashName, allowEmpty: true
            }
        }
    }
}