def call(Map stageParams) {

    def listFiles = env.CHANGE_ID
        ? sh(
            script: "git fetch origin ${CHANGE_TARGET} & git diff --name-status origin/${CHANGE_TARGET}...HEAD | cut -f2-",
            returnStdout: true
            ).trim().split('\n')
        : sh(
            script: "git diff --name-status HEAD^ HEAD | cut -f2-",
            returnStdout: true
            ).trim().split('\n')
    
    // def listedFiles = listFiles.split('\n')
    def ignoreList = env.IGNORE_FILES.split(',').collect { it.trim() }
    def blocked = listFiles.findAll { file ->
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