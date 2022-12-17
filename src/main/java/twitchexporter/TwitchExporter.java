package twitchexporter;

import basemod.BaseMod;
import basemod.interfaces.PostInitializeSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

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
        httpCon.setRequestProperty("Accept", "application/json");
        OutputStreamWriter out = new OutputStreamWriter(
                httpCon.getOutputStream());

        JsonObject jsonBody = new JsonObject();

        int playerHp = 0;
        int playerMaxHp = 0;
        JsonArray relicsArray = new JsonArray();

        if (AbstractDungeon.player != null) {
            playerHp = AbstractDungeon.player.currentHealth;
            playerMaxHp = AbstractDungeon.player.currentHealth;

            // TODO: relics have a bad id system, need to fix that in the mean time we'll just use
            // exisitng relics
            int relicIndex = 1;
            int relicEndIndex = 4;

            for (AbstractRelic relic : AbstractDungeon.player.relics) {
                if (relicIndex > relicEndIndex) {
                    break;
                }

                JsonObject relicJson = new JsonObject();

                // TODO: THIS THING SHOULD NOT BE HERE
                relicJson.addProperty("id", relicIndex);

                relicJson.addProperty("name", relic.name);
                relicJson.addProperty("description", relic.description);
                relicJson.addProperty("x_pos", relic.hb.x);
                relicJson.addProperty("y_pos", relic.hb.y);

                relicIndex++;
                relicsArray.add(relicJson);
            }
        }

        jsonBody.addProperty("player_current_hp", playerHp);
        jsonBody.addProperty("twitch_username", "twitchslayssspire");
        jsonBody.addProperty("player_max_hp", playerMaxHp);

        jsonBody.add("relics", relicsArray);

        out.write(jsonBody.toString());
        out.close();
        httpCon.getInputStream();
    }
}