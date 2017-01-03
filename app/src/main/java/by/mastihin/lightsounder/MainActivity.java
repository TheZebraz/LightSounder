package by.mastihin.lightsounder;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private final int sampleRate = 8000;
    private final int numSamples = 1000;
    private final double sample[] = new double[numSamples];
    private double freqOfTone = 440; // hz

    private SensorManager sensorManager;
    private Sensor lightSensor;

    private final byte generatedSnd[] = new byte[2 * numSamples];

    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Use a new tread as this can take a while
        final Thread thread = new Thread(new Runnable() {
            public void run() {

                handler.post(new Runnable() {

                    public void run() {
                        while(true){
                            genTone();
                            playSound();
                        }
                    }
                });
            }
        });

        thread.start();
    }

    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this); //Отключаемся от сенсора
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        float luxLevel = event.values[0];
        freqOfTone = 440 * (double)luxLevel;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    void genTone(){
        // fill out the array
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/freqOfTone));
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
    }

    void playSound(){
        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length, AudioTrack.MODE_STATIC);
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        audioTrack.play();
    }
}
