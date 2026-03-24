def call() {
    return sh(
        script: "git log -1 --pretty=%B",
        returnStdout: true
    ).trim()
}