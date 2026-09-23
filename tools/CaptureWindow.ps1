param([Parameter(Mandatory=$true)][string]$OutputFile)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type @'
using System;
using System.Runtime.InteropServices;
public class WindowCapture {
    [DllImport("user32.dll")]
    public static extern bool SetProcessDPIAware();
    [DllImport("user32.dll", CharSet=CharSet.Unicode)]
    public static extern IntPtr FindWindow(string cls, string title);
    [DllImport("user32.dll")]
    public static extern bool GetWindowRect(IntPtr h, out RECT r);
    [DllImport("user32.dll")]
    public static extern bool PrintWindow(IntPtr h, IntPtr dc, uint flags);
    public struct RECT { public int Left, Top, Right, Bottom; }
}
'@
[void][WindowCapture]::SetProcessDPIAware()
$viewer = Get-Process java | Where-Object { $_.MainWindowTitle -eq 'Java Networking Practice | Source & execution evidence' } | Select-Object -First 1
$window = if ($viewer) { $viewer.MainWindowHandle } else { [IntPtr]::Zero }
if ($window -eq [IntPtr]::Zero) { throw 'Viewer window not found' }
$rect = New-Object WindowCapture+RECT
[void][WindowCapture]::GetWindowRect($window, [ref]$rect)
$bitmap = New-Object System.Drawing.Bitmap(($rect.Right - $rect.Left), ($rect.Bottom - $rect.Top))
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$dc = $graphics.GetHdc()
try {
    if (-not [WindowCapture]::PrintWindow($window, $dc, 2)) { throw 'Window capture failed' }
} finally { $graphics.ReleaseHdc($dc) }
try { $bitmap.Save($OutputFile, [System.Drawing.Imaging.ImageFormat]::Png) }
finally { $graphics.Dispose(); $bitmap.Dispose() }
