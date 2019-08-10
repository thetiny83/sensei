package com.github.zastrixarundell.torambot.commands.search.items.extra;

import com.github.zastrixarundell.torambot.Parser;
import com.github.zastrixarundell.torambot.Values;
import com.github.zastrixarundell.torambot.objects.Item;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class UpgradeCommand implements MessageCreateListener
{

    private static final int sizeOfPage = 2000;

    private Document document = null;

    public UpgradeCommand()
    {
        try
        {
            document = Jsoup.connect("http://coryn.club/item.php")
                    .data("special", "xtal")
                    .data("show", String.valueOf(sizeOfPage))
                    .get();
        }
        catch (Exception ignore)
        {

        }
    }

    @Override
    public void onMessageCreate(MessageCreateEvent messageCreateEvent)
    {

        //Cancel if the sender is a bot
        if (!messageCreateEvent.getMessageAuthor().isRegularUser())
            return;

        //Cancel if the command is not <prefix>item
        if (!messageCreateEvent.getMessageContent().toLowerCase().startsWith(Values.getPrefix() + "upgrade"))
            if (!messageCreateEvent.getMessageContent().toLowerCase().startsWith(Values.getPrefix() + "enhance"))
                return;

        ArrayList<String> arguments = Parser.argumentsParser(messageCreateEvent);

        if (arguments.isEmpty())
        {
            emptySearch(messageCreateEvent);
            return;
        }

        String data = String.join(" ", arguments);

        Runnable runnable;
        runnable = () ->
        {
            try
            {
                if(document == null)
                    document = Jsoup.connect("http://coryn.club/item.php")
                            .data("special", "xtal")
                            .data("show", String.valueOf(sizeOfPage))
                            .get();

                Element table = document.getElementsByClass("table table-striped").first();
                Element body = table.getElementsByTag("tbody").first();

                List<Item> itemList = getItems(body, data);

                if(itemList.isEmpty())
                {
                    noResults(messageCreateEvent);
                    return;
                }

                itemList.forEach(item -> sendItemEmbed(item, messageCreateEvent));
            }
            catch (Exception e)
            {
                sendErrorMessage(messageCreateEvent);
                document = null;
            }
        };

        (new Thread(runnable)).start();
    }

    private ArrayList<Item> getItems(Element body, String data)
    {

        Elements trs = body.getElementsByTag("tr");

        ArrayList<Item> listOfItems = new ArrayList<>();

        for(int size = 0, count = 0; size < trs.size() && count < 5; size++)
            if(trs.get(size).parent() == body)
            {
                Item item = new Item(trs.get(size));

                if(String.join("\n", item.getStats()).toLowerCase().contains("upgrade for: " + data.toLowerCase()))
                {
                    listOfItems.add(item);
                    count++;
                }
            }

        return listOfItems;
    }

    private void sendItemEmbed(Item item, MessageCreateEvent messageCreateEvent)
    {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(item.getName())
                .addInlineField("NPC sell price:", item.getPrice())
                .addInlineField("Processed into:", item.getProc());

        String stats = String.join("\n", item.getStats());

        embed.addField("Stats/Effect:", stats);

        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < item.getObtainedFrom().size() && i < 10; i++)
        {
            String key = item.getObtainedFrom().get(i);
            stringBuilder.append(i == 0 ? key : "\n" + key);
        }

        embed.addInlineField("Obtained from:", stringBuilder.toString());

        if (item.getApp() != null)
            embed.setThumbnail(item.getApp());
        else
            Parser.parseThumbnail(embed, messageCreateEvent);

        Parser.parseFooter(embed, messageCreateEvent);
        Parser.parseColor(embed, messageCreateEvent);

        messageCreateEvent.getChannel().sendMessage(embed);
    }

    private void noResults(MessageCreateEvent messageCreateEvent)
    {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("No results!")
                .setDescription("Looks like there isn't an upgrade xtal for that!");

        Parser.parseThumbnail(embed, messageCreateEvent);
        Parser.parseFooter(embed, messageCreateEvent);
        Parser.parseColor(embed, messageCreateEvent);

        messageCreateEvent.getChannel().sendMessage(embed);
    }

    private void emptySearch(MessageCreateEvent messageCreateEvent)
    {
    EmbedBuilder embed = new EmbedBuilder()
            .setTitle("Empty search!")
            .setDescription("You can not find an item without specifying the item!");

    Parser.parseThumbnail(embed, messageCreateEvent);
    Parser.parseFooter(embed, messageCreateEvent);
    Parser.parseColor(embed, messageCreateEvent);

    messageCreateEvent.getChannel().sendMessage(embed);
    }

    private void sendErrorMessage(MessageCreateEvent messageCreateEvent)
    {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Error while getting item!")
                .setDescription("An error happened! Does the item even exist? The item may not be added yet.");

        Parser.parseThumbnail(embed, messageCreateEvent);
        Parser.parseFooter(embed, messageCreateEvent);
        Parser.parseColor(embed, messageCreateEvent);

        messageCreateEvent.getChannel().sendMessage(embed);
    }

}