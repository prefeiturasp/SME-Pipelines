#!/usr/bin/env groovy

def call(Map stageParams) {

    def packageManager = stageParams.packageManager
    def packageCommand = stageParams?.packageCommand ?: ""
    def testCommand = stageParams.testCommand
    def nodeVersion = stageParams.nodeVersion

    nodejs(cacheLocationStrategy: workspace(), nodeJSInstallationName: nodeVersion) {
        if (packageManager == "yarn") {
            sh 'npm install -g yarn'
            sh 'yarn -v'
            sh 'yarn'
                
        } else if (packageManager == "npm") {
            sh 'npm install'
        } else if (packageManager == "custom") {
            sh "${packageCommand}"
        } else {
            echo "Package manager não definido."
        }
        
        sh "${testCommand}"
        stash name: 'coverage', includes: 'coverage/lcov.info'
    }
}