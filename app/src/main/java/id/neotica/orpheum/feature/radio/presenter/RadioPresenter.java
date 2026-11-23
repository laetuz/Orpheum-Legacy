package id.neotica.orpheum.feature.radio.presenter;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import id.neotica.orpheum.R;
import id.neotica.orpheum.feature.radio.contract.RadioView;
import id.neotica.orpheum.feature.radio.domain.RadioItem;
import id.neotica.orpheum.network.ApiCallback;
import id.neotica.orpheum.network.ApiTask;
import id.neotica.orpheum.utils.Constants;
import id.neotica.orpheum.utils.JsonHelper;

/**
 * MVP presenter for TuneIn radio: browse/search parsing plus stream tuning
 * (including M3U/PLS playlist resolution). Rendering is delegated to a
 * {@link RadioView} and networking stays here.
 */
public class RadioPresenter {

    private static class StreamCandidate {
        String url;
        boolean playlist;
    }

    private final Context context;

    private RadioView view;

    public RadioPresenter(Context context) {
        this.context = context;
    }

    public void attach(RadioView view) {
        this.view = view;
    }

    public void detach() {
        this.view = null;
    }

    public void load(String pageUrl) {
        requestJson(withJson(pageUrl), context.getString(R.string.radio_loading), new ApiCallback() {
            @Override
            public void onSuccess(String response) {
                renderParsed(response);
            }

            @Override
            public void onError(String errorMessage) {
                showError(errorMessage);
            }
        });
    }

