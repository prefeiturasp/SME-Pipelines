def call() {
    def msg = sh(
        script: "git log -1 --pretty=%B",
        returnStdout: true
    ).trim()

    def matcher = msg =~ /from\s+[^\/]+\/(.+)/

    if (matcher) {
        return matcher[0][1].trim()
    }

    return null
}