def call(Map stageParams) {

    def listFiles = env.CHANGE_ID
        ? sh(
            script: "git diff --name-status ${env.CHANGE_TARGET}^ HEAD | cut -f2-",
            returnStdout: true
            ).trim()
        : sh(
            script: "git diff --name-status HEAD^ HEAD | cut -f2-",
            returnStdout: true
            ).trim()
    
    def listedFiles = listFiles.split('\n')
    def ignoreList = env.IGNORE_FILES.split(',').collect { it.trim() }
    def blocked = listedFiles.findAll { file ->
        ignoreList.any { ignore ->
            file.contains(ignore)
        }
    }

    echo "Changes:\n${listedFiles}"

    if (blocked) {
        currentBuild.result = 'ABORTED'
        error("""
            ⚠️ Pipeline cancelada no commit foi realizado pelo time de QA!
        """)
    }
}