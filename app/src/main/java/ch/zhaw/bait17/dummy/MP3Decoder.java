package ch.zhaw.bait17.dummy;

import android.util.Log;

import java.io.InputStream;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

/**
 * <p>
 * Implementation of a mp3 decoder.
 * PCM sample blocks can be read one by one with {@link #getNextSampleBlock()}.
 * </p>
 *
 * @author georgrem, stockan1
 */

public class MP3Decoder implements AudioDecoder {

    private static final String TAG = MP3Decoder.class.getSimpleName();

    private InputStream is;
    private Bitstream bitstream;
    private Decoder decoder;
    private int frameIndex;
    private int position;
    private int shortSamplesRead;
    private int sampleRate;
    private int channels;

    public MP3Decoder(InputStream is) {
        this.is = is;
        init();
    }

    @Override
    public PCMSampleBlock getNextSampleBlock() {
        PCMSampleBlock sampleBlock = null;
        try {
            Header currentFrameHeader = bitstream.readFrame();
            if (currentFrameHeader != null) {
                frameIndex++;
                position += currentFrameHeader.ms_per_frame();
                SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(currentFrameHeader, bitstream);
                sampleBlock = new PCMSampleBlock(
                        samples.getBuffer(), samples.getSampleFrequency());
                shortSamplesRead += sampleBlock.getSamples().length;
            }
            bitstream.closeFrame();
        } catch (BitstreamException | DecoderException ex) {
            Log.e(TAG, ex.getMessage(), ex);
        }
        return sampleBlock;
    }

    @Override
    public int getSampleRate() {
        return sampleRate;
    }

    @Override
    public int getChannels() {
        return channels;
    }

    private void init() {
        bitstream = new Bitstream(is);
        decoder = new Decoder();
        extractFrameHeaderInfo(bitstream);
        shortSamplesRead = 0;
        position = 0;
        frameIndex = 0;
    }

    private void extractFrameHeaderInfo(Bitstream bitstream) {
        try {
            Header frameHeader = bitstream.readFrame();
            SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(frameHeader, bitstream);
            sampleRate = samples.getSampleFrequency();
            channels = samples.getChannelCount();
            bitstream.closeFrame();
            bitstream.unreadFrame();
        } catch (BitstreamException | DecoderException ex) {
            Log.e(TAG, "Failed to extract frame header data.\n " + ex.getMessage());
        }
    }

}
