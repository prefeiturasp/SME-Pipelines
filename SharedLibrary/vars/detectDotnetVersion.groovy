def call(String defaultVersion = 'dotnet-5', String pathDetect = '.') {
    // Garante que o caminho não é vazio ou nulo
    if (!pathDetect) {
        pathDetect = '.'
    }

    // Tenta primeiro via global.json no caminho especificado ou na raiz
    def globalJsonPath = 'global.json'
    if (pathDetect != '.' && fileExists("${pathDetect}/global.json")) {
        globalJsonPath = "${pathDetect}/global.json"
    }

    if (fileExists(globalJsonPath)) {
        try {
            def globalJson = readJSON file: globalJsonPath
            def version = globalJson?.sdk?.version?.split('\\.')?[0]
            if (version) return "dotnet-${version}"
        } catch (e) {
            echo "Erro ao ler global.json: ${e.message}"
        }
    }

    // Tenta via .csproj (buscando a tag TargetFramework)
    try {
        // Executa um shell local no workspace para extrair da pasta correspondente
        def targetFramework = sh(
            script: "grep -roPh '(?<=<TargetFramework>)[^<]+' \"${pathDetect}\" | head -n 1",
            returnStdout: true
        ).trim()

        if (targetFramework.startsWith("net")) {
            def versionStr = targetFramework.replaceAll("net(coreapp)?", "")
            def major = versionStr.split('\\.')[0]
            return "dotnet-${major}"
        }
    } catch (e) {
        echo "Não foi possível extrair a versão dos arquivos .csproj em ${pathDetect}: ${e.message}"
    }

    // Se falhar no subdiretório, tenta no workspace inteiro como último suspiro
    if (pathDetect != '.') {
        try {
            def targetFramework = sh(
                script: "grep -roPh '(?<=<TargetFramework>)[^<]+' src/ || grep -roPh '(?<=<TargetFramework>)[^<]+' . | head -n 1",
                returnStdout: true
            ).trim()

            if (targetFramework.startsWith("net")) {
                def versionStr = targetFramework.replaceAll("net(coreapp)?", "")
                def major = versionStr.split('\\.')[0]
                return "dotnet-${major}"
            }
        } catch (e) {
            // Silencioso se também falhar
        }
    }

    return defaultVersion
}
