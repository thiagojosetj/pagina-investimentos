#requires -Version 5.1
# Testes sem Pester, sem acesso ao runtime: operacoes de sistema sao simuladas.
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$utility = Join-Path $PSScriptRoot 'repair-docker-runtime.ps1'
$tokens = $null
$parseErrors = $null
$null = [Management.Automation.Language.Parser]::ParseFile($utility, [ref] $tokens, [ref] $parseErrors)
if ($parseErrors.Count -ne 0) { throw "Erro de sintaxe: $parseErrors" }
. $utility

$localData = [Environment]::GetFolderPath('LocalApplicationData').TrimEnd('\')
$dockerParent = Join-Path $localData 'Docker'
$runDirectory = Join-Path $dockerParent 'run'
$secretsDirectory = Join-Path $localData 'docker-secrets-engine'
$script:checks = 0

function Reset-Fixture {
    $script:activeProcess = $false
    $script:activeService = $false
    $script:processChecks = 0
    $script:startBeforeMove = $false
    $script:directoryReads = 0
    $script:changeBeforeMove = $false
    $script:badParent = $false
    $script:socketError = 1920
    $script:moves = @()
    $script:entries = @{}
    foreach ($directory in @($runDirectory, $secretsDirectory)) {
        $name = if ($directory -eq $runDirectory) { 'sailor-ingest.sock' } else { 'engine.sock' }
        $script:entries[$directory] = @([pscustomobject]@{
            Name = $name; FullName = Join-Path $directory $name
            PSIsContainer = $false; Length = 0; Attributes = [IO.FileAttributes]::ReparsePoint
        })
    }
}

# Os mocks deliberadamente nao chamam os cmdlets reais.
function Get-Process {
    param($ErrorAction)
    $script:processChecks++
    if ($script:activeProcess -or ($script:startBeforeMove -and $script:processChecks -gt 1)) {
        [pscustomobject]@{ ProcessName = 'com.docker.backend' }
    }
}
function Get-Service {
    param($ErrorAction)
    if ($script:activeService) { [pscustomobject]@{ Name = 'com.docker.service'; Status = 'Running' } }
}
function Get-Item {
    param($LiteralPath, [switch] $Force, $ErrorAction)
    if ($LiteralPath -notin @($localData, $dockerParent, $runDirectory, $secretsDirectory)) {
        throw "Leitura inesperada: $LiteralPath"
    }
    $attributes = [IO.FileAttributes]::Directory
    if ($script:badParent -and $LiteralPath -eq $dockerParent) {
        $attributes = $attributes -bor [IO.FileAttributes]::ReparsePoint
    }
    [pscustomobject]@{ FullName = $LiteralPath; PSIsContainer = $true; Attributes = $attributes }
}
function Test-Path {
    param($LiteralPath, $ErrorAction)
    return $LiteralPath -in @($runDirectory, $secretsDirectory)
}
function Get-ChildItem {
    param($LiteralPath, [switch] $Force, $ErrorAction)
    if (-not $script:entries.ContainsKey($LiteralPath)) { throw 'Enumeracao fora dos alvos.' }
    $script:directoryReads++
    if ($script:changeBeforeMove -and $script:directoryReads -gt 2) {
        $script:entries[$LiteralPath][0].Length = 10
    }
    return $script:entries[$LiteralPath]
}
function Get-Acl {
    param($LiteralPath, $ErrorAction)
    if ($script:socketError -ne 0) {
        throw [ComponentModel.Win32Exception]::new($script:socketError)
    }
}
function Move-Item {
    param($LiteralPath, $Destination, $ErrorAction)
    if ($LiteralPath -notin @($runDirectory, $secretsDirectory) -or
        (Split-Path $LiteralPath) -ne (Split-Path $Destination) -or
        -not $Destination.StartsWith($LiteralPath + '.stale-')) {
        throw 'Tentativa de renomeacao fora dos alvos e backups permitidos.'
    }
    $script:moves += [pscustomobject]@{ Source = $LiteralPath; Destination = $Destination }
}

function Assert-True {
    param([bool] $Condition, [string] $Message)
    if (-not $Condition) { throw $Message }
    $script:checks++
}
function Assert-Refused {
    param([string] $Name, [string] $ExpectedMessage)
    $errorMessage = ''
    try { Invoke-DockerRuntimeRepair -Confirm:$false -WarningAction SilentlyContinue | Out-Null }
    catch { $errorMessage = $_.Exception.Message }
    Assert-True ($errorMessage -match $ExpectedMessage) "$Name nao recusado corretamente: $errorMessage"
    Assert-True ($script:moves.Count -eq 0) "$Name realizou uma renomeacao."
}

Reset-Fixture
$script:activeProcess = $true
Assert-Refused 'Processo ativo' 'Docker ainda esta ativo'
Reset-Fixture
$script:activeService = $true
Assert-Refused 'Servico ativo' 'Docker ainda esta ativo'
Reset-Fixture
$script:badParent = $true
Assert-Refused 'Ancestral redirecionado' 'ancestral inesperada'
Reset-Fixture
$script:socketError = 0
Assert-Refused 'Socket acessivel' 'Nenhum socket com erro Win32 1920'
Reset-Fixture
$script:socketError = 5
Assert-Refused 'Outro erro de ACL' ([regex]::Escape([ComponentModel.Win32Exception]::new(5).Message))
Assert-True (Test-InaccessibleSocketError ([InvalidOperationException]::new('Method failed with unexpected error code 1920.'))) 'Formato real do erro 1920 nao reconhecido.'
Reset-Fixture
$script:startBeforeMove = $true
Assert-Refused 'Processo iniciado antes da mutacao' 'Docker ainda esta ativo'
Reset-Fixture
$script:changeBeforeMove = $true
Assert-Refused 'Conteudo modificado antes da mutacao' 'Conteudo inesperado'
foreach ($mutation in @('unknown', 'directory', 'nonempty', 'ordinary')) {
    Reset-Fixture
    # Invalido no SEGUNDO alvo: garante pre-validacao antes de mover o primeiro.
    $entry = $script:entries[$secretsDirectory][0]
    switch ($mutation) {
        'unknown' { $entry.Name = 'unrelated.txt' }
        'directory' { $entry.PSIsContainer = $true }
        'nonempty' { $entry.Length = 10 }
        'ordinary' { $entry.Attributes = [IO.FileAttributes]::Normal }
    }
    Assert-Refused $mutation 'Conteudo inesperado'
}
Reset-Fixture
Invoke-DockerRuntimeRepair -WhatIf -WarningAction SilentlyContinue
Assert-True ($script:moves.Count -eq 0) '-WhatIf realizou uma renomeacao.'
Reset-Fixture
Invoke-DockerRuntimeRepair -Confirm:$false -WarningAction SilentlyContinue | Out-Null
Assert-True ($script:moves.Count -eq 2) 'Fluxo aprovado nao renomeou exatamente as duas pastas simuladas.'
Assert-True ($script:moves[0].Destination -ne $script:moves[1].Destination) 'Backups conflitantes.'
Write-Output "PASS: $script:checks verificacoes sinteticas; nenhum cmdlet real de runtime foi executado."
