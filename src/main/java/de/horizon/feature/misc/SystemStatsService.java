package de.horizon.feature.misc;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;

public final class SystemStatsService {
    private final SystemInfo systemInfo = new SystemInfo();
    private final HardwareAbstractionLayer hardware = systemInfo.getHardware();
    private final CentralProcessor processor = hardware.getProcessor();

    private volatile long[] previousTicks = processor.getSystemCpuLoadTicks();
    private volatile long lastUpdate = 0L;
    private volatile double cpuLoad = 0.0D;
    private volatile double cpuTemp = Double.NaN;
    private volatile String gpuName = "";
    private volatile Double gpuUsage = null;
    private volatile Double gpuTemp = null;
    private volatile boolean polling;

    public void requestUpdate() {
        long now = System.currentTimeMillis();
        if (polling || now - lastUpdate < 3000L) {
            return;
        }

        polling = true;
        CompletableFuture.runAsync(() -> {
            try {
                cpuLoad = processor.getSystemCpuLoadBetweenTicks(previousTicks) * 100.0D;
                previousTicks = processor.getSystemCpuLoadTicks();
                double readTemp = hardware.getSensors().getCpuTemperature();
                cpuTemp = readTemp > 0.0D ? readTemp : pollWindowsCpuTemp();
                pollGpu();
                lastUpdate = System.currentTimeMillis();
            } finally {
                polling = false;
            }
        });
    }

    private void pollGpu() {
        try {
            Process process = new ProcessBuilder("nvidia-smi", "--query-gpu=name,utilization.gpu,temperature.gpu", "--format=csv,noheader,nounits")
                .redirectErrorStream(true)
                .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && !line.isBlank()) {
                    String[] parts = line.split(",");
                    gpuName = parts[0].trim();
                    gpuUsage = parts.length > 1 ? parseDouble(parts[1].trim()) : null;
                    gpuTemp = parts.length > 2 ? parseDouble(parts[2].trim()) : null;
                } else {
                    gpuName = "GPU n/a";
                    gpuUsage = null;
                    gpuTemp = null;
                }
            }
        } catch (Exception ignored) {
            if (!hardware.getGraphicsCards().isEmpty()) {
                gpuName = hardware.getGraphicsCards().get(0).getName();
            } else {
                gpuName = "GPU n/a";
            }
            gpuUsage = null;
            gpuTemp = null;
        }
    }

    private double pollWindowsCpuTemp() {
        Double openHardwareMonitor = queryWmiTemperature("root\\OpenHardwareMonitor", "Sensor", "Value", "SensorType='Temperature' AND Name LIKE '%CPU%'");
        if (openHardwareMonitor != null) {
            return openHardwareMonitor;
        }
        Double libreHardwareMonitor = queryWmiTemperature("root\\LibreHardwareMonitor", "Sensor", "Value", "SensorType='Temperature' AND Name LIKE '%CPU%'");
        if (libreHardwareMonitor != null) {
            return libreHardwareMonitor;
        }
        Double acpi = queryWmiTemperature("root\\wmi", "MSAcpi_ThermalZoneTemperature", "CurrentTemperature", null);
        if (acpi != null && acpi > 2000.0D) {
            return (acpi / 10.0D) - 273.15D;
        }
        return Double.NaN;
    }

    private Double queryWmiTemperature(String namespace, String clazz, String property, String where) {
        try {
            String query = "SELECT " + property + " FROM " + clazz + (where == null ? "" : " WHERE " + where);
            Process process = new ProcessBuilder("powershell", "-NoProfile", "-Command",
                "$value = Get-CimInstance -Namespace '" + namespace + "' -Query \"" + query + "\" -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty " + property + "; if ($null -ne $value) { [Console]::WriteLine($value) }")
                .redirectErrorStream(true)
                .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Double parsed = parseDouble(line.trim());
                    if (parsed != null) {
                        process.waitFor(250, TimeUnit.MILLISECONDS);
                        return parsed;
                    }
                }
            }
            process.waitFor(2, TimeUnit.SECONDS);
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public double getCpuLoad() {
        return cpuLoad;
    }

    public double getCpuTemp() {
        return cpuTemp;
    }

    public String getGpuName() {
        return gpuName;
    }

    public Double getGpuUsage() {
        return gpuUsage;
    }

    public Double getGpuTemp() {
        return gpuTemp;
    }
}
