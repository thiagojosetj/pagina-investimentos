#requires -Version 5.1
<#
.SYNOPSIS
Preserva sockets inacessiveis do Docker Desktop em pastas de backup.
.DESCRIPTION
Recuperacao manual para o erro Win32 1920 em sockets locais. Afeta o runtime
global do Docker Desktop, nao apenas este projeto. Feche o Docker e pare seu
servico antes de executar. Nao inicia/encerra processos, apaga arquivos, altera
configuracoes ou acessa discos WSL, containers, imagens e volumes.
Use -WhatIf para inspecionar; uma execucao real pede confirmacao.
#>
[CmdletBinding(SupportsShouldProcess, ConfirmImpact = 'High')]
param()

Set-StrictMode -Version Latest

function Assert-DockerRuntimeStopped {
    $processes = @(Get-Process -ErrorAction Stop | Where-Object {
        $_.ProcessName -match '^(Docker Desktop|DockerCli|docker|com\.docker\..*|Docker Desktop Installer.*)$'
    })
    $services = @(Get-Service -ErrorAction Stop | Where-Object {
        $_.Name -eq 'com.docker.service' -and $_.Status -ne 'Stopped'
    })
    if ($processes.Count -gt 0 -or $services.Count -gt 0) {
        throw 'Docker ainda esta ativo. Feche o Docker Desktop, aguarde os processos terminarem e pare o servico com.docker.service antes de tentar novamente.'
    }
}

function Test-InaccessibleSocketError {
    param([System.Exception] $Exception)

    for ($current = $Exception; $null -ne $current; $current = $current.InnerException) {
        if (($current -is [ComponentModel.Win32Exception] -and $current.NativeErrorCode -eq 1920) -or
            ($current.HResult -band 0xffff) -eq 1920 -or
            $current.Message -match '(?<!\d)1920(?!\d)') {
            return $true
        }
    }
    return $false
}

function Get-DockerRuntimeRecoveryPlan {
    # Nao aceita um diretorio fornecido pelo chamador: os alvos sao fixos.
    $localData = [Environment]::GetFolderPath('LocalApplicationData')
    if ([Environment]::OSVersion.Platform -ne 'Win32NT' -or
        [string]::IsNullOrWhiteSpace($localData)) {
        throw 'Este utilitario requer Windows e a pasta LocalApplicationData do usuario atual.'
    }
    $localData = [IO.Path]::GetFullPath($localData).TrimEnd('\')
    $dockerParent = Join-Path $localData 'Docker'
    $targets = @(
        @{ Path = Join-Path $dockerParent 'run'; Names = @(
            'dockerEthernetVfkit', 'dockerInference', 'sailor-ingest.sock', 'userAnalyticsOtlpHttp.sock'
        ) },
        @{ Path = Join-Path $localData 'docker-secrets-engine'; Names = @('engine.sock') }
    )
    $suffix = '.stale-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '-' +
        [Guid]::NewGuid().ToString('N').Substring(0, 8)
    $plan = @()
    $foundInaccessibleSocket = $false

    # Validar todos os ancestrais e conteudos antes de permitir qualquer renomeacao.
    foreach ($directory in @($localData, $dockerParent)) {
        $item = Get-Item -LiteralPath $directory -Force -ErrorAction Stop
        if (-not $item.PSIsContainer -or
            ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
            throw "Pasta ancestral inesperada ou redirecionada: $directory"
        }
    }
    foreach ($target in $targets) {
        $source = [IO.Path]::GetFullPath($target.Path)
        if (-not $source.StartsWith($localData + '\', [StringComparison]::OrdinalIgnoreCase)) {
            throw "Alvo fora de LocalApplicationData: $source"
        }
        if (-not (Test-Path -LiteralPath $source -ErrorAction Stop)) {
            continue
        }
        $directory = Get-Item -LiteralPath $source -Force -ErrorAction Stop
        if (-not $directory.PSIsContainer -or
            ($directory.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0 -or
            -not [string]::Equals($directory.FullName, $source, [StringComparison]::OrdinalIgnoreCase)) {
            throw "Alvo nao e uma pasta normal no caminho esperado: $source"
        }
        foreach ($entry in @(Get-ChildItem -LiteralPath $source -Force -ErrorAction Stop)) {
            if ($entry.PSIsContainer -or $entry.Name -cnotin $target.Names -or
                $entry.Length -ne 0 -or
                ($entry.Attributes -band [IO.FileAttributes]::ReparsePoint) -eq 0) {
                throw "Conteudo inesperado; nenhuma pasta sera movida: $($entry.FullName)"
            }
            try {
                $null = Get-Acl -LiteralPath $entry.FullName -ErrorAction Stop
            }
            catch {
                if (-not (Test-InaccessibleSocketError -Exception $_.Exception)) {
                    throw
                }
                $foundInaccessibleSocket = $true
            }
        }
        $destination = $source + $suffix
        if (Test-Path -LiteralPath $destination -ErrorAction Stop) {
            throw "Backup ja existe; nenhuma pasta sera movida: $destination"
        }
        $plan += [pscustomobject]@{ Source = $source; Destination = $destination }
    }
    if (-not $foundInaccessibleSocket) {
        throw 'Nenhum socket com erro Win32 1920 foi confirmado. Nenhuma pasta sera movida; investigue o diagnostico antes de tentar outro reparo.'
    }
    return $plan
}

function Invoke-DockerRuntimeRepair {
    [CmdletBinding(SupportsShouldProcess, ConfirmImpact = 'High')]
    param()

    $ErrorActionPreference = 'Stop'
    Write-Warning 'Este reparo afeta o runtime GLOBAL do Docker Desktop. Use apenas para sockets inacessiveis (erro 1920), com Docker totalmente parado. Nada sera excluido.'
    Assert-DockerRuntimeStopped
    $plan = @(Get-DockerRuntimeRecoveryPlan)
    $description = ($plan.Source -join ', ')
    if (-not $PSCmdlet.ShouldProcess($description, 'Renomear pastas de sockets para backups .stale-* no mesmo diretorio')) {
        return
    }

    # O usuario pode demorar na confirmacao: refazer todas as verificacoes.
    Assert-DockerRuntimeStopped
    $plan = @(Get-DockerRuntimeRecoveryPlan)
    foreach ($item in $plan) {
        try {
            Move-Item -LiteralPath $item.Source -Destination $item.Destination -ErrorAction Stop
            Write-Output "Preservado: $($item.Source) -> $($item.Destination)"
        }
        catch {
            Write-Warning 'Reparo interrompido. Renomeacoes ja concluidas permanecem preservadas nos backups informados; nao houve exclusao. Nao tente restaurar sobre pastas recriadas pelo Docker.'
            throw
        }
    }
    Write-Output 'Abra o Docker Desktop manualmente e verifique o resultado. Este reparo nao garante que o erro nao reapareca.'
}

# Dot-sourcing carrega somente as funcoes para os testes sinteticos.
if ($MyInvocation.InvocationName -ne '.') {
    Invoke-DockerRuntimeRepair @PSBoundParameters
}
