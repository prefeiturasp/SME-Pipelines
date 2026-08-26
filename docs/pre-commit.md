# Pre-commit no SME-Pipelines

Este repositório usa o [pre-commit](https://pre-commit.com/) com o hook
[`jenkinsfilelint`](https://github.com/jenkinsci/jenkinsfilelint) para validar a sintaxe dos
`jenkinsfile-*` e dos `.groovy` da `SharedLibrary/` contra o Jenkins real antes de cada commit.

> O `jenkinsfilelint` não faz parsing offline: ele manda o conteúdo do pipeline pro endpoint
> `/pipeline-model-converter/validate` de uma instância Jenkins de verdade e devolve o erro de
> sintaxe que o próprio Jenkins reportaria.

## Requisitos

| Requisito | Motivo |
|---|---|
| **Python 3.10+** | O `jenkinsfilelint` (pacote instalado pelo pre-commit) exige `>=3.10` no `pyproject.toml`. Se o `python3` do seu `PATH` for mais antigo, a instalação do hook falha com `Package 'jenkinsfilelint' requires a different Python`. |
| **`pre-commit` instalado** | `pip install pre-commit` (ou `pipx install pre-commit`). |
| **Acesso de rede ao Jenkins** | O hook precisa alcançar a instância Jenkins (VPN/rede interna, se aplicável). |
| **Credenciais do Jenkins** | Variáveis de ambiente `JENKINS_URL`, `JENKINS_USER` e `JENKINS_TOKEN` (ver [Arquivos a configurar](#arquivos-a-configurar)). |

### Checando sua versão de Python

```bash
python3 --version
```

Se for menor que 3.10, instale uma versão mais nova sem afetar o Python padrão do sistema:

```bash
# RHEL / Oracle Linux / CentOS (AppStream ou EPEL)
sudo dnf install -y python3.13

# Debian / Ubuntu
sudo apt install -y python3.13
```

## Instalação

1. Clone o repositório e entre na pasta:
   ```bash
   git clone <url-do-repo>
   cd SME-Pipelines
   ```
2. Instale o `pre-commit` (uma vez por máquina):
   ```bash
   pip install pre-commit
   ```
3. Instale o hook de git no repositório (uma vez por clone):
   ```bash
   pre-commit install
   ```
4. Configure as credenciais do Jenkins (veja a seção abaixo) — **sem isso o hook não roda**.
5. Teste num arquivo antes de confiar no commit automático:
   ```bash
   pre-commit run jenkinsfilelint --files jenkinsfile-sme-airflow
   ```

A partir daqui, todo `git commit` roda o `jenkinsfilelint` automaticamente nos `jenkinsfile-*`/`.groovy`
alterados.

## Arquivos a configurar

### 1. `.pre-commit-config.yaml` (na raiz do repo, já commitado)

```yaml
repos:
  - repo: https://github.com/jenkinsci/jenkinsfilelint
    rev: v1.4.0
    hooks:
      - id: jenkinsfilelint
        # o hook por padrão só casa "Jenkinsfile*" (J maiúsculo); nossos arquivos
        # usam "jenkinsfile-*" minúsculo, por isso o override case-insensitive
        files: (?i)^.*(jenkinsfile.*|.*\.groovy)$
        # jenkinsfilelint exige Python >=3.10; fixamos a versão instalada
        language_version: python3.13
```

Esse arquivo é compartilhado pelo time — não coloque `JENKINS_URL`/`JENKINS_USER`/`JENKINS_TOKEN` aqui
via `args`, mesmo que o hook aceite. Credenciais vão em arquivo local, fora do git (próximo item).

### 2. Credenciais do Jenkins (local, **fora do repositório**, nunca commitado)

Crie um arquivo só seu, com permissão restrita, guardando as 3 variáveis:

```bash
mkdir -p ~/.config/sme-pipelines
cat > ~/.config/sme-pipelines/jenkins.env <<'EOF'
export JENKINS_URL=https://jenkins2.sme.prefeitura.sp.gov.br
export JENKINS_USER=SEU_USUARIO
export JENKINS_TOKEN=SEU_TOKEN
EOF
chmod 600 ~/.config/sme-pipelines/jenkins.env
```

> ⚠️ `JENKINS_URL` precisa do scheme (`https://`). Sem ele o hook falha com
> `Invalid URL '...': No scheme supplied`.

`JENKINS_TOKEN` é gerado em **Jenkins → seu usuário → Configure → API Token**.

### 3. Carregar as credenciais automaticamente em todo terminal novo

Se seu shell é bash e já existe (ou você cria) uma pasta `~/.bashrc.d/` carregada pelo `~/.bashrc`,
basta criar um arquivo lá que só referencia o arquivo de credenciais:

```bash
mkdir -p ~/.bashrc.d
cat > ~/.bashrc.d/jenkins-sme.sh <<'EOF'
[ -f "$HOME/.config/sme-pipelines/jenkins.env" ] && . "$HOME/.config/sme-pipelines/jenkins.env"
EOF
```

Se seu `~/.bashrc` não tiver esse loop, adicione direto nele (ou no `~/.zshrc`, se usar zsh):

```bash
echo '[ -f "$HOME/.config/sme-pipelines/jenkins.env" ] && . "$HOME/.config/sme-pipelines/jenkins.env"' >> ~/.bashrc
```

Depois:

```bash
source ~/.bashrc   # ou abra um terminal novo
echo $JENKINS_URL  # confirma que carregou
```

## Troubleshooting

| Sintoma | Causa | Solução |
|---|---|---|
| `Package 'jenkinsfilelint' requires a different Python: X not in '>=3.10'` | `python3` do PATH é antigo | Instale Python 3.10+ e adicione `language_version: python3.1x` no hook |
| `Invalid URL '...': No scheme supplied` | `JENKINS_URL` sem `https://` | Exporte com o scheme completo |
| `Error connecting to Jenkins` (timeout/DNS) | Rede/VPN sem acesso ao Jenkins, ou URL errada | Confirme conectividade e o valor de `JENKINS_URL` |
| Hook nunca roda / "no files to check" | `files:` do hook não bate com a convenção `jenkinsfile-*` minúsculo | Confirme o override `files: (?i)^.*(jenkinsfile.*|.*\.groovy)$` no `.pre-commit-config.yaml` |
| `pre-commit run --all-files` some tempo/erro em massa | Primeira instalação do venv do hook, ou muitos arquivos sendo validados contra o Jenkins remoto de uma vez | Normal na primeira execução; rode em um arquivo por vez para depurar (`--files caminho`) |
