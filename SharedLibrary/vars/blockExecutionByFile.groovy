def call(Map stageParams) {

    def listFiles = sh(
        script: "git diff --name-status HEAD^ HEAD | cut -f2-",
        returnStdout: true
        ).trim().split('\n')
    
    def ignoreList = env.IGNORE_FILES.split(',').collect { it.trim() }
    def blocked = listFiles.findAll { file ->
        ignoreList.any { ignore ->
            file.contains(ignore)
        }
    }

    echo "Changes:\n${listFiles}"

    if (blocked) {
        currentBuild.result = 'SUCCESS'
        echo '⚠️ Execução cancelada pois na change existem arquivos ignorados!'
        return
    }
}