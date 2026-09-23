import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;

public class PracticeViewer {
    private static JFrame frame;
    private static JTextArea source;
    private static JTextArea output;
    private static JLabel title;

    private static JTextArea area() {
        JTextArea text = new JTextArea();
        text.setEditable(false);
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        text.setBackground(new Color(20, 27, 39));
        text.setForeground(new Color(222, 232, 244));
        text.setMargin(new Insets(16, 16, 16, 16));
        text.setTabSize(4);
        return text;
    }

    public static void main(String[] args) throws Exception {
        String[][] examples = {
            {"01-url", "com/gpcoder/net/UrlExample", "01-url.txt"},
            {"02-urlconnection", "com/gpcoder/net/URLConnectionExample", "02-http-online.txt"},
            {"03-inetaddress", "com/gpcoder/net/InetAddressExample", "03-dns-online.txt"},
            {"04-tcp-server", "vn/viettuts/server/ServerExample", "04-tcp-server.txt"},
            {"04b-tcp-server", "vn/viettuts/server/ServerExample", "04-tcp-server.txt"},
            {"05-tcp-client", "vn/viettuts/client/ClientExample", "05-tcp-client.txt"}
        };
        SwingUtilities.invokeAndWait(() -> {
            frame = new JFrame("Java Networking Practice | Source & execution evidence");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            source = area();
            source.setLineWrap(true);
            output = area();
            output.setLineWrap(true);
            output.setWrapStyleWord(true);
            title = new JLabel("  Java Networking Practice");
            title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            title.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    new JScrollPane(source), new JScrollPane(output));
            split.setResizeWeight(0.61);
            frame.add(title, BorderLayout.NORTH);
            frame.add(split, BorderLayout.CENTER);
            JLabel footer = new JLabel("  Java Swing viewer | Left: source file | Right: saved stdout from actual Java processes");
            footer.setBorder(BorderFactory.createEmptyBorder(10, 4, 10, 4));
            frame.add(footer, BorderLayout.SOUTH);
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            frame.setSize(Math.min(1550, screen.width - 60), Math.min(1000, screen.height - 80));
            frame.setLocationRelativeTo(null);
            frame.setAlwaysOnTop(true);
            frame.setVisible(true);
        });
        Files.createDirectories(Path.of("screenshot"));
        Robot robot = new Robot();
        for (String[] example : examples) {
            String code = Files.readString(Path.of("src/" + example[1] + ".java"));
            String log = Files.readString(Path.of("results/" + example[2]), StandardCharsets.UTF_8);
            StringBuilder numbered = new StringBuilder();
            String[] lines = code.split("\n");
            for (int i = 0; i < lines.length; i++) {
                numbered.append(String.format("%2d  %s%n", i + 1, lines[i]));
            }
            SwingUtilities.invokeAndWait(() -> {
                title.setText("  " + Path.of(example[1]).getFileName() + ".java   |   Java 21   |   results/" + example[2]);
                source.setText(numbered.toString());
                output.setText(log);
                source.setCaretPosition(0);
                if (example[0].equals("04b-tcp-server")) {
                    source.setCaretPosition(source.getText().length());
                }
                output.setCaretPosition(0);
                frame.toFront();
            });
            robot.delay(1200);
            Rectangle[] bounds = new Rectangle[1];
            SwingUtilities.invokeAndWait(() -> bounds[0] = frame.getBounds());
            if (System.getProperty("os.name").startsWith("Windows")) {
                Process capture = new ProcessBuilder("powershell", "-NoProfile", "-File",
                        "tools/CaptureWindow.ps1", "-OutputFile",
                        Path.of("screenshot/" + example[0] + ".png").toAbsolutePath().toString())
                        .inheritIO().start();
                if (capture.waitFor() != 0) {
                    throw new IllegalStateException("Window capture failed");
                }
            } else {
                ImageIO.write(robot.createScreenCapture(bounds[0]), "png",
                        Path.of("screenshot/" + example[0] + ".png").toFile());
            }
            System.out.println("Captured screenshot/" + example[0] + ".png");
        }
        SwingUtilities.invokeAndWait(() -> frame.dispose());
    }
}
