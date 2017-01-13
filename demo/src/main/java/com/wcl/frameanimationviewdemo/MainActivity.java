package com.wcl.frameanimationviewdemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.wcl.frameanimation.FrameAnimationView;

/**
 * 自定义帧动画demo
 */
public class MainActivity extends Activity {

    Button btn_play;
    Button btn_pause;

    Spinner spinnerScaleType;

    CheckBox checkBoxFinger;

    TextView tvInfo;

    FrameAnimationView frameAnimationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_play = (Button) findViewById(R.id.btn_play);
        btn_pause = (Button) findViewById(R.id.btn_pause);
        tvInfo = (TextView) findViewById(R.id.tv_info);
        spinnerScaleType = (Spinner)findViewById(R.id.sp_scaletype);
        checkBoxFinger = (CheckBox) findViewById(R.id.cb_finger);

        spinnerScaleType.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, new String[]{"居中","填充","原始"}));
        spinnerScaleType.setOnItemSelectedListener(onItemSelectedListener);

        checkBoxFinger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                frameAnimationView.setFingerGestureEnable(checkBoxFinger.isChecked());
            }
        });

        btn_play.setOnClickListener(onClickListener);
        btn_pause.setOnClickListener(onClickListener);

        frameAnimationView = (FrameAnimationView)findViewById(R.id.framevview);
        frameAnimationView.setFrameDuration(40);
        frameAnimationView.setFingerGestureEnable(checkBoxFinger.isChecked());
        frameAnimationView.setOnFramePlayListener(framePlayListener);
        frameAnimationView.setRepeat(true);

        frameAnimationView = new FrameAnimationView(this, "image/pi_%d.jpg", 20, true);
    }

    private AdapterView.OnItemSelectedListener onItemSelectedListener = new AdapterView.OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if(position == 0){
                frameAnimationView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            }
            else if(position == 1){
                frameAnimationView.setScaleType(ImageView.ScaleType.FIT_XY);
            }
            else if(position == 2){
                frameAnimationView.setScaleType(ImageView.ScaleType.CENTER);
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(v.getId() == R.id.btn_play){
                frameAnimationView.play();
            }
            else if(v.getId() == R.id.btn_pause){
                frameAnimationView.pause();
            }
        }
    };

    private FrameAnimationView.FramePlayListener framePlayListener = new FrameAnimationView.FramePlayListener() {
        @Override
        public void FramePlay(FrameAnimationView FrameAnimationView, int frameIndex, FrameAnimationView.FramePlayState framePlayState) {
            tvInfo.setText("当前播放第 "+ frameIndex +" 帧"+"  播放状态："+framePlayState.toString());
        }
    };
}
