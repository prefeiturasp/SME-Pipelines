#!/usr/bin/env groovy

def call(Map stageParams) {

    def packageManager = stageParams.packageManager 
    def testCommand = stageParams.testCommand
    def nodeVersion = stageParams.nodeVersion

    withCredentials([
        file(credentialsId: "${env.kubeconfig}", variable: 'config'),
        string(credentialsId: 'MYSQL_QA', variable: 'database')
        ]){
        
        sh '''
            [ -f "$HOME/.kube/config" ] && rm -f "$HOME/.kube/config"
            mkdir -p "$HOME/.kube"
            cp "$config" "$HOME/.kube/config"
            export KUBECONFIG="$HOME/.kube/config"

            cd "${helmDir}"

            echo "CONFIGURANDO AMBIENTE"
            export BRANCHNAME="${branchname}"
            export AMBIENTE="${ambiente}"
            export DATABASE_HOST="$database"
            export DATABASE_NAME="${project}_${ambiente}"
            export DATABASE_USER="usr_${project}_${ambiente}"
            export IMAGE_TAG="${TAG}"
            export INGRESS_HOST="${ambiente}-${project}.sme.prefeitura.sp.gov.br"
            export WP_URL="http://${INGRESS_HOST}"

            envsubst < values.yaml > values-${ambiente}.yaml

            echo "REALIZANDO HELM UPGRADE"
            helm upgrade ${project} . \
                --namespace ${ambiente}-${project} \
                --set replicaCount=1 \
                --values values-${ambiente}.yaml \
                --install \
                --history-max 3 \
                --timeout 1m0s \
                --atomic \
                #--debug \
                --wait
        '''

        sh '''
            echo "REALIZANDO RESTART DO DEPLOYMENT"
            kubectl rollout restart deployment/${project}-wp-stack -n ${ambiente}-${project}
        '''
    }
}