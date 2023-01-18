package twitchexporter;

import basemod.BaseMod;
import basemod.interfaces.PostInitializeSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.map.MapEdge;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.relics.AbstractRelic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

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
        URL url = new URL("https://boardengineer.net/player/");
        HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
        httpCon.setDoOutput(true);
        httpCon.setRequestMethod("PUT");
        httpCon.setRequestProperty("Content-Type", "application/json");
        httpCon.setRequestProperty("Accept", "application/json");
        httpCon.setRequestProperty("Authorization", "Token " + "4d014333df73dd618096766e621973ed3b72442a");
        OutputStreamWriter out = new OutputStreamWriter(
                httpCon.getOutputStream());

        JsonObject jsonBody = new JsonObject();

        int playerHp = 0;
        int playerMaxHp = 0;
        JsonArray relicsArray = new JsonArray();

        if (AbstractDungeon.player != null) {
            playerHp = AbstractDungeon.player.currentHealth;
            playerMaxHp = AbstractDungeon.player.currentHealth;

            for (AbstractRelic relic : AbstractDungeon.player.relics) {
                JsonObject relicJson = new JsonObject();

                // TODO: THIS THING SHOULD NOT BE HERE
                relicJson.addProperty("id", 1);

                relicJson.addProperty("name", relic.name);

                String description = relic.description;

                description = description.replace("#y", "");
                description = description.replace("#b", "");
                description = description.replace("NL", "");

                relicJson.addProperty("description", description);
                relicJson.addProperty("x_pos", relic.hb.x);
                relicJson.addProperty("y_pos", relic.hb.y);
                relicJson.addProperty("width", relic.hb.width);
                relicJson.addProperty("height", relic.hb.height);

                relicsArray.add(relicJson);
            }
        }
        jsonBody.add("relics", relicsArray);

        float mapButtonX = 0;
        float mapButtonY = 0;
        float mapButtonHeight = 0;
        float mapButtonWidth = 0;
        try {
            Hitbox mapHitbox = AbstractDungeon.topPanel.mapHb;

            mapButtonX = mapHitbox.x;
            mapButtonY = mapHitbox.y;
            mapButtonHeight = mapHitbox.height;
            mapButtonWidth = mapHitbox.width;
        } catch (NullPointerException e) {
            // This is fine, the game's not initiated yet
        }
        jsonBody.addProperty("map_button_x", mapButtonX);
        jsonBody.addProperty("map_button_y", mapButtonY);
        jsonBody.addProperty("map_button_height", mapButtonHeight);
        jsonBody.addProperty("map_button_width", mapButtonWidth);

        float deckButtonX = 0;
        float deckButtonY = 0;
        float deckButtonHeight = 0;
        float deckButtonWidth = 0;
        try {
            Hitbox deckHitbox = AbstractDungeon.topPanel.deckHb;

            deckButtonX = deckHitbox.x;
            deckButtonY = deckHitbox.y;
            deckButtonHeight = deckHitbox.height;
            deckButtonWidth = deckHitbox.width;
        } catch (NullPointerException e) {
            // This is fine, the game's not initiated yet
        }
        jsonBody.addProperty("deck_button_x", deckButtonX);
        jsonBody.addProperty("deck_button_y", deckButtonY);
        jsonBody.addProperty("deck_button_height", deckButtonHeight);
        jsonBody.addProperty("deck_button_width", deckButtonWidth);

        JsonArray deckArray = new JsonArray();
        try {
            for (AbstractCard card : AbstractDungeon.player.masterDeck.group) {
                JsonObject cardJson = new JsonObject();

                cardJson.addProperty("name", card.name);
                cardJson.addProperty("description", descriptionForCard(card));

                deckArray.add(cardJson);
            }
        } catch (NullPointerException e) {
            // No deck but it's probably okay.
        }

        jsonBody.add("deck", deckArray);

        jsonBody.addProperty("user", 1);

        jsonBody.addProperty("player_current_hp", playerHp);
        jsonBody.addProperty("player_max_hp", playerMaxHp);

        jsonBody.addProperty("screen_height", Settings.HEIGHT);
        jsonBody.addProperty("screen_width", Settings.WIDTH);

        jsonBody.addProperty("boss_name", AbstractDungeon.bossKey == null ? "" : AbstractDungeon.bossKey);

        JsonArray mapNodes = new JsonArray();
        JsonArray mapEdges = new JsonArray();

        try {
            for (ArrayList<MapRoomNode> list : AbstractDungeon.map) {
                for (MapRoomNode node : list) {
                    if (!node.getEdges().isEmpty()) {
                        JsonObject jsonNode = new JsonObject();

                        jsonNode.addProperty("x", node.x);
                        jsonNode.addProperty("y", node.y);

                        jsonNode.addProperty("offset_x", node.offsetX);
                        jsonNode.addProperty("offset_y", node.offsetY);

                        jsonNode.addProperty("symbol", node.getRoomSymbol(true));

                        mapNodes.add(jsonNode);

                        for (MapEdge edge : node.getEdges()) {
                            int source = edge.srcX + edge.srcY * 7;
                            int destination = edge.dstX + edge.dstY * 7;

                            JsonObject edgeJson = new JsonObject();

                            edgeJson.addProperty("source", source);
                            edgeJson.addProperty("destination", destination);

                            mapEdges.add(edgeJson);
                        }
                    }
                }
            }
        } catch (NullPointerException e) {
            System.out.println("no map");
        }

        jsonBody.add("map_nodes", mapNodes);
        jsonBody.add("map_edges", mapEdges);


        out.write(jsonBody.toString());
//        System.out.println(jsonBody.toString());
        out.close();
        try {
            httpCon.getInputStream();
        } catch (IOException e) {
            System.err.println(httpCon.getResponseCode());
            System.err.println(httpCon.getResponseMessage());
            BufferedReader br = new BufferedReader(new InputStreamReader(httpCon.getErrorStream()));
            String strCurrentLine;
            while ((strCurrentLine = br.readLine()) != null) {
                System.out.println(strCurrentLine);
            }
        }
    }

    private static String descriptionForCard(AbstractCard card) {
        String result = card.rawDescription;

        result = result.replaceAll("!B!", Integer.toString(card.baseBlock));
        result = result.replaceAll("!D!", Integer.toString(card.baseDamage));
        result = result.replaceAll("!M!", Integer.toString(card.baseMagicNumber));

        return result;
    }
}