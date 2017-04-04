package ch.zhaw.bait17.dummy;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import org.greenrobot.eventbus.EventBus;

import java.io.InputStream;

import ch.zhaw.bait17.dummy.filter.Filter;


public class MainActivity extends AppCompatActivity {

    private static final int BUFFER_LENGTH_PER_CHANNEL_IN_SECONDS = 3;

    private volatile boolean keepDecoding = true;

    private AudioDecoder mp3Decoder;
    private AudioTrack audioTrack;
    private int sampleRate;
    private int channels;
    private Filter filter;
    private EventBus eventBus;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init() {
        buildEventBus();

        InputStream is = getResources().openRawResource(R.raw.voodoo);
        mp3Decoder = new MP3Decoder(is);

        filter = FilterUtil.getFilter(getResources().openRawResource(R.raw.b_fir_lowpass));

        createAudioTrack();
        audioTrack.play();
    }

    private void buildEventBus() {
        eventBus = EventBus.getDefault();
    }

    private void decodeAndPlay() {
        keepDecoding = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (keepDecoding) {
                    PCMSampleBlock pcmSampleBlock = mp3Decoder.getNextSampleBlock();
                    if (pcmSampleBlock != null) {
                        short[] samples = pcmSampleBlock.getSamples();
                        audioTrack.write(samples, 0, samples.length);
                    } else {
                        keepDecoding = false;
                    }
                }
            }
        }).start();
    }

    private void decodeFilterAndPlay() {
        keepDecoding = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (keepDecoding) {
                    PCMSampleBlock pcmSampleBlock = mp3Decoder.getNextSampleBlock();
                    if (pcmSampleBlock != null) {
                        eventBus.post(new PreFilterSampleBlock(
                                pcmSampleBlock.getSamples(),
                                Constants.DEFAULT_SAMPLE_RATE));

                        PCMSampleBlock output = applyFilter(new PCMSampleBlock(pcmSampleBlock.getSamples(), Constants.DEFAULT_SAMPLE_RATE));
                        short[] samples = output.getSamples();
                        audioTrack.write(samples, 0, samples.length);

                        eventBus.post(new PostFilterSampleBlock(
                                samples,
                                Constants.DEFAULT_SAMPLE_RATE));
                    } else {
                        keepDecoding = false;
                    }
                }
            }
        }).start();
    }

    private void createAudioTrack() {
        sampleRate = Constants.DEFAULT_SAMPLE_RATE;
        channels = Constants.DEFAULT_CHANNELS;

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

    private PostFilterSampleBlock applyFilter(PCMSampleBlock input) {
        if (filter == null) {
            return new PostFilterSampleBlock(input.getSamples(), input.getSampleRate());
        }
        float[] samples = PCMUtil.short2FloatArray(input.getSamples());
        short[] filtered = PCMUtil.float2ShortArray(filter.apply(samples));
        return new PostFilterSampleBlock(filtered, input.getSampleRate());
    }

    public void onClick_decodeAndPlay(View view) {
        decodeAndPlay();
    }

    public void onClick_decodeFilterAndPlay(View view) {
        decodeFilterAndPlay();
    }

}
