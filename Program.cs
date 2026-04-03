using System;
using System.Diagnostics;

class Program {
    static void Main(string[] args) {
        var psi = new ProcessStartInfo();
        psi.FileName = "java";
        psi.Arguments = "-jar SpeedLAN.jar " + string.Join(" ", args);
        psi.UseShellExecute = false;
        Process.Start(psi).WaitForExit();
    }
}
