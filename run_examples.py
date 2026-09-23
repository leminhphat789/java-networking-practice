from pathlib import Path
import queue
import re
import subprocess
import threading
import time

ROOT = Path(__file__).resolve().parent
LOGS = ROOT / "results"
JAVA = ["java", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8",
        "-Dstderr.encoding=UTF-8", "-cp", "build/classes"]


def save(name, command, result):
    output = re.sub(r"(?m)^Host Name: .*", "Host Name: <REDACTED_HOSTNAME>", result.stdout, count=1)
    output = re.sub(r"(?m)^IP Address: .*", "IP Address: <REDACTED_PRIVATE_IP>", output, count=1)
    text = "$ " + subprocess.list2cmdline(command) + "\n\n" + output
    text += f"\n[exit code: {result.returncode}]\n"
    (LOGS / name).write_text(text, encoding="utf-8")


def run(name, class_name, *args):
    command = JAVA + [class_name, *args]
    result = subprocess.run(command, cwd=ROOT, capture_output=True, text=True,
                            encoding="utf-8", errors="replace", timeout=45)
    result.stdout += result.stderr
    save(name, command, result)
    if result.returncode or "Exception" in result.stdout:
        raise RuntimeError(f"{class_name} failed; see results/{name}")
    return result.stdout


def main():
    LOGS.mkdir(exist_ok=True)
    (ROOT / "build/classes").mkdir(parents=True, exist_ok=True)
    command = ["javac", "-encoding", "UTF-8", "-Xlint:all", "-d", "build/classes"]
    command += [str(p.relative_to(ROOT)) for p in sorted((ROOT / "src").rglob("*.java"))]
    build = subprocess.run(command, cwd=ROOT, capture_output=True, text=True, encoding="utf-8")
    build.stdout += build.stderr
    save("00-build.txt", command, build)
    if build.returncode:
        raise RuntimeError(build.stdout)
    checks = ["PASS compilation (original URL constructors emit deprecation warnings on Java 21)"]
    output = run("01-url.txt", "com.gpcoder.net.UrlExample")
    assert "port : 80" in output and "default port : 443" in output
    assert "query : page=1&amp;amp;amp;order=desc" in output
    assert "ref : java-core" in output
    checks.append("PASS UrlExample: components match the article sample")
    output = run("02-http-online.txt", "com.gpcoder.net.URLConnectionExample")
    assert "<html" in output.lower() and "W3Schools" in output
    checks.append("PASS URLConnectionExample: actual W3Schools HTML received")
    output = run("03-dns-online.txt", "com.gpcoder.net.InetAddressExample")
    assert "www.studytonight.com" in output and "www.google.com/" in output
    checks.append("PASS InetAddressExample: local host and public DNS resolved")
    command = JAVA + ["vn.viettuts.server.ServerExample", "0"]
    server = subprocess.Popen(command, cwd=ROOT, stdout=subprocess.PIPE,
                              stderr=subprocess.STDOUT, text=True, encoding="utf-8")
    messages = queue.Queue()
    threading.Thread(target=lambda: messages.put(server.stdout.readline()), daemon=True).start()
    try:
        first = messages.get(timeout=15)
        match = re.search(r"Waiting for client on port (\d+)", first)
        if not match:
            raise RuntimeError(first)
        client = run("05-tcp-client.txt", "vn.viettuts.client.ClientExample", "127.0.0.1", match[1])
        remaining, _ = server.communicate(timeout=40)
        result = subprocess.CompletedProcess(command, server.returncode, first + remaining)
        save("04-tcp-server.txt", command, result)
        assert server.returncode == 0
        assert "Hello from /127.0.0.1:" in result.stdout
        assert "Socket timed out!" in result.stdout
        assert "Server says Thank you for connecting to /127.0.0.1:" in client
        assert "Goodbye!" in client
        checks.append("PASS ServerExample: received greeting and exited after its 30-second timeout")
        checks.append("PASS ClientExample: received server response and Goodbye")
    finally:
        if server.poll() is None:
            server.kill()
            server.wait()
    summary = "Run time: " + time.strftime("%Y-%m-%d %H:%M:%S %z") + "\n"
    summary += "\n".join(checks) + "\nALL CHECKS PASSED\n"
    (LOGS / "summary.txt").write_text(summary, encoding="utf-8")
    print(summary)


if __name__ == "__main__":
    main()
