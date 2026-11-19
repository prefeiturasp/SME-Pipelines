#!/usr/bin/env groovy

// Solicita aprovação manual antes do deploy em produção.
// Avisa no Telegram, aguarda o input de um dos aprovadores e notifica quem aprovou.
// O nome do aprovador fica disponível em env.APROVADO_POR.
//
// Parâmetros:
//   aprovadoresCredentialsId (obrigatório) - credential (secret text) com os usuários/grupos aprovadores, separados por vírgula
//   telegramChatId           (opcional)    - credential do chat do Telegram (padrão: env.TELEGRAM_CHAT_ID)
//   timeoutHours             (opcional)    - tempo máximo de espera em horas (padrão: 24)
//   message                  (opcional)    - mensagem exibida no input (padrão: 'Deseja realizar o deploy em produção?')
//
// Exemplo:
//   approveProd(aprovadoresCredentialsId: 'aprovadores-x')

def call(Map stageParams = [:]) {

    def aprovadoresCredentialsId = stageParams.aprovadoresCredentialsId
    def telegramChatId = stageParams.telegramChatId ?: env.TELEGRAM_CHAT_ID
    def timeoutHours = stageParams.timeoutHours ?: 24
    def message = stageParams.message ?: 'Deseja realizar o deploy em produção?'

    if (!aprovadoresCredentialsId?.trim()) {
        error "approveProd: parâmetro 'aprovadoresCredentialsId' é obrigatório"
    }

    withCredentials([string(credentialsId: aprovadoresCredentialsId, variable: 'aprovadores')]) {
        timeout(time: timeoutHours as int, unit: 'HOURS') {
            approveTelegram(telegramChatId)

            def aprovadoPor = input(
                message: message,
                ok: 'SIM',
                submitter: "${aprovadores}",
                submitterParameter: 'APPROVED_BY'
            )

            env.APROVADO_POR = "${aprovadoPor}"
            echo "Deploy aprovado por: ${env.APROVADO_POR}"

            approvedTelegram(telegramChatId, env.APROVADO_POR)
        }
    }
}
