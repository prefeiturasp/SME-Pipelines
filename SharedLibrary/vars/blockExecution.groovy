def call(Map stageParams) {

    def blockedAuthors = [
        'bot-ci',
        'dependabot[bot]',
        'Wilson Calixto Cordovil junior'
    ]

    def author = env.CHANGE_ID
        ? sh(
            script: "git log origin/${env.CHANGE_TARGET}..HEAD --pretty=format:%an | sort -u",
            returnStdout: true
            ).trim()
        : sh(
            script: "git log -1 --pretty=format:%an",
            returnStdout: true
            ).trim()
    
    echo "Author(es) do change:\n${author}"

    def authors = author.split('\n')

    def blocked = authors.findAll { it in blockedAuthors }

    if (blocked) {
        currentBuild.result = 'ABORTED'
        error("""
            ⚠️ Pipeline cancelada pois o commit foi realizado pelo time de QA!
        """)
    }
}