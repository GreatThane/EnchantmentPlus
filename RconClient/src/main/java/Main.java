import com.github.t9t.minecraftrconclient.RconClient;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        try (RconClient client = RconClient.open("localhost", 25575, "minecraft")) {
            client.sendCommand("reload confirm");
        } catch (Exception ignored) {
            System.out.println("WARN: Server not running. Reload command not sent.");
        }
    }
}
