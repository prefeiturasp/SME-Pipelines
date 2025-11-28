#!/usr/bin/env groovy

def call(Map stageParams) {

    def scannerHome = tool("sonar-${stageParams.scannerHome}")
    def dotnetVersion = stageParams.dotnetVersion
    def coverageType = stageParams.coverageType
    def project = stageParams.project
    def coverageExclusions = stageParams.coverageExclusions
    def sonarExclusions = stageParams.sonarExclusions

    if (coverageType == "dotnet-coverage") {
        def coverageTool = "/d:sonar.cs.vscoveragexml.reportsPaths="
        def coveragePath = "**/coverage.xml"
    } else if (coverageType == "dotCover") {
        def coverageTool = "/d:sonar.cs.dotcover.reportsPaths="
        def coveragePath = "**/dotCover.Output.html"
    } else if (coverageType == "OpenCover") {
        def coverageTool = "/d:sonar.cs.opencover.reportsPaths"
        def coveragePath = "**/coverage.xml"
    } else if (coverageType == "Coverlet") {
        def coverageTool = "/d:sonar.cs.opencover.reportsPaths"
        def coveragePath = "**/coverage.xml"
    } else {
        def coverageTool = "/d:sonar.cs.opencover.reportsPaths"
        def coveragePath = "**/coverage.xml"
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