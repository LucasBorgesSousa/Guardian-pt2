# Copie para .local/configurar.ps1 e preencha a senha do usuário MySQL.
# Não publique senhas no GitHub.
$env:SAFETAG_DB_URL = 'jdbc:mysql://127.0.0.1:3306/safetag?connectionTimeZone=UTC&connectTimeout=5000&socketTimeout=5000&useAffectedRows=false'
$env:SAFETAG_DB_USER = 'safetag_app'
$env:SAFETAG_DB_PASSWORD = 'PREENCHA_LOCALMENTE'
