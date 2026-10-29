param(
    [string]$OutDir = "$PSScriptRoot\..\app\src\main\res\raw"
)

$sampleRate = 22050
if (-not (Test-Path $OutDir)) { New-Item -ItemType Directory -Path $OutDir | Out-Null }

function New-Silence([int]$count) {
    return New-Object 'double[]' $count
}

function Add-Tone {
    param(
        [double[]]$Buffer,
        [double]$Freq,
        [double]$StartSec,
        [double]$DurSec,
        [double]$Amp = 0.5,
        [double]$Decay = 6.0
    )
    $start = [int]($StartSec * $sampleRate)
    $n = [int]($DurSec * $sampleRate)
    for ($i = 0; $i -lt $n -and ($start + $i) -lt $Buffer.Length; $i++) {
        $t = $i / [double]$sampleRate
        $env = [Math]::Exp(-$Decay * $t)
        $attack = [Math]::Min(1.0, $i / 80.0)
        $Buffer[$start + $i] += $Amp * $env * $attack * [Math]::Sin(2 * [Math]::PI * $Freq * $t)
    }
}

function Add-Noise {
    param(
        [double[]]$Buffer,
        [double]$StartSec,
        [double]$DurSec,
        [double]$Amp = 0.4,
        [double]$Decay = 12.0,
        [int]$Seed = 12345
    )
    $start = [int]($StartSec * $sampleRate)
    $n = [int]($DurSec * $sampleRate)
    $rnd = New-Object System.Random $Seed
    $prev = 0.0
    for ($i = 0; $i -lt $n -and ($start + $i) -lt $Buffer.Length; $i++) {
        $t = $i / [double]$sampleRate
        $env = [Math]::Exp(-$Decay * $t)
        $white = ($rnd.NextDouble() * 2.0) - 1.0
        $prev = $prev * 0.65 + $white * 0.35
        $Buffer[$start + $i] += $Amp * $env * $prev
    }
}

function Save-Wav {
    param([double[]]$Buffer, [string]$Path)
    $stream = [System.IO.File]::Open($Path, [System.IO.FileMode]::Create)
    $writer = New-Object System.IO.BinaryWriter $stream
    try {
        $dataSize = $Buffer.Length * 2
        $writer.Write([System.Text.Encoding]::ASCII.GetBytes("RIFF"))
        $writer.Write([int](36 + $dataSize))
        $writer.Write([System.Text.Encoding]::ASCII.GetBytes("WAVE"))
        $writer.Write([System.Text.Encoding]::ASCII.GetBytes("fmt "))
        $writer.Write([int]16)
        $writer.Write([int16]1)
        $writer.Write([int16]1)
        $writer.Write([int]$sampleRate)
        $writer.Write([int]($sampleRate * 2))
        $writer.Write([int16]2)
        $writer.Write([int16]16)
        $writer.Write([System.Text.Encoding]::ASCII.GetBytes("data"))
        $writer.Write([int]$dataSize)
        foreach ($v in $Buffer) {
            $clamped = [Math]::Max(-1.0, [Math]::Min(1.0, $v))
            $writer.Write([int16]($clamped * 32000))
        }
    } finally {
        $writer.Close()
        $stream.Close()
    }
}

# move: soft wooden click
$buf = New-Silence ([int]($sampleRate * 0.18))
Add-Noise $buf 0.0 0.04 0.55 38 7
Add-Tone $buf 660 0.005 0.09 0.35 28
Add-Tone $buf 430 0.01 0.08 0.22 24
Save-Wav $buf (Join-Path $OutDir "move.wav")

# capture: heavier knock
$buf = New-Silence ([int]($sampleRate * 0.32))
Add-Noise $buf 0.0 0.09 0.75 22 11
Add-Tone $buf 180 0.0 0.18 0.5 16
Add-Tone $buf 95 0.005 0.22 0.35 12
Save-Wav $buf (Join-Path $OutDir "capture.wav")

# check: two clear tones
$buf = New-Silence ([int]($sampleRate * 0.5))
Add-Tone $buf 880 0.0 0.14 0.4 10
Add-Tone $buf 1320 0.13 0.18 0.4 9
Save-Wav $buf (Join-Path $OutDir "check.wav")

# win: rising arpeggio
$buf = New-Silence ([int]($sampleRate * 0.8))
$notes = @(523.25, 659.25, 783.99, 1046.5)
for ($i = 0; $i -lt $notes.Count; $i++) {
    Add-Tone $buf $notes[$i] ($i * 0.14) 0.32 0.38 5
}
Save-Wav $buf (Join-Path $OutDir "win.wav")

# lose: falling tones
$buf = New-Silence ([int]($sampleRate * 0.9))
$notes = @(440.0, 349.23, 261.63)
for ($i = 0; $i -lt $notes.Count; $i++) {
    Add-Tone $buf $notes[$i] ($i * 0.2) 0.4 0.36 4
}
Save-Wav $buf (Join-Path $OutDir "lose.wav")

Get-ChildItem $OutDir | Select-Object Name, Length
