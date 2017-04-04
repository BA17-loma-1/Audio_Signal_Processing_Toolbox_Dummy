package ch.zhaw.bait17.dummy;


/**
 * An interface representing an audio decoder.
 * @author georgrem, stockan1
 */

public interface AudioDecoder {

    /**
     * Returns the next PCM sample block or null if end of stream is reached.
     * @return
     */
    PCMSampleBlock getNextSampleBlock();

    /**
     * Returns the sample rate.
     * @return
     */
    int getSampleRate();

    /**
     * Returns the number of channels.
     * @return
     */
    int getChannels();

}
