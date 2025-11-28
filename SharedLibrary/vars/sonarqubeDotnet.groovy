#!/usr/bin/env groovy

def call(Map stageParams) {

    def dotnetVersion = stageParams.dotnetVersion
    def scannerHome = tool("sonar-${stageParams.dotnetVersion}")
    def coverageType = stageParams.coverageType
    def project = stageParams.project
    def coverageExclusions = stageParams.coverageExclusions
    def sonarExclusions = stageParams.sonarExclusions
    def coverageTool
    def coveragePath

    if (coverageType == "dotnet-coverage") {
        coverageTool = "/d:sonar.cs.vscoveragexml.reportsPaths="
        coveragePath = "**/coverage.xml"
    } else if (coverageType == "dotCover") {
        coverageTool = "/d:sonar.cs.dotcover.reportsPaths="
        coveragePath = "**/dotCover.Output.html"
    } else if (coverageType == "OpenCover") {
        coverageTool = "/d:sonar.cs.opencover.reportsPaths"
        coveragePath = "**/coverage.xml"
    } else if (coverageType == "Coverlet") {
        coverageTool = "/d:sonar.cs.opencover.reportsPaths"
        coveragePath = "**/coverage.xml"
    } else {
        coverageTool = "/d:sonar.cs.opencover.reportsPaths"
        coveragePath = "**/coverage.xml"
    }
    
    withDotNet(sdk: dotnetVersion) {
        withSonarQubeEnv('sonarqube-sme'){
            if (!env.BRANCH_NAME.startsWith('PR-')) {
                sh"""
                    dotnet ${scannerHome}/SonarScanner.MSBuild.dll begin \
                        /k:"${SONAR_PROJECT}" \
                        ${coverageTool}"${coveragePath}" \
                        /d:sonar.branch.name=${branchname} \
                        /d:sonar.coverage.exclusions="${coverageExclusions}" \
                        /d:sonar.exclusions="${coverageExclusions}"
                """
            } else {
                sh"""
                    dotnet ${scannerHome}/SonarScanner.MSBuild.dll begin \
                        /k:"${SONAR_PROJECT}" \
                        ${coverageTool}"${coveragePath}" \
                        /d:sonar.pullrequest.branch=${env.CHANGE_BRANCH} \
                        /d:sonar.pullrequest.base=${env.CHANGE_TARGET} \
                        /d:sonar.pullrequest.key=${env.CHANGE_ID} \
                        /d:sonar.coverage.exclusions="${coverageExclusions}" \
                        /d:sonar.exclusions="${coverageExclusions}"
                """
            }

            dotnetBuild project: project
            
            sh "dotnet ${scannerHome}/SonarScanner.MSBuild.dll end"
        }
    }
}