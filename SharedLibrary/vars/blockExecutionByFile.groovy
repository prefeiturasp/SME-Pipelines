def call(Map stageParams) {

    def listFiles = env.CHANGE_ID
        ? sh(
            script: "git diff --name-status ${env.CHANGE_TARGET}^ HEAD | cut -f2- > ./commit_files_all",
            returnStdout: true
            ).trim()
        : sh(
            script: "git diff --name-status HEAD^ HEAD | cut -f2- > ./commit_files_all",
            returnStdout: true
            ).trim()
    
    def listedFiles = listFiles.split('\n')
    def blocked = listedFiles.findAll { it in ${env.START_IGNORE} }

    echo "Change:\n${listFiles}"

    // if (blocked) {
    //     currentBuild.result = 'ABORTED'
    //     error("""
    //         ⚠️ Pipeline cancelada pois o commit foi realizado pelo time de QA!
    //     """)
    // }
}