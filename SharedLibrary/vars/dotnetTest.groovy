#!/usr/bin/env groovy

def call(Map tests) {

    def tasks = [:]

    tests.each { testName, config ->

        tasks[testName] = {
            stage(testName) {
                agent { kubernetes { label 'builder-debian'; defaultContainer 'builder-debian' } }
                withDotNet(sdk: config.dotnetVersion) {
                    retry(2) {
                        if (config.testTool == "dotnet-coverage") {
                            sh """
                                dotnet tool install --global dotnet-coverage
                                export PATH="\$PATH:/home/jenkins/.dotnet/tools"
                                cd ${config.projectPath}
                                dotnet-coverage collect "dotnet test" -f xml -o "coverage.xml"
                            """
                            stash includes: "${config.projectPath}/coverage.xml", name: config.stashName, allowEmpty: true
                        } else if (config.testTool == "dotnet test") {
                            sh """
                                dotnet tool install --global coverlet.console
                                export PATH="\$PATH:/home/jenkins/.dotnet/tools"
                                dotnet test ${config.projectPath} /p:CollectCoverage=true /p:CoverletOutputFormat=opencover --collect 'XPlat Code Coverage'
                            """
                            stash includes: "${config.projectPath}/coverage.opencover.xml", name: config.stashName, allowEmpty: true
                        } else {
                            echo "Tool não definida."
                        }
                    }
                }
            }
        }
    }

    parallel tasks
}