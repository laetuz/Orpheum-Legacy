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

    public void search(String query) {
        String encoded;
        try {
            encoded = URLEncoder.encode(query == null ? "" : query.trim(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            encoded = query;
        }
        String url = Constants.TUNEIN_SEARCH_URL + "?query=" + encoded + "&filter=s&render=json";
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
            JSONArray children = obj.optJSONArray("children");

            if ("audio".equals(type)) {
                String guideId = JsonHelper.getString(obj, "guide_id");
                if (guideId.length() > 0 && text.length() > 0) {
                    out.add(RadioItem.station(guideId, text,
                            JsonHelper.getString(obj, "subtext"),
                            JsonHelper.getString(obj, "image")));
                }
            } else if (children != null) {
                collect(children, out);
            } else if ("link".equals(type) && url.length() > 0) {
                if (text.length() > 0) {
                    out.add(RadioItem.link(text, url));
                }
            }
        }
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
