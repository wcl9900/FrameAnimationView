package com.wcl.frameanimation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView.ScaleType;

import java.io.IOException;

/**
 * 帧动画视图，可以播放逐帧动画,支持动画的暂停，停止，播放，播放延迟，定位播放到指定帧，及播放到帧监听器，帧监听器可以用来对播放到指定帧做一些其他处理。
 * @author 王春龙
 *
 */
public class FrameAnimationView extends View implements OnGestureListener, OnTouchListener{

	public enum FramePlayState{
		PLAYING,
		PAUSE,
		LOCATING
	}

	ScaleType[] scaleTypeArray = new ScaleType[]{
			ScaleType.FIT_CENTER,
			ScaleType.CENTER,
			ScaleType.FIT_XY
	};
	private GestureDetector gesture;

	private Paint paint;
	private PaintFlagsDrawFilter pfd;
	
	private Config bitmapConfig;
	
	private Matrix matrix;
	
	private ScaleType scaleType = ScaleType.FIT_CENTER;
	
	private Bitmap bitmap ;
	
	private static int frameDuration = 20;
	
	private int frameCount = -1;

	private String formatFilePath;
	
	private int bitmapIndex = 0;
	
	private Thread frameThread;
	
	private FramePlayState framePlayState = FramePlayState.PAUSE;
	
	private boolean repeat = true;
	
	private FramePlayListener framePlayListener;
	
	private int toFrame = -1;
	
	private int preFrame = -1;
	
	private int delayTime = 0;
	
	private boolean playEnable = true;

	private boolean releaseThread = false;
	
	private int speed_hand = 30;
	
	private long preTime = 0;
	
	private boolean autoPlay = true;

	/**
	 * 就近定位
	 */
	private boolean agentSearch = false;
	private boolean addFrame = true;
	
	private boolean fingerGestureEnable = true;

	private boolean foreUpdateUI = false;
	/**
	 * 帧动画监听器
	 * @author 王春龙
	 *
	 */
	public interface FramePlayListener{
		void FramePlay(FrameAnimationView FrameAnimationView, int frameIndex, FramePlayState framePlayState);
	}
	
