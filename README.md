#FrameAnimationView

#效果图
![name](https://raw.githubusercontent.com/wcl9900/FrameAnimationView/master/frameanimationview.gif)
    
#使用方式
    1.对象创建
       frameAnimationView = new FrameAnimationView(this, "image/pi_%d.jpg", 20, true);
       
    2.布局创建
    <com.wcl.frameanimation.FrameAnimationView
        android:id="@+id/framevview"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@android:color/black"
        app:formatFilePath="image/pi_%d.jpg"
        app:scaleType="FIT_CENTER"/>
        
    3.播放回调监听
    private FrameAnimationView.FramePlayListener framePlayListener = new FrameAnimationView.FramePlayListener() {
        @Override
        public void FramePlay(FrameAnimationView FrameAnimationView, int frameIndex, FrameAnimationView.FramePlayState framePlayState) {
            tvInfo.setText("当前播放第 "+ frameIndex +" 帧"+"  播放状态："+framePlayState.toString());
        }
    };
    frameAnimationView.setOnFramePlayListener(framePlayListener); 
     
     4.其他
       frameAnimationView.play();//播放
       frameAnimationView.pause();//暂停
       frameAnimationView.setScaleType(ImageView.ScaleType.FIT_CENTER);//显示方式
       frameAnimationView.setFingerGestureEnable(true);//是否支持手势控制
       frameAnimationView.setFrameDuration(20);//帧播放时间
       frameAnimationView.setRepeat(true);//是否支持循环播放