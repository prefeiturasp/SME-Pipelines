#!/usr/bin/env groovy

def call(Map stageParams) {

    def scannerHome = tool stageParams.scannerHome
    def dotnetVersion = stageParams.dotnetVersion
    def coverageType = stageParams.coverageType
    def project = stageParams.project

    withDotNet(sdk: dotnetVersion) {
        withSonarQubeEnv('sonarqube-sme'){
            if (!env.BRANCH_NAME.startsWith('PR-')) {
                sh"""
                    dotnet ${scannerHome}/SonarScanner.MSBuild.dll begin \
                        /k:"${SONAR_PROJECT}" \
                        /d:sonar.cs.vscoveragexml.reportsPaths="**/coverage.xml" \
                        /d:sonar.branch.name=${branchname} \
                        /d:sonar.coverage.exclusions="**/teste/**,**/*Dto.cs,**/*ViewModel.cs" \
                        /d:sonar.exclusions="teste/**,**/Migrations/**,**/Configurations/**,**/*Dto.cs,**/*ViewModel.cs,**/Startup.cs,**/Program.cs"
                """
            } else {
                sh"""
                    dotnet ${scannerHome}/SonarScanner.MSBuild.dll begin \
                        /k:"${SONAR_PROJECT}" \
                        /d:sonar.cs.vscoveragexml.reportsPaths="**/coverage.xml" \
                        /d:sonar.pullrequest.branch=${env.CHANGE_BRANCH} \
                        /d:sonar.pullrequest.base=${env.CHANGE_TARGET} \
                        /d:sonar.pullrequest.key=${env.CHANGE_ID} \
                        /d:sonar.coverage.exclusions="**/teste/**,**/*Dto.cs,**/*ViewModel.cs" \
                        /d:sonar.exclusions="teste/**,**/Migrations/**,**/Configurations/**,**/*Dto.cs,**/*ViewModel.cs,**/Startup.cs,**/Program.cs"
                """
            }

            dotnetBuild project: project
            
            sh "dotnet ${scannerHome}/SonarScanner.MSBuild.dll end"
        }
    }
}