    public void search(String query, boolean showsOnly) {
        String encoded;
        try {
            encoded = URLEncoder.encode(query == null ? "" : query.trim(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            encoded = query;
        }
        String filter = showsOnly ? "p" : "s";
        String url = Constants.TUNEIN_SEARCH_URL + "?query=" + encoded + "&filter=" + filter + "&render=json";
        requestJson(url, context.getString(R.string.radio_searching), new ApiCallback() {
            @Override
            public void onSuccess(String response) {
                renderParsed(response);
            }

            @Override
            public void onError(String errorMessage) {
                showError(errorMessage);
            }
        });
    }

    public void tune(final RadioItem station) {
        if (station == null || station.guideId == null) {
            return;
        }
        String url = Constants.TUNEIN_TUNE_URL + "?id=" + station.guideId + "&render=json";
        requestJson(url, context.getString(R.string.radio_tuning), new ApiCallback() {
            @Override
            public void onSuccess(String response) {
                StreamCandidate candidate;
                try {
                    candidate = pickStream(new JSONObject(response));
                } catch (Exception e) {
                    candidate = null;
                }

                if (candidate == null) {
                    showError(context.getString(R.string.radio_unavailable));
                    return;
                }
                if (!candidate.playlist) {
                    play(station, candidate.url);
                    return;
                }
                resolvePlaylist(candidate.url, station);
            }

            @Override
            public void onError(String errorMessage) {
                showError(errorMessage);
            }
        });
    }

    private void resolvePlaylist(final String playlistUrl, final RadioItem station) {
        requestJson(playlistUrl, null, new ApiCallback() {
            @Override
            public void onSuccess(String response) {
                String direct = parsePlaylist(response, playlistUrl);
                if (direct == null) {
                    showError(context.getString(R.string.radio_unavailable));
                    return;
                }
                play(station, direct);
            }

            @Override
            public void onError(String errorMessage) {
                showError(errorMessage);
            }
        });
    }

    private void play(RadioItem station, String streamUrl) {
        if (view != null) {
            view.playStation(station, streamUrl);
        }
    }

    private void renderParsed(String response) {
        List<RadioItem> items;
        try {
            items = parseBody(new JSONObject(response));
        } catch (Exception e) {
            showError(context.getString(R.string.common_error_parse));
            return;
        }
        if (view != null) {
            if (items.isEmpty()) {
                view.showError(context.getString(R.string.radio_no_results));
            } else {
                view.renderItems(items);
            }
        }
    }

    private void showError(String message) {
        if (view != null) {
            view.showError(message);
        }
    }

    private List<RadioItem> parseBody(JSONObject root) {
        List<RadioItem> items = new ArrayList<RadioItem>();
        collect(root.optJSONArray("body"), items);
        return items;
    }

    private void collect(JSONArray array, List<RadioItem> out) {
        if (array == null) {
            return;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) {
                continue;
            }
            String type = JsonHelper.getString(obj, "type");
            String text = JsonHelper.getString(obj, "text");
            String url = JsonHelper.getString(obj, "URL");
            String subtext = JsonHelper.getString(obj, "subtext");
            String image = JsonHelper.getString(obj, "image");
            JSONArray children = obj.optJSONArray("children");

            if ("audio".equals(type)) {
                String guideId = JsonHelper.getString(obj, "guide_id");
                if (guideId.length() == 0 || text.length() == 0) {
                    continue;
                }
                String item = JsonHelper.getString(obj, "item");
                boolean live = "station".equals(item)
                        || (item.length() == 0 && guideId.startsWith("s"));
                if (live) {
                    out.add(RadioItem.station(guideId, text, subtext, image));
                } else {
                    TitleInfo info = parseTitle(text);
                    String description = subtext;
                    if (info.seconds > 0) {
                        String label = formatDurationLabel(info.seconds);
                        description = description.length() > 0 ? label + " - " + description : label;
                    }
                    out.add(RadioItem.episode(guideId, info.title, description, image, info.seconds));
                }
            } else if (children != null) {
                if (text.length() > 0) {
                    int last = out.size() - 1;
                    boolean duplicate = last >= 0
                            && out.get(last).type == RadioItem.TYPE_HEADER
                            && text.equals(out.get(last).name);
                    if (!duplicate) {
                        out.add(RadioItem.header(text));
                    }
                }
                collect(children, out);
            } else if ("link".equals(type) && url.length() > 0) {
                if ("show".equals(JsonHelper.getString(obj, "item"))) {
                    String guideId = JsonHelper.getString(obj, "guide_id");
                    if (guideId.length() > 0 && text.length() > 0) {
                        out.add(RadioItem.show(guideId, text, subtext, image, url));
                    }
                } else if (text.length() > 0) {
                    out.add(RadioItem.link(text, url));
                }
            }
        }
    }

    private static class TitleInfo {
        String title = "";
        int seconds;
    }

    private TitleInfo parseTitle(String raw) {
        TitleInfo info = new TitleInfo();
        info.title = raw != null ? raw.trim() : "";
        if (info.title.length() == 0 || !info.title.endsWith(")")) {
            return info;
        }
        int open = info.title.lastIndexOf('(');
        if (open < 0) {
            return info;
        }
        String inside = info.title.substring(open + 1, info.title.length() - 1)
                .trim().toLowerCase();
        if (inside.length() == 0) {
            return info;
        }

        int hours = 0;
        int minutes = 0;
        boolean matched = false;
        int index = 0;
        while (index < inside.length()) {
            while (index < inside.length() && inside.charAt(index) == ' ') {
                index++;
            }
            int start = index;
            while (index < inside.length() && Character.isDigit(inside.charAt(index))) {
                index++;
            }
            if (start == index) {
                return info;
            }
            int value;
            try {
                value = Integer.parseInt(inside.substring(start, index));
            } catch (Throwable t) {
                return info;
            }
            if (index < inside.length() && inside.charAt(index) == 'h') {
                hours = value;
                index++;
                matched = true;
            } else if (index < inside.length() && inside.charAt(index) == 'm') {
                minutes = value;
                index++;
                matched = true;
            } else {
                return info;
            }
            while (index < inside.length() && inside.charAt(index) == ' ') {
                index++;
            }
        }
        if (!matched || (hours == 0 && minutes == 0)) {
            return info;
        }
        info.seconds = hours * 3600 + minutes * 60;
        info.title = info.title.substring(0, open).trim();
        return info;
    }

    private static String formatDurationLabel(int seconds) {
        int totalMinutes = seconds / 60;
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        if (hours > 0) {
            return minutes > 0 ? hours + "h " + minutes + "m" : hours + "h";
        }
        return minutes + "m";
    }

    private StreamCandidate pickStream(JSONObject root) {
        JSONArray body = root.optJSONArray("body");
        if (body == null) {
            return null;
        }

        StreamCandidate best = null;
        int bestScore = -1;
        int bestReliability = -1;

        for (int i = 0; i < body.length(); i++) {
            JSONObject obj = body.optJSONObject(i);
            if (obj == null) {
                continue;
            }
            String mediaType = JsonHelper.getString(obj, "media_type");
            String hls = JsonHelper.getString(obj, "is_hls_advanced");
            String url = JsonHelper.getString(obj, "url");
            String playlistType = JsonHelper.getString(obj, "playlist_type").toLowerCase();

            if (url.length() == 0 || "true".equals(hls) || "hls".equals(mediaType.toLowerCase())) {
                continue;
            }
            String lowerUrl = url.toLowerCase();
            if (lowerUrl.endsWith(".m3u8")) {
                continue;
            }

            int score = 0;
            if ("mp3".equals(mediaType.toLowerCase())) {
                score = 6;
            } else if ("aac".equals(mediaType.toLowerCase()) || "aacp".equals(mediaType.toLowerCase())) {
                score = 3;
            } else {
                continue;
            }
            if (lowerUrl.startsWith("http://")) {
                score += 2;
            } else if (lowerUrl.startsWith("https://")) {
                score += 1;
            } else {
                continue;
            }

            int reliability = JsonHelper.getInt(obj, "reliability", 0);
            if (score > bestScore || (score == bestScore && reliability > bestReliability)) {
                bestScore = score;
                bestReliability = reliability;
                best = new StreamCandidate();
                best.url = url;
                best.playlist = "m3u".equals(playlistType) || "pls".equals(playlistType)
                        || lowerUrl.endsWith(".m3u") || lowerUrl.endsWith(".pls");
            }
        }
        return best;
    }

    private String parsePlaylist(String content, String baseUrl) {
        if (content == null) {
            return null;
        }
        String[] lines = content.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String raw = lines[i].trim();
            if (raw.length() == 0 || raw.startsWith("#") || raw.startsWith("[")) {
                continue;
            }

            String candidate = null;
            String lower = raw.toLowerCase();
            if (lower.startsWith("file") && raw.indexOf('=') > 0) {
                candidate = raw.substring(raw.indexOf('=') + 1).trim();
            } else if (lower.startsWith("http://") || lower.startsWith("https://")) {
                candidate = raw;
            }
            if (candidate == null || candidate.length() == 0) {
                continue;
            }
            if (candidate.length() > 1 && candidate.startsWith("\"") && candidate.endsWith("\"")) {
                candidate = candidate.substring(1, candidate.length() - 1);
            }
            if (candidate.toLowerCase().endsWith(".m3u8")) {
                continue;
            }
            if (!candidate.toLowerCase().startsWith("http://")
                    && !candidate.toLowerCase().startsWith("https://")) {
                try {
                    candidate = new URL(new URL(baseUrl), candidate).toString();
                } catch (Throwable t) {
                    continue;
                }
            }
            return candidate;
        }
        return null;
    }

    private String withJson(String url) {
        if (url == null) {
            return Constants.TUNEIN_BROWSE_URL + "?render=json";
        }
        if (url.indexOf("render=") >= 0) {
            return url;
        }
        return url + (url.indexOf('?') >= 0 ? "&" : "?") + "render=json";
    }

    // --- I/O seams: overridden in tests to run synchronously ---

    void requestJson(String url, String loadingMessage, ApiCallback callback) {
        new ApiTask(context, "GET", url, null, loadingMessage, callback).execute();
    }
}
