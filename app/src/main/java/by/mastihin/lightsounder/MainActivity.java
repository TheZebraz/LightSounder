package by.mastihin.lightsounder;

import android.app.Activity;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {

    private Button mButtonStartSound;
    private TextView mTextViewFrequency;
    private TextView mTextViewLightLevel;
    private View mValues;
    private SeekBar mSeekBarStartFrequencyCoef;

    private double mStartFrequencyCoef = 50;
    private boolean mKeepGoing = false;
    private double mSynthFrequency = 440;

    private SensorManager mSensorManager;
    private Sensor mLightSensor;

    private Thread mSoundThread = new Thread(new Runnable() {
        @Override
        public void run() {
            final int SAMPLE_RATE = 44100;
            int minSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, minSize, AudioTrack.MODE_STREAM);
            audioTrack.play();
            short[] buffer = new short[minSize];
            float angle = 0;
            while (mKeepGoing) {
                for (int i = 0; i < buffer.length; i++) {
                    buffer[i] = (short) (Short.MAX_VALUE * ((float) Math.sin(angle)));
                    angle += (float) (Math.PI) * mSynthFrequency / SAMPLE_RATE;
                }
                audioTrack.write(buffer, 0, buffer.length);
            }
        }
    });

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initBackground();
        initViews();
        initSensor();
        initSeek();
    }

    private void initViews() {
        mButtonStartSound = (Button) this.findViewById(R.id.start_sound);
        mButtonStartSound.setOnClickListener(this);

        mValues = findViewById(R.id.values);
        mValues.setVisibility(View.INVISIBLE);

        mTextViewFrequency = (TextView) findViewById(R.id.tvFreqValue);
        mTextViewLightLevel = (TextView) findViewById(R.id.tvLightValue);
    }

    private void initBackground() {
        RelativeLayout constraintLayout = (RelativeLayout) findViewById(R.id.activity_main);

        AnimationDrawable animationDrawable = (AnimationDrawable) constraintLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2500);
        animationDrawable.setExitFadeDuration(5000);
        animationDrawable.start();
    }

    private void initSeek() {
        mSeekBarStartFrequencyCoef = (SeekBar) findViewById(R.id.sbStartFrequency);
        mSeekBarStartFrequencyCoef.setMax(500);
        mSeekBarStartFrequencyCoef.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
    }

    SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            double value = seekBar.getProgress();
            switch (seekBar.getId()) {
                case R.id.sbStartFrequency:
                    mStartFrequencyCoef = value;
                    break;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };

    private void initSensor() {
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    @Override
    public void onPause() {
        super.onPause();
        mKeepGoing = false;
    }

    public void onClick(View v) {
        if (!mKeepGoing) {
            start();
        } else {
            stop();
        }
    }

    private void stop() {
        mKeepGoing = false;
        mValues.setVisibility(View.INVISIBLE);
        mButtonStartSound.setText("Start");

        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mLightSensorListener);
        }

    }

    private void start() {
        mKeepGoing = true;
        mButtonStartSound.setText("Stop");
        mValues.setVisibility(View.VISIBLE);

        if (mLightSensor != null) {
            mSensorManager.registerListener(
                    mLightSensorListener,
                    mLightSensor,
                    SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            Toast.makeText(this, getString(R.string.all_no_sensor_message), Toast.LENGTH_SHORT).show();
        }
        if(!mSoundThread.isInterrupted()) {
            mSoundThread.start();
        }
    }

    private final SensorEventListener mLightSensorListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                mSynthFrequency = event.values[0] + mStartFrequencyCoef;
                mTextViewFrequency.setText(Double.toString(mSynthFrequency) + " Hz");
                mTextViewLightLevel.setText(Double.toString(event.values[0]));
            }
        }
    };
}