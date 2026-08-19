def call() {
    def msg = sh(
        script: "git log -1 --pretty=%B",
        returnStdout: true
    ).trim()

    // Match GitHub pull requests
    def matcher = msg =~ /from\s+[^\/]+\/(.+)/
    if (matcher) {
        return matcher[0][1].trim()
    }

    // Match GitLab merge requests
    def gitlabMatcher = msg =~ /Merge branch '([^']+)'/
    if (gitlabMatcher) {
        return gitlabMatcher[0][1].trim()
    }

    return null
}