	public FrameAnimationView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initAttrs(attrs);
		init();
	}

	public FrameAnimationView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initAttrs(attrs);
		init();
	}
	/**
	 *
	 * @param context
	 * @param formatFilePath 图片资源所在asset格式化文件路径，如image_%d.jpg
	 */
	public FrameAnimationView(Context context, String formatFilePath) {
		this(context, formatFilePath, frameDuration,true);
	}
	/**
	 * 
	 * @param context
	 * @param formatFilePath 图片资源所在asset格式化文件路径，如image_%d.jpg
	 * @param frameDuration 帧播放间隔时间
	 */
	public FrameAnimationView(Context context, String formatFilePath, int frameDuration) {
		this(context, formatFilePath, frameDuration,true);
	}
	/**
	 * 
	 * @param context
	 * @param formatFilePath 图片资源所在asset格式化文件路径，如image_%d.jpg
	 * @param frameDuration 帧播放间隔时间
	 * @param autoPlay 是否开启线程使帧动画可以支持自动播放
	 */
	public FrameAnimationView(Context context, String formatFilePath, int frameDuration, boolean autoPlay) {
		super(context);
		this.formatFilePath = formatFilePath;
		this.frameDuration = frameDuration;
		this.autoPlay = autoPlay;
		init();
	}
	private void initAttrs(AttributeSet attrs){
		TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.FrameAnimationView);
		formatFilePath = ta.getString(R.styleable.FrameAnimationView_formatFilePath);
		scaleType = scaleTypeArray[ta.getInt(R.styleable.FrameAnimationView_scaleType, 0)];
		frameCount = ta.getInt(R.styleable.FrameAnimationView_frameCount, frameCount);
		frameDuration = ta.getInt(R.styleable.FrameAnimationView_frameDuration, frameDuration);
		speed_hand = ta.getInt(R.styleable.FrameAnimationView_speedHand, speed_hand);
		autoPlay = ta.getBoolean(R.styleable.FrameAnimationView_autoPlay, autoPlay);
		agentSearch = ta.getBoolean(R.styleable.FrameAnimationView_agentSearch, false);

		ta.recycle();
	}

	public void setBitmapConfig(Config config){
		this.bitmapConfig = config;
	}
	
	public void setScaleType(ScaleType scaleType){
		this.scaleType = scaleType;
		foreUpdateUI = true;
		postInvalidate();
	}
	
	@Override
	protected void onDetachedFromWindow() {
		recycle();
		super.onDetachedFromWindow();
	}

	/**
	 * 关闭线程，回收图片资源
	 */
	public void recycle() {
		releaseThread = true;
		if(frameThread != null){
			frameThread.interrupt();
			frameThread = null;
		}
		if(bitmap != null && !bitmap.isRecycled()){
			bitmap.recycle();
			bitmap = null;
		}
		System.gc();
	}
	
	private void init(){
		if(frameCount == -1){
			try {
				String[] list = getContext().getAssets().list(ImageReadUtils.getFolderName(formatFilePath));
				if(list != null){
					frameCount = list.length;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		this.setLongClickable(true);
		this.setOnTouchListener(this);
		gesture = new GestureDetector(getContext(), this);
		gesture.setIsLongpressEnabled(true);
		
		matrix = new Matrix();
		
		paint = new Paint();
		paint.setAntiAlias(true);  
		
		pfd = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG| Paint.FILTER_BITMAP_FLAG);

		post(new Runnable() {
			@Override
			public void run() {
				startFrameThread();
				if(autoPlay){
					play();
				}
			}
		});
	}
	
	public void setAgentSerchEnable(boolean agentSearch){
		this.agentSearch = agentSearch;
	}
	public boolean isAgentSearch(){
		return this.agentSearch;
	}
	
	public int getFrameCount(){
		return frameCount;
	}
	
	public void setFramePlayState(FramePlayState animPlayState){
		this.framePlayState = animPlayState;
	}
	
	public FramePlayState getAnimPlayState(){
		return this.framePlayState;
	}
	
	public void setOnFramePlayListener(FramePlayListener framePlayListener){
		this.framePlayListener = framePlayListener;
	}
	
	public void setCurrentFrame(int currentFrame){
		if(currentFrame < 0 || currentFrame >= frameCount || currentFrame == bitmapIndex) return;
		
		bitmapIndex = currentFrame;
		updateUI();
	}
	
	public void setRepeat(boolean repeat){
		this.repeat = repeat;
	}
	
	public void setPlayEnable(boolean playEnable){
		this.playEnable = playEnable;
	}

	/**
	 * 是否可通过手指进行左右播放控制
	 * @param enable
     */
	public void setFingerGestureEnable(boolean enable){
		this.fingerGestureEnable = enable;
	}
	
	/**
	 * 设定每帧播放时间
	 * @param duration
	 */
	public void setFrameDuration(int duration){
		this.frameDuration = duration;
	}
	
	public boolean isRepeat(){
		return this.repeat;
	}
	
	public int getCurrentFrame(){
		return this.bitmapIndex;
	}
	
	public void play(){
		play(0);
	}
	
	public void play(int delayTime){
		if(!playEnable || isPlaying()) return;
		this.delayTime = delayTime;

		if(isPlayEnd()){
			bitmapIndex = 0;
		}

		toFrame = -1;
		framePlayState = FramePlayState.PLAYING;
	}
	
	public void playToFrame(int toFrame){
		if(toFrame < 0 || toFrame > frameCount - 1 || isPlaying()) return;
		
		if(bitmapIndex == toFrame){
			this.framePlayState = FramePlayState.LOCATING;
			updateUI();
			return;
		}
		
		addFrame = addFrameFlag(toFrame);
		
		setRepeat(true);
		this.toFrame = toFrame;
		this.framePlayState = FramePlayState.LOCATING;
	}

	private boolean addFrameFlag(int toFrame){
		int addCount = 0;
		int delCount = 0;
		if(bitmapIndex > toFrame){
			addCount = frameCount - 1 - bitmapIndex + toFrame;
			delCount = bitmapIndex - toFrame;
		}
		else{
			addCount = toFrame - bitmapIndex;
			delCount = bitmapIndex + frameCount - 1 - toFrame;
		}
		return addCount <= delCount;
	}
	public void playToFrame(int toFrame , int delayTime){
		this.delayTime = delayTime;
		playToFrame(toFrame);
	}
	
	public boolean isPlaying(){
		return framePlayState == FramePlayState.PLAYING || framePlayState == FramePlayState.LOCATING;
	}
	
	private boolean isPlayEnd() {
		return !repeat && bitmapIndex == frameCount - 1;
	}
	
	public void pause(){
		if(isPlaying()){
			 framePlayState = FramePlayState.PAUSE;
		}
	}
	
	public void stop(){
		framePlayState = FramePlayState.PAUSE;
		bitmapIndex = 0;
		updateUI();
	}

	private Runnable frameRunable = new Runnable() {
		@Override
		public void run() {
			while(true){
				if(releaseThread) return;
				if(framePlayState == FramePlayState.PAUSE){
					continue;
				}
				if(framePlayState == FramePlayState.LOCATING){
					if(bitmapIndex == toFrame){
						continue;
					}
				}

				try {
					if(delayTime != 0){
						Thread.sleep(delayTime);
						delayTime = 0;
					}
					Thread.sleep(frameDuration);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if(addFrame){
					bitmapIndex_add();
				}
				else{
					bitmapIndex_del();
				}

				updateUI();
			}
		}
	};

	/**
	 * 开启帧动画线程
	 */
	private void startFrameThread(){
		if(!releaseThread && frameThread != null) return ;
		releaseThread = false;
		frameThread = new Thread(frameRunable);
		frameThread.start();
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return gesture.onTouchEvent(event);
	}
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return true;
	}
	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}
	
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
							float distanceY) {
		if(!fingerGestureEnable || !playEnable || isPlaying()) return false;

		if(Math.abs(distanceY) > Math.abs(distanceX) * 2) return false;
		
		if(System.currentTimeMillis() - preTime < speed_hand){
			return true;
		}
		else{
			preTime = System.currentTimeMillis();
		}
		
		if(distanceX < 0){
			if(!repeat && bitmapIndex == frameCount - 1) return false;
			bitmapIndex = (++bitmapIndex) % frameCount;
		}
		else{
			if(!repeat && bitmapIndex == 0) return false;
			bitmapIndex_del();
		}
		updateUI();
		return true;
	}

	private void updateUI() {
		postInvalidate();
	}

	@Override
	public void onLongPress(MotionEvent e) {
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
						   float velocityY) {
		return false;
	}
	
	private void bitmapIndex_add(){
		if(isPlayEnd() || repeat && bitmapIndex == toFrame){
			framePlayState = FramePlayState.PAUSE;
			return;
		}
		
		bitmapIndex = (++bitmapIndex) % frameCount;
	}
	
	private void bitmapIndex_del(){
		if(bitmapIndex == 0 && !repeat){
			return;
		}
		bitmapIndex = bitmapIndex - 1 < 0 ? frameCount - 1 : bitmapIndex - 1;
	}

	@SuppressLint("DrawAllocation")
	@Override
	protected void onDraw(Canvas canvas) {
		try {
			if(foreUpdateUI || bitmapIndex != preFrame || bitmap == null){
				foreUpdateUI = false;
				if(bitmap != null){
					bitmap.recycle();
					bitmap = null;
				}
				
				String imagePath = String.format(formatFilePath, bitmapIndex);
				bitmap = ImageReadUtils.getAssetBitmap(getContext() , imagePath, 1,
						bitmapConfig != null ? bitmapConfig : Config.RGB_565);
				
				resetMatrix(bitmap);
				
				preFrame = bitmapIndex;
			}
			
			canvas.save();
			
			canvas.setDrawFilter(pfd);
			canvas.drawBitmap(bitmap, matrix, paint);
			
			canvas.restore();

			fireFramePlayListener();
		}
		catch(OutOfMemoryError ooe){
			ooe.printStackTrace();
			Log.e("帧动画视图", "读取图片内存溢出！！！！！！！！！！！");
		}
		catch (Exception e) {
			e.printStackTrace();			
			Log.e("帧动画视图", "读取图片出错！！！！！！！！！！！ :" + e.getMessage());
		}
	}

	private void fireFramePlayListener() {
		if(framePlayListener != null){
			framePlayListener.FramePlay(this , bitmapIndex , framePlayState);
			if(bitmapIndex == toFrame && framePlayState == FramePlayState.LOCATING){
				pause();
			}
		}
	}
	
	private void resetMatrix(Bitmap bmp){
		switch (scaleType) {
		
		case FIT_XY:
			matrix.setScale((float)getWidth() / (float)bmp.getWidth() , (float)this.getHeight() / (float)bmp.getHeight());
			break;
		
		case FIT_CENTER :
			fixFitCenter(bmp);
			break;
			
		case CENTER:
			if(bmp.getHeight() <= getHeight() && bmp.getWidth() <= getWidth()) {
				matrix.setTranslate((float) getWidth() / 2 - (float) bmp.getWidth() / 2, (float) getHeight() / 2 - (float) bmp.getHeight() / 2);
			}
			else {
				fixFitCenter(bmp);
			}
			break;
		
		default:
			matrix.setScale((float)getWidth() / (float)bmp.getWidth() , (float)this.getHeight() / (float)bmp.getHeight());
			break;
		}
	}

	private void fixFitCenter(Bitmap bmp) {
		float bmpRatioWH = (float)bmp.getWidth() / (float)bmp.getHeight();
		float viewRatioWH = (float)getWidth() / (float)getHeight();
		float showWidth, showHeight;
		if(bmpRatioWH >= viewRatioWH){
            showWidth = getWidth();
            showHeight = showWidth / bmpRatioWH;
        }
        else {
            showHeight = getHeight();
            showWidth = showHeight * bmpRatioWH;
        }
		float scale = showWidth / (float)bmp.getWidth();
		matrix.setScale(scale, scale);
		matrix.postTranslate((getWidth() - showWidth) / 2.0f, (getHeight() - showHeight) / 2.0f);
	}
}
