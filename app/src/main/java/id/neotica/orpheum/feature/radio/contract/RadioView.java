package id.neotica.orpheum.feature.radio.contract;

import java.util.List;

import id.neotica.orpheum.feature.radio.domain.RadioItem;

/**
 * View contract for the radio screen, implemented by the radio Activity.
 * {@code RadioPresenter} drives all rendering through this interface.
 */
public interface RadioView {

    void renderItems(List<RadioItem> items);

    void playStation(RadioItem station, String streamUrl);

    void showError(String message);
}
