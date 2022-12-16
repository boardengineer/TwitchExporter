package twitchexporter;

import basemod.BaseMod;
import basemod.interfaces.PostInitializeSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.google.gson.JsonObject;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

@SpireInitializer
public class TwitchExporter implements PostInitializeSubscriber {
    public static void initialize() {
        BaseMod.subscribe(new TwitchExporter());
    }

    @Override
    public void receivePostInitialize() {
        new Thread(() -> {
            while (true) {
                try {
                    sendRequest();
                    Thread.sleep(2500);
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    static void sendRequest() throws IOException {
        URL url = new URL("https://boardengineer.net/player/players/1/");
        HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
        httpCon.setDoOutput(true);
        httpCon.setRequestMethod("PUT");
        httpCon.setRequestProperty("Content-Type", "application/json");
        OutputStreamWriter out = new OutputStreamWriter(
                httpCon.getOutputStream());

        JsonObject jsonBody = new JsonObject();

        int playerHp = 0;
        int playerMaxHp = 0;

        if(AbstractDungeon.player != null) {
            playerHp = AbstractDungeon.player.currentHealth;
            playerMaxHp = AbstractDungeon.player.currentHealth;
        }

        jsonBody.addProperty("player_current_hp", playerHp);
        jsonBody.addProperty("twitch_username", "twitchslayssspire");
        jsonBody.addProperty("player_max_hp", playerMaxHp);

        out.write(jsonBody.toString());
        out.close();
        httpCon.getInputStream();
    }
}