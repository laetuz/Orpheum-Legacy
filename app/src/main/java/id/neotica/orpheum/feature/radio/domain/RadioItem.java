package id.neotica.orpheum.feature.radio.domain;

/**
 * One row in a TuneIn browse/search result: either a playable station or a
 * link to another browse page.
 */
public class RadioItem {

    public static final int TYPE_STATION = 0;
    public static final int TYPE_LINK = 1;

    public int type;
    public String guideId;
    public String name;
    public String description;
    public String imageUrl;
    public String pageUrl;

    public static RadioItem station(String guideId, String name, String description, String imageUrl) {
        RadioItem item = new RadioItem();
        item.type = TYPE_STATION;
        item.guideId = guideId;
        item.name = name;
        item.description = description;
        item.imageUrl = imageUrl;
        return item;
    }

    public static RadioItem link(String name, String pageUrl) {
        RadioItem item = new RadioItem();
        item.type = TYPE_LINK;
        item.name = name;
        item.pageUrl = pageUrl;
        return item;
    }
}
