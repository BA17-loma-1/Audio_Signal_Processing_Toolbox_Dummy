package ch.zhaw.bait17.dummy;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import java.io.InputStream;
import ch.zhaw.bait17.dummy.filter.Filter;

public class MainActivity extends AppCompatActivity {

    private static final int BUFFER_LENGTH_PER_CHANNEL_IN_SECONDS = 3;

    private volatile boolean keepDecoding = true;

    private MP3Decoder mp3Decoder;
    private AudioTrack audioTrack;
    private int sampleRate;
    private int channels;
    private Filter filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private void init() {
        InputStream is = getResources().openRawResource(R.raw.amen_breakbeat);
        mp3Decoder = MP3Decoder.getInstance();
        mp3Decoder.setSource(is);
        sampleRate = mp3Decoder.getSampleRate();
        channels = mp3Decoder.getChannels();
        filter = FilterUtil.getFilter(getResources().openRawResource(R.raw.b_fir_lowpass));
        createAudioTrack();
    }

    private void decodeAndPlay() {
        init();
        if (isInitialised()) {
            keepDecoding = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (keepDecoding) {
                        short[] pcmSampleBlock = mp3Decoder.getNextSampleBlock();
                        if (pcmSampleBlock != null) {
                            audioTrack.write(pcmSampleBlock, 0, pcmSampleBlock.length);
                        } else {
                            keepDecoding = false;
                        }
                    }
                }
            }).start();
            audioTrack.play();
        }
    }

    private void decodeFilterAndPlay() {
        init();
        if (isInitialised()) {
            keepDecoding = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (keepDecoding) {
                        short[] pcmSampleBlock = mp3Decoder.getNextSampleBlock();
                        if (pcmSampleBlock != null) {
                            float[] samples = PCMUtil.short2FloatArray(pcmSampleBlock);
                            audioTrack.write(PCMUtil.float2ShortArray(applyFilter(samples)), 0,
                                    samples.length);
                        } else {
                            keepDecoding = false;
                        }
                    }
                }
            }).start();
            audioTrack.play();
        }
    }

    private void createAudioTrack() {
        int optimalBufferSize = sampleRate * channels * BUFFER_LENGTH_PER_CHANNEL_IN_SECONDS;
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate,
                channels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize < optimalBufferSize) {
            bufferSize = optimalBufferSize;
        }
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                channels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

    }

    private float[] applyFilter(float[] input) {
        if (filter == null) {
            return input;
        }
        return filter.apply(input);
    }

    public void onClick_decodeAndPlay(View view) {
        decodeAndPlay();
    }

    public void onClick_decodeFilterAndPlay(View view) {
        decodeFilterAndPlay();
    }

    private boolean isInitialised() {
        return audioTrack != null && audioTrack.getState() == AudioTrack.STATE_INITIALIZED;
    }

}
