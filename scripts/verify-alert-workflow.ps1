param([string]$ApiUrl = 'http://localhost:8081')

$ErrorActionPreference = 'Stop'
# Run from the repository root with Kafka, the .NET processor, and Spring already running.
# This creates one uniquely named simulation fixture and retains it for inspection.
$code = 'M3-DEMO-' + [Guid]::NewGuid().ToString('N').Substring(0, 12)
$api = $ApiUrl.TrimEnd('/')

function Send-Telemetry($Event) {
    $json = $Event | ConvertTo-Json -Compress
    "${code}:$json" | wsl sh -lc 'cd /mnt/d/own/plantos && docker compose exec -T kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic plantos.machine.telemetry.v1 --reader-property parse.key=true --reader-property key.separator=:'
    if ($LASTEXITCODE -ne 0) { throw 'Kafka publication failed' }
}

function Wait-ForOccurrences([int]$Expected) {
    $deadline = [DateTime]::UtcNow.AddSeconds(30)
    do {
        $alerts = @(Invoke-RestMethod "$api/api/machines/$code/alerts" | ForEach-Object { $_ })
        if ($alerts.Count -gt 1) { throw 'Expected one incident, found multiple alerts' }
        if ($alerts.Count -eq 1) {
            if ($alerts[0].occurrenceCount -gt $Expected) { throw 'Duplicate replay changed occurrence count' }
            if ($alerts[0].occurrenceCount -eq $Expected) { return $alerts[0] }
        }
        Start-Sleep -Milliseconds 250
    } while ([DateTime]::UtcNow -lt $deadline)
    throw "Timed out waiting for $Expected occurrences. Check Spring and processor logs."
}

$machine = @{ code = $code; name = 'Milestone 3 simulated overheating demo' } | ConvertTo-Json
Invoke-RestMethod "$api/api/machines" -Method Post -ContentType 'application/json' -Body $machine | Out-Null
Invoke-RestMethod "$api/api/machines/$code/status" -Method Patch -ContentType 'application/json' -Body '{"status":"RUNNING"}' | Out-Null

$sourceId = [Guid]::NewGuid().ToString()
$now = [DateTimeOffset]::UtcNow.ToString('O')
$event = [ordered]@{
    eventId = $sourceId; schemaVersion = 1; eventType = 'MachineTelemetry'
    occurredAt = $now; producedAt = $now; machineCode = $code
    correlationId = [Guid]::NewGuid().ToString(); causationId = [Guid]::NewGuid().ToString()
    sequenceNumber = 1; temperatureCelsius = 95.0; vibrationMillimetersPerSecond = 1.0
    powerKilowatts = 10.0; rpm = 1200; unitsProduced = 2
}
Send-Telemetry $event
$alert = Wait-ForOccurrences 1
if ($alert.severity -ne 'CRITICAL') { throw 'Expected critical overheating alert' }
Write-Output "Created critical alert $($alert.id) for $code from raw telemetry."

# Replay the exact raw event twice. The .NET processor derives the same anomaly ID.
Send-Telemetry $event
Send-Telemetry $event
$event.eventId = [Guid]::NewGuid().ToString()
$event.occurredAt = [DateTimeOffset]::UtcNow.ToString('O')
$event.producedAt = $event.occurredAt
$event.sequenceNumber = 2
$event.temperatureCelsius = 96.0
Send-Telemetry $event

# This new event is a same-partition marker: reaching count 2 proves both replays passed through.
$alert = Wait-ForOccurrences 2
$history = @(Invoke-RestMethod "$api/api/alerts/$($alert.id)/history" | ForEach-Object { $_ })
if ($history.Count -ne 2) { throw 'Replay duplicated audit records' }
Write-Output 'Replay passed: four raw deliveries produced one incident, two occurrences, and two audit records.'

Invoke-RestMethod "$api/api/alerts/$($alert.id)/acknowledgements" -Method Post | Out-Null
$resolved = Invoke-RestMethod "$api/api/alerts/$($alert.id)/resolve" -Method Post -ContentType 'application/json' -Body '{"note":"Verified simulated fault and cleared the demo incident"}'
if ($resolved.status -ne 'RESOLVED') { throw 'Alert resolution failed' }
$history = @(Invoke-RestMethod "$api/api/alerts/$($alert.id)/history" | ForEach-Object { $_ })
if ($history.Count -ne 4) { throw 'Expected creation, update, acknowledgement, and resolution history' }

[pscustomobject]@{
    MachineCode = $code; AlertId = $alert.id; Status = $resolved.status
    Occurrences = $resolved.occurrenceCount; AuditRecords = $history.Count
    DetailUrl = "$api/api/alerts/$($alert.id)"
}
