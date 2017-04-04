package ch.zhaw.bait17.dummy.filter;

/**
 * Enumerator for filter types.
 * @author georgrem, stockan1
 */

public enum FilterType {

    LOWPASS("lowpass"), HIGHPASS("highpass"), BANDPASS("bandpass"), BANDSTOP("bandstop");

    private String type;

    private FilterType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

}
