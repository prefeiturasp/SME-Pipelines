#!/usr/bin/env groovy

def call(config) {
    // Se receber uma lista, executa em paralelo
    if (config instanceof List) {
        return executeParallel(config)
    }
    
    // Se receber um Map, executa individual
    if (config instanceof Map) {
        return executeSingle(config)
    }
    
    error("Configuração inválida. Esperado List ou Map.")
}

def executeParallel(List testConfigs) {
    def parallelStages = [:]
    
    testConfigs.each { config ->
        parallelStages[config.name] = {
            node {
                kubernetes {
                    label config.kubernetesLabel ?: 'builder-debian'
                    defaultContainer config.defaultContainer ?: 'builder-debian'
                }
                
                container(config.defaultContainer ?: 'builder-debian') {
                    executeSingle(config)
                }
            }
        }
    }
    
    parallel parallelStages
}

def executeSingle(Map config) {
    // Validação de parâmetros obrigatórios
    if (!config.testTool || !config.projectPath || !config.stashName || !config.dotnetVersion) {
        error("Parâmetros obrigatórios faltando: testTool, projectPath, stashName, dotnetVersion")
    }

    withDotNet(sdk: config.dotnetVersion) {
        env.failedStage = env.STAGE_NAME
        
        retry(config.retries ?: 2) {
            switch(config.testTool) {
                case "dotnet-test":
                    executeDotnetTest(config)
                    break
                case "dotnet-coverage":
                    executeDotnetCoverage(config)
                    break
                default:
                    error("Ferramenta de teste não suportada: ${config.testTool}")
            }
        }
    }
}

def executeDotnetTest(Map config) {
    dotnetTest(
        project: config.projectPath,
        properties: [
            CollectCoverage: 'true',
            CoverletOutputFormat: 'opencover'
        ],
        collect: 'Code Coverage',
        noBuild: config.noBuild ?: false,
        continueOnError: config.continueOnError ?: false
    )
    stash includes: "${config.projectPath}/coverage.opencover.xml", 
          name: config.stashName, 
          allowEmpty: true
}

def executeDotnetCoverage(Map config) {
    sh """
        dotnet tool install --global dotnet-coverage
        export PATH="\$PATH:/home/jenkins/.dotnet/tools"
        cd ${config.projectPath}
        dotnet-coverage collect "dotnet test" -f xml -o "coverage.xml"
    """
    stash includes: "${config.projectPath}/coverage.xml", 
          name: config.stashName, 
          allowEmpty: true
}