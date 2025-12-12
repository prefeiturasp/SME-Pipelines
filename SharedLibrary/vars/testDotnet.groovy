#!/usr/bin/env groovy

def call(Map tests) {

    def tasks = [:]

    tests.each { testName, config ->

        tasks[testName] = {
            stage(testName) {
                agent {
                    kubernetes {
                        label 'builder-debian'
                        defaultContainer 'builder-debian'
                    }
                }

                steps {
                    script {
                        withDotNet(sdk: config.dotnetVersion) {
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
                                    stash includes: "${config.projectPath}/coverage.opencover.xml", name: config.stashName, allowEmpty: true
                                }

                                if (config.testTool == "dotnet-coverage") {
                                    sh """
                                        dotnet tool install --global dotnet-coverage
                                        export PATH="\$PATH:/home/jenkins/.dotnet/tools"
                                        cd ${config.projectPath}
                                        dotnet-coverage collect "dotnet test" -f xml -o "coverage.xml"
                                    """
                                    stash includes: "${config.projectPath}/coverage.xml", name: config.stashName, allowEmpty: true
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    parallel tasks
}
