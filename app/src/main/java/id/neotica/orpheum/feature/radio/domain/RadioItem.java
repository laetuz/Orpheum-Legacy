package id.neotica.orpheum.feature.radio.domain;

import java.io.Serializable;

/**
 * One row in a TuneIn browse/search result: a playable station or episode,
 * a show/category link, or a non-clickable section header.
 */
public class RadioItem implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int TYPE_STATION = 0;
    public static final int TYPE_LINK = 1;
    public static final int TYPE_SHOW = 2;
    public static final int TYPE_HEADER = 3;

    public int type;
    public String guideId;
    public String name;
    public String description;
    public String imageUrl;
    public String pageUrl;
    public boolean live;
    public int durationSeconds;

    public static RadioItem station(String guideId, String name, String description, String imageUrl) {
        RadioItem item = new RadioItem();
        item.type = TYPE_STATION;
        item.guideId = guideId;
        item.name = name;
        item.description = description;
        item.imageUrl = imageUrl;
        item.live = true;
        return item;
    }

    public static RadioItem episode(String guideId, String name, String description,
                                    String imageUrl, int durationSeconds) {
        RadioItem item = new RadioItem();
        item.type = TYPE_STATION;
        item.guideId = guideId;
        item.name = name;
        item.description = description;
        item.imageUrl = imageUrl;
        item.live = false;
        item.durationSeconds = durationSeconds;
        return item;
    }

    public static RadioItem show(String guideId, String name, String description,
                                 String imageUrl, String pageUrl) {
        RadioItem item = new RadioItem();
        item.type = TYPE_SHOW;
        item.guideId = guideId;
        item.name = name;
        item.description = description;
        item.imageUrl = imageUrl;
        item.pageUrl = pageUrl;
        return item;
    }

    public static RadioItem link(String name, String pageUrl) {
        RadioItem item = new RadioItem();
        item.type = TYPE_LINK;
        item.name = name;
        item.pageUrl = pageUrl;
        return item;
    }

    public static RadioItem header(String name) {
        RadioItem item = new RadioItem();
        item.type = TYPE_HEADER;
        item.name = name;
        return item;
    }
}
