package ch.zhaw.bait17.dummy;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

/**
 * <p>
 *     Implementation of a MP3 decoder based on Java Zoom JLayer.
 *     PCM sample blocks can be read one by one with {@link #getNextSampleBlock()}.
 * </p>
 * @author georgrem, stockan1
 */

public class MP3Decoder implements AudioDecoder {

    private static final String TAG = MP3Decoder.class.getSimpleName();
    private static final MP3Decoder INSTANCE = new MP3Decoder();
    private static Decoder decoder;

    private static short[] sampleBlock = null;
    private static Bitstream bitstream;
    private InputStream is;
    private static int sampleRate;
    private static int channels;
    private static int shortSamplesRead;
    private static int position;

    private MP3Decoder() {

    }

    /**
     * Returns the singleton instance of the MP3 decoder.
     * @return
     */
    public static MP3Decoder getInstance() {
        return INSTANCE;
    }

    /**
     * Sets the audio source.
     * @param inputStream
     */
    public void setSource(@NonNull InputStream inputStream) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {

            }
        }
        is = inputStream;
        bitstream = new Bitstream(is);
        decoder = new Decoder();
        init();
    }

    @Override
    @Nullable
    public short[] getNextSampleBlock() {
        try {
            Header currentFrameHeader = bitstream.readFrame();
            if (currentFrameHeader != null) {
                position += currentFrameHeader.ms_per_frame();
                SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(currentFrameHeader, bitstream);
                sampleBlock = samples.getBuffer();
                shortSamplesRead += sampleBlock.length;
            } else {
                return null;
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

    private static void init() {
        extractFrameHeaderInfo(bitstream);
        shortSamplesRead = 0;
        position = 0;
    }

    private static void extractFrameHeaderInfo(Bitstream bitstream) {
        try {
            Header frameHeader = bitstream.readFrame();
            SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(frameHeader, bitstream);
            if (samples != null) {
                sampleRate = samples.getSampleFrequency();
                channels = samples.getChannelCount();
            }
            bitstream.closeFrame();
            bitstream.unreadFrame();
        } catch(BitstreamException | DecoderException ex) {
            Toast.makeText(ApplicationContext.getAppContext(),
                    "Failed to extract frame header data.\n " + ex.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

}
