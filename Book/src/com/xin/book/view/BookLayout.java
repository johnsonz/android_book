package com.xin.book.view;

import java.util.Date;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.PorterDuff.Mode;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.GestureDetector.OnGestureListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class BookLayout extends FrameLayout {

	public static final String LOG_TAG = "com.zhang";
	private int totalPageNum;
	private Context mContext;
	private boolean hasInit = false;
	private final int defaultWidth = 600, defaultHeight = 400;
	private int contentWidth = 0;
	private int contentHeight = 0;
	private View currentPage,middlePage,nextPage,prevPage;
	private LinearLayout invisibleLayout;
	private LinearLayout mainLayout;
	private BookView mBookView;
	private Handler aniEndHandle;
	private static boolean closeBook = false;



	private Corner mSelectCorner;
	private final int clickCornerLen = 250 * 250; // 50dip，分割线
	private float scrollX = 0, scrollY = 0;
	private int indexPage = 0;


	private BookState mState;
	private Point aniStartPos;
	private Point aniStopPos;
	private Date aniStartTime;
	private long aniTime = 800;
	private long timeOffset = 10;//绘图间隔

//	private Listener mListener;
	private BookAdapter mPageAdapter;

	private GestureDetector mGestureDetector;
	private BookOnGestureListener mGestureListener;

	public BookLayout(Context context) {
		super(context);
		Init(context);
	}

	public BookLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		Init(context);
	}

	public BookLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		Init(context);
	}

	



	
	public void setPageAdapter(BookAdapter pa) {
		Log.d(LOG_TAG, "setPageAdapter");
		mPageAdapter = pa;
	}

	public void Init(Context context) {
		Log.d(LOG_TAG, "Init");
		totalPageNum = 0;
		mContext = context;
		mSelectCorner = Corner.None;

		mGestureListener = new BookOnGestureListener();
		mGestureDetector = new GestureDetector(mGestureListener);
		mGestureDetector.setIsLongpressEnabled(false);
		aniEndHandle = new Handler();

		this.setOnTouchListener(touchListener);
		this.setLongClickable(true);
	}


	protected void dispatchDraw(Canvas canvas) {
		Log.d(LOG_TAG, "dispatchDraw");
		super.dispatchDraw(canvas);
		if (!hasInit) {
			hasInit = true;
			indexPage = 0;
			if (mPageAdapter == null) {
				throw new RuntimeException("please set the PageAdapter on init");
			}
			//初始化
			totalPageNum = mPageAdapter.getCount();
			mainLayout = new LinearLayout(mContext);
			mainLayout.setLayoutParams(new LayoutParams(contentWidth, contentHeight));
			mainLayout.setBackgroundColor(0xffffffff);
			mState = BookState.READY;

			invisibleLayout = new LinearLayout(mContext);
			invisibleLayout.setLayoutParams(new LayoutParams(contentWidth, contentHeight));


			this.addView(invisibleLayout);
			this.addView(mainLayout);

			mBookView = new BookView(mContext);
			mBookView.setLayoutParams(new LayoutParams(contentWidth, contentHeight));
			this.addView(mBookView);

			updatePageView();
			invalidate();
		} else if (mState == BookState.READY) {
			mBookView.update();
		}
	}

	/**
	 * 此方法做过修改，改成一页显示
	 */
	public void updatePageView() {
		Log.d(LOG_TAG, "updatePageView");
		if (indexPage < 0 || indexPage > totalPageNum - 1) {
			return;
		}
		invisibleLayout.removeAllViews();
		mainLayout.removeAllViews();
		
		/*当前页*/
		currentPage = mPageAdapter.getView(indexPage);
		if(currentPage == null){
			currentPage = new WhiteView(mContext);
		}
		currentPage.setLayoutParams(new LayoutParams(contentWidth,contentHeight));
		mainLayout.addView(currentPage);
		
		/*背景页*/
		middlePage = new WhiteView(mContext);
		middlePage.setLayoutParams(new LayoutParams(contentWidth,contentHeight));
		invisibleLayout.addView(middlePage);
		
		/*前一页*/
		prevPage = null;
		if(indexPage>0){
			prevPage = mPageAdapter.getView(indexPage-1);
		}
		if(prevPage==null){
			prevPage = new WhiteView(mContext);
		}
		prevPage.setLayoutParams(new LayoutParams(contentWidth,contentHeight));
		invisibleLayout.addView(prevPage);
		
		/*后一页*/
		nextPage = null;
		if(indexPage<totalPageNum-1){
			nextPage = mPageAdapter.getView(indexPage + 1);
		}
		if(nextPage==null){
			nextPage = new WhiteView(mContext);
		}
		nextPage.setLayoutParams(new LayoutParams(contentWidth,contentHeight));
		invisibleLayout.addView(nextPage);
		

		Log.d(LOG_TAG, "updatePageView finish");
	}

	OnTouchListener touchListener = new OnTouchListener() {
		public boolean onTouch(View v, MotionEvent event) {
			/*
			 * 向右翻页时：
	  (0,0)  _________________________________(2*Width,0)
			|                |                |
		    |                |                |
		    |                |                |
		    | Current Page   |                |
		    |                |                |
		    |                |                |
		    |                |                |
		    |________________|________________| 
		    */
			Log.d(LOG_TAG, "onTouch " + " x: " + event.getX() + " y: " + event.getY() + " mState:" + mState);
			mGestureDetector.onTouchEvent(event);
			int action = event.getAction();
			if (action == MotionEvent.ACTION_UP && mSelectCorner != Corner.None && mState == BookState.TRACKING) {
				if (mState == BookState.ANIMATING)
					return false;
				if(mSelectCorner == Corner.LeftTop){
					if(scrollX<contentWidth/2){
						//未触发翻页
						aniStopPos = new Point(0,0);
					}else{
						//触发翻页，终点为(2*contentWidth,0)
						aniStopPos = new Point(2*contentWidth,0);
					}
				}else if(mSelectCorner == Corner.RightTop){
					if(scrollX<contentWidth/2){
						aniStopPos = new Point(-contentWidth,0);
					}else{
						aniStopPos = new Point(contentWidth,0);
					}
				}else if(mSelectCorner == Corner.LeftBottom){
					if(scrollX<contentWidth/2){
						aniStopPos = new Point(0,contentHeight);
					}else{
						aniStopPos = new Point(2*contentWidth,contentHeight);
					}
				}else if(mSelectCorner == Corner.RightBottom){
					if(scrollX<contentWidth/2){
						aniStopPos = new Point(-contentWidth,contentHeight);
					}else{
						aniStopPos = new Point(contentWidth,contentHeight);
					}
				}
				aniStartPos = new Point((int) scrollX, (int) scrollY);
				aniTime = 800;
				mState = BookState.ABOUT_TO_ANIMATE;
				closeBook = true;
				aniStartTime = new Date();
				mBookView.startAnimation();
			}
			return false;
		}
	};

	class BookOnGestureListener implements OnGestureListener {
		public boolean onDown(MotionEvent event) {
			Log.d(LOG_TAG, "onDown");
			if (mState == BookState.ANIMATING)
				return false;
			float x = event.getX(), y = event.getY();
			int w = contentWidth, h = contentHeight;
			//确定手指落点，以(250,250)为圆心，250为半径画圆
			/*
			_________________
			|      |         | 
		    |      |         |
		    |______.(250,250)|
		    |                |
		    |                |
		    |                |
		    |                | 
		    _________________
		    */
			if (x * x + y * y < clickCornerLen) {
				//左上角
				if (indexPage > 0) {
					mSelectCorner = Corner.LeftTop;
					//设置翻页起点（0,0）
					aniStartPos = new Point(0, 0);
				}
			} else if ((x - w) * (x - w) + y * y < clickCornerLen) {
				//右上角
				if (indexPage < totalPageNum - 1) {
					mSelectCorner = Corner.RightTop;
					aniStartPos = new Point(contentWidth, 0);
				}
			} else if (x * x + (y - h) * (y - h) < clickCornerLen) {
				//左下角
				if (indexPage > 0) {
					mSelectCorner = Corner.LeftBottom;
					aniStartPos = new Point(0, contentHeight);
				}
			} else if ((x - w) * (x - w) + (y - h) * (y - h) < clickCornerLen) {
				//右下角
				if (indexPage < totalPageNum - 1) {
					mSelectCorner = Corner.RightBottom;
					aniStartPos = new Point(contentWidth, contentHeight);
				}
			}
			if (mSelectCorner != Corner.None) {
				//如果手指一直在屏幕上，得到当前手指落点，也就是翻页终点（x,y）
				aniStopPos = new Point((int) x, (int) y);
				aniTime = 800;
				mState = BookState.ABOUT_TO_ANIMATE;
				closeBook = false;
				aniStartTime = new Date();
				mBookView.startAnimation();
			}
			return false;
		}

		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			Log.d(LOG_TAG, "onFling velocityX:" + velocityX + " velocityY:" + velocityY);
			if (mSelectCorner != Corner.None) {
				if (mSelectCorner == Corner.LeftTop) {
					if (velocityX < 0) {
						aniStopPos = new Point(0, 0);
					} else {
						aniStopPos = new Point(2*contentWidth, 0);
					}
				}else if( mSelectCorner == Corner.RightTop){
					if (velocityX < 0) {
						aniStopPos = new Point(-contentWidth, 0);
					} else {
						aniStopPos = new Point(contentWidth, 0);
					}
				}else if (mSelectCorner == Corner.LeftBottom ) {
					if (velocityX < 0) {
						aniStopPos = new Point(0, contentHeight);
					} else {
						aniStopPos = new Point(2*contentWidth, contentHeight);
					}
				}else if( mSelectCorner == Corner.RightBottom){
					if (velocityX < 0) {
						aniStopPos = new Point(-contentWidth, contentHeight);
					} else {
						aniStopPos = new Point(contentWidth, contentHeight);
					}
				}
				Log.d(LOG_TAG, "onFling animate");
				aniStartPos = new Point((int) scrollX, (int) scrollY);
				aniTime = 1000;
				mState = BookState.ABOUT_TO_ANIMATE;
				closeBook = true;
				aniStartTime = new Date();
				mBookView.startAnimation();
			}
			return false;
		}

		public void onLongPress(MotionEvent e) {
			Log.d(LOG_TAG, "onLongPress");
		}

		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			mState = BookState.TRACKING;
			if (mSelectCorner != Corner.None) {
				scrollX = e2.getX();
				scrollY = e2.getY();
				mBookView.startAnimation();
			}
			return false;
		}

		public void onShowPress(MotionEvent e) {
			Log.d(LOG_TAG, "onShowPress");
		}

		public boolean onSingleTapUp(MotionEvent e) {
			Log.d(LOG_TAG, "onSingleTapUp");

			if (mSelectCorner != Corner.None) {
				if (mSelectCorner == Corner.LeftTop) {
					if (scrollX < contentWidth / 2) {
						aniStopPos = new Point(0, 0);
					} else {
						aniStopPos = new Point(2*contentWidth, 0);
					}
				} else if(mSelectCorner == Corner.RightTop){
					if (scrollX < contentWidth / 2) {
						aniStopPos = new Point(-contentWidth, 0);
					} else {
						aniStopPos = new Point(contentWidth, 0);
					}
				}else if (mSelectCorner == Corner.LeftBottom) {
					if (scrollX < contentWidth / 2) {
						aniStopPos = new Point(0, contentHeight);
					} else {
						aniStopPos = new Point(2*contentWidth, contentHeight);
					}
				}else if(mSelectCorner == Corner.RightBottom){
					if (scrollX < contentWidth / 2) {
						aniStopPos = new Point(-contentWidth, contentHeight);
					} else {
						aniStopPos = new Point(contentWidth, contentHeight);
					}
				}
				aniStartPos = new Point((int) scrollX, (int) scrollY);
				aniTime = 800;
				mState = BookState.ABOUT_TO_ANIMATE;
				closeBook = true;
				aniStartTime = new Date();
				mBookView.startAnimation();
			}
			return false;
		}
	}

	protected void onFinishInflate() {
		Log.d(LOG_TAG, "onFinishInflate");
		super.onFinishInflate();
	}

	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);

		contentWidth = this.getWidth();
		contentHeight = this.getHeight();
		if (contentWidth == 0)
			contentWidth = defaultWidth;
		if (contentHeight == 0)
			contentHeight = defaultHeight;
		Log.d(LOG_TAG, "onLayout, width:" + contentWidth + " height:" + contentHeight);
	}



	class BookView extends SurfaceView implements SurfaceHolder.Callback {
		DrawThread dt;
		SurfaceHolder surfaceHolder;
		Paint mDarkPaint = new Paint();
		Paint mPaint = new Paint();
		Bitmap tmpBmp = Bitmap.createBitmap(contentWidth, contentHeight, Bitmap.Config.ARGB_8888);
		Canvas mCanvas = new Canvas(tmpBmp);

		Paint bmpPaint = new Paint();
		Paint ivisiblePaint = new Paint();

		public BookView(Context context) {
			super(context);
			surfaceHolder = getHolder();
			//Add a Callback interface for this holder. There can several Callback interfaces associated to a holder.
			surfaceHolder.addCallback(this);

			mDarkPaint.setColor(0x88000000);
			/*
			 * Android中提供了Shader类专门用来渲染图像以及一些几何图形，Shader下面包括几个直接子类，分别是BitmapShader、 ComposeShader、
			 *LinearGradient、RadialGradient、SweepGradient。 BitmapShader主要用来渲染图像，LinearGradient 用来进行梯度渲染，RadialGradient 
			 *用来进行环形渲染，SweepGradient 用来进行梯度渲染，ComposeShader则是一个 混合渲染，可以和其它几个子类组合起来使用。 
			 *
			 * 创建LinearGradient并设置渐变的颜色数组 说明一下这几个参数  
	         * 第一个 起始的x坐标 
	         * 第二个 起始的y坐标 
	         * 第三个 结束的x坐标 
	         * 第四个 结束的y坐标 
	         * 第五个 颜色数组 
	         * 第六个 这个也是一个数组用来指定颜色数组的相对位置 如果为null 就沿坡度线均匀分布 
	         * 第七个 渲染模式 ，平铺方式，这里设置为镜像
	         * */  
			Shader mLinearGradient = new LinearGradient(0, 0, contentWidth, 0, new int[] { 0x00000000, 0x33000000,
					0x00000000 }, new float[] { 0.35f, 0.5f, 0.65f }, Shader.TileMode.MIRROR);
			////设置是否使用抗锯齿功能
			mPaint.setAntiAlias(true);
			
			//设置渲染对象
			mPaint.setShader(mLinearGradient);
			
			//如果该项设置为true，则图像在动画进行中会滤掉对Bitmap图像的优化操作，加快显示   
		    //速度，本设置项依赖于dither和xfermode的设置   
			bmpPaint.setFilterBitmap(true);
			//设置是否使用抗锯齿功能，会消耗较大资源，绘制图形速度会变慢。   
			bmpPaint.setAntiAlias(true);

			//设置绘制图形的透明度。   
			ivisiblePaint.setAlpha(0);
			ivisiblePaint.setFilterBitmap(true);
			ivisiblePaint.setAntiAlias(true);
            
			
			//设置图形重叠时的处理方式，如合并，取交集或并集，经常用来制作橡皮的擦除效果
			//PorterDuffXfermode是一个非常强大的转换模式，它可以使用图像合成的16条Porter-Duff规则的任意一条来控制Paint如何与已有的Canvas图像进行交互。
			//这里使用Mode.DST_IN规则：取两层绘制交集。显示下层。
			/*			
			1.PorterDuff.Mode.CLEAR          所绘制不会提交到画布上。
			2.PorterDuff.Mode.SRC            显示上层绘制图片
			3.PorterDuff.Mode.DST            显示下层绘制图片
			4.PorterDuff.Mode.SRC_OVER       正常绘制显示，上下层绘制叠盖。
			5.PorterDuff.Mode.DST_OVER       上下层都显示。下层居上显示。
			6.PorterDuff.Mode.SRC_IN         取两层绘制交集。显示上层。
			7.PorterDuff.Mode.DST_IN         取两层绘制交集。显示下层。
			8.PorterDuff.Mode.SRC_OUT        取上层绘制非交集部分。
			9.PorterDuff.Mode.DST_OUT        取下层绘制非交集部分。
			10.PorterDuff.Mode.SRC_ATOP      取下层非交集部分与上层交集部分
			11.PorterDuff.Mode.DST_ATOP      取上层非交集部分与下层交集部分
			12.PorterDuff.Mode.XOR			  取上层和下层非交集部分 
			13.PorterDuff.Mode.DARKEN        [Sa + Da - Sa*Da, Sc*(1 - Da) + Dc*(1 - Sa) + min(Sc, Dc)]  
			14.PorterDuff.Mode.LIGHTEN       [Sa + Da - Sa*Da, Sc*(1 - Da) + Dc*(1 - Sa) + max(Sc, Dc)]  
            15.PorterDuff.Mode.MULTIPLY      [Sa * Da, Sc * Dc]，  取交集部分，将交集部分两张图片的对应的点的像素相乘，再除以255，然后以新的像素来重新绘制显示合成后的图像  
            16.PorterDuff.Mode.SCREEN        [Sa + Da - Sa * Da, Sc + Dc - Sc * Dc]  
            */
			ivisiblePaint.setXfermode( new PorterDuffXfermode(Mode.DST_IN));
		}
        /*
         * 开始翻页
         */
		public void startAnimation() {
			if (dt == null) {
				Log.d(LOG_TAG, "startAnimation");
				dt = new DrawThread(this, getHolder());
				dt.start();
			}
		}

		public void stopAnimation() {
			Log.d(LOG_TAG, "stopAnimation");
			if (dt != null) {
				dt.flag = false;
				Thread t = dt;
				dt = null;
				t.interrupt();
			}
		}

		public void drawLT(Canvas canvas) {
			double dx = contentWidth - scrollX, dy = scrollY;
			double len = Math.sqrt(dx * dx + dy * dy);
			if (len > contentWidth) {
				scrollX = (float)(contentWidth - contentWidth*dx/len);
				scrollY = (float)(contentWidth*dy/len);
			}

			double px = scrollX;
			double py = scrollY;
			double arc = 2 * Math.atan(py / px) * 180 / Math.PI;

			Matrix m = new Matrix();
			m.postTranslate(scrollX - contentWidth, scrollY);
			m.postRotate((float) (arc), scrollX, scrollY);

			middlePage.draw(mCanvas);

			Paint ps = new Paint();
			/*
			 *Shader.TileMode.CLAMP 	replicate the edge color if the shader draws outside of its original bounds  
			 *Shader.TileMode.MIRROR 	repeat the shader's image horizontally and vertically, alternating mirror images 
			 *                          so that adjacent images always seam  
			 *Shader.TileMode.REPEAT 	repeat the shader's image horizontally and vertically  
			*/			
			Shader lg1 = new LinearGradient(contentWidth , 0, contentWidth - (float) px, (float) py, new int[] {
					0x00000000, 0x33000000, 0x00000000 }, new float[] { 0.35f, 0.5f, 0.65f }, Shader.TileMode.CLAMP);
			ps.setShader(lg1);
			mCanvas.drawRect(0, 0, contentWidth , contentHeight, ps);
			canvas.drawBitmap(tmpBmp, m, bmpPaint);

			prevPage.draw(mCanvas);
			Shader lg2 = new LinearGradient(scrollX, scrollY, 0, 0, new int[] { 0x00000000, 0x33000000, 0x00000000 },
					new float[] { 0.35f, 0.5f, 0.65f }, Shader.TileMode.CLAMP);
			ps.setShader(lg2);
			mCanvas.drawRect(0, 0, contentWidth , contentHeight, ps);

			arc = arc * Math.PI / 360;
			Path path = new Path();
			double r = Math.sqrt(px * px + py * py);
			double p1 = r / (2 * Math.cos(arc));
			double p2 = r / (2 * Math.sin(arc));
			Log.d(LOG_TAG, "p1: " + p1 + " p2:" + p2);
			if (arc == 0) {
				path.moveTo((float) p1, 0);
				path.lineTo(contentWidth , 0);
				path.lineTo(contentWidth , contentHeight);
				path.lineTo((float) p1, contentHeight);
				path.close();
			} else if (p2 > contentHeight || p2 < 0) {
				double p3 = (p2 - contentHeight) * Math.tan(arc);
				path.moveTo((float) p1, 0);
				path.lineTo(contentWidth , 0);
				path.lineTo(contentWidth , contentHeight);
				path.lineTo((float) p3, contentHeight);
				path.close();
			} else {
				path.moveTo((float) p1, 0);
				path.lineTo(contentWidth , 0);
				path.lineTo(contentWidth , contentHeight);
				path.lineTo(0, contentHeight);
				path.lineTo(0, (float) p2);
				path.close();
			}
			mCanvas.drawPath(path, ivisiblePaint);
			canvas.drawBitmap(tmpBmp, 0, 0, null);
		}

		public void drawLB(Canvas canvas) {
			double dx = contentWidth - scrollX, dy = contentHeight-scrollY;
			double len = Math.sqrt(dx * dx + dy * dy);
			if (len > contentWidth ) {
				scrollX = (float) (contentWidth-contentWidth * dx /len);
				scrollY = (float) (contentHeight-contentWidth * dy / len);
			}
			double px = scrollX;
			double py = contentHeight - scrollY;
			double arc = 2 * Math.atan(py / px) * 180 / Math.PI;

			Matrix m = new Matrix();
			m.postTranslate(scrollX - contentWidth , scrollY - contentHeight);
			m.postRotate((float) (-arc), scrollX, scrollY);

			middlePage.draw(mCanvas);

			Paint ps = new Paint();
			Shader lg1 = new LinearGradient(contentWidth , contentHeight, contentWidth  - (float) px,
					contentHeight - (float) py, new int[] { 0x00000000, 0x33000000, 0x00000000 }, new float[] { 0.35f,
							0.5f, 0.65f }, Shader.TileMode.CLAMP);
			ps.setShader(lg1);
			mCanvas.drawRect(0, 0, contentWidth, contentHeight, ps);
			canvas.drawBitmap(tmpBmp, m, bmpPaint);

			prevPage.draw(mCanvas);
			Shader lg2 = new LinearGradient(scrollX, scrollY, 0, contentHeight, new int[] { 0x00000000, 0x33000000,
					0x00000000 }, new float[] { 0.35f, 0.5f, 0.65f }, Shader.TileMode.CLAMP);
			ps.setShader(lg2);
			mCanvas.drawRect(0, 0, contentWidth, contentHeight, ps);

			arc = arc * Math.PI / 360;
			Path path = new Path();
			double r = Math.sqrt(px * px + py * py);
			double p1 = r / (2 * Math.cos(arc));
			double p2 = r / (2 * Math.sin(arc));
			Log.d(LOG_TAG, "p1: " + p1 + " p2:" + p2);
			if (arc == 0) {
				path.moveTo((float) p1, 0);
				path.lineTo(contentWidth , 0);
				path.lineTo(contentWidth , contentHeight);
				path.lineTo((float) p1, contentHeight);
				path.close();
			} else if (p2 > contentHeight || p2 < 0) {
				double p3 = (p2 - contentHeight) * Math.tan(arc);
				path.moveTo((float) p3, 0);
				path.lineTo(contentWidth , 0);
				path.lineTo(contentWidth , contentHeight);
				path.lineTo((float) p1, contentHeight);
				path.close();
			} else {
				path.moveTo(0, 0);
				path.lineTo(contentWidth , 0);
				path.lineTo(contentWidth , contentHeight);
				path.lineTo((float) p1, contentHeight);
				path.lineTo(0, contentHeight - (float) p2);
				path.close();
			}
			mCanvas.drawPath(path, ivisiblePaint);
			canvas.drawBitmap(tmpBmp, 0, 0, null);
		}

		public void drawRT(Canvas canvas) {
			double dx = scrollX , dy = scrollY;
			double len = Math.sqrt(dx * dx + dy * dy);
			if (len > contentWidth) {
				scrollX = (float) (contentWidth * dx /len);
				scrollY = (float) (contentWidth * dy / len);
			}

			double px = contentWidth - scrollX;
			double py = scrollY;
			double arc = 2 * Math.atan(py / px) * 180 / Math.PI;

			Matrix m = new Matrix();
			m.postTranslate(scrollX, scrollY);
			m.postRotate((float) (-arc), scrollX, scrollY);

			middlePage.draw(mCanvas);

			Paint ps = new Paint();
			Shader lg1 = new LinearGradient(0, 0, (float) px, (float) py, new int[] { 0x00000000, 0x33000000,
					0x00000000 }, new float[] { 0.35f, 0.5f, 0.65f }, Shader.TileMode.CLAMP);
			ps.setShader(lg1);
			mCanvas.drawRect(0, 0, contentWidth, contentHeight, ps);
			canvas.drawBitmap(tmpBmp, m, bmpPaint);

			nextPage.draw(mCanvas);
			Shader lg2 = new LinearGradient(scrollX - contentWidth, scrollY, contentWidth, 0, new int[] {
					0x00000000, 0x33000000, 0x00000000 }, new float[] { 0.35f, 0.5f, 0.65f }, Shader.TileMode.CLAMP);
			ps.setShader(lg2);
			mCanvas.drawRect(0, 0, contentWidth, contentHeight, ps);

			arc = arc * Math.PI / 360;
			Path path = new Path();
			double r = Math.sqrt(px * px + py * py);
			double p1 = contentWidth - r / (2 * Math.cos(arc));
			double p2 = r / (2 * Math.sin(arc));
			Log.d(LOG_TAG, "p1: " + p1 + " p2:" + p2);
			if (arc == 0) {
				path.moveTo(0, 0);
				path.lineTo((float) p1, 0);
				path.lineTo((float) p1, contentHeight);
				path.lineTo(0, contentHeight);
				path.close();
			} else if (p2 > contentHeight || p2 < 0) {
				double p3 = contentWidth  - (p2 - contentHeight) * Math.tan(arc);
				path.moveTo(0, 0);
				path.lineTo((float) p1, 0);
				path.lineTo((float) p3, contentHeight);
				path.lineTo(0, contentHeight);
				path.close();
			} else {
				path.moveTo(0, 0);
				path.lineTo((float) p1, 0);
				path.lineTo(contentWidth , (float) p2);
				path.lineTo(contentWidth , contentHeight);
				path.lineTo(0, contentHeight);
				path.close();
			}
			mCanvas.drawPath(path, ivisiblePaint);
			canvas.drawBitmap(tmpBmp, 0 , 0, null);
		}

		public void drawRB(Canvas canvas) {
			double dx = scrollX , dy = contentHeight - scrollY;
			double len = Math.sqrt(dx * dx + dy * dy);
			if (len > contentWidth ) {
				scrollX = (float) (contentWidth * dx /len);
				scrollY = (float) (contentHeight-contentWidth * dy / len);
			}		

			double px = contentWidth - scrollX;
			double py = contentHeight - scrollY;
			double arc = 2 * Math.atan(py / px) * 180 / Math.PI;

			Matrix m = new Matrix();
			m.postTranslate(scrollX, scrollY - contentHeight);
			m.postRotate((float) (arc), scrollX, scrollY);

			middlePage.draw(mCanvas);

			Paint ps = new Paint();
			Shader lg1 = new LinearGradient(0, contentHeight, (float) px, contentHeight - (float) py, new int[] {
					0x00000000, 0x33000000, 0x00000000 }, new float[] { 0.35f, 0.5f, 0.65f }, Shader.TileMode.CLAMP);
			ps.setShader(lg1);
			mCanvas.drawRect(0, 0, contentWidth , contentHeight, ps);
			canvas.drawBitmap(tmpBmp, m, bmpPaint);

			nextPage.draw(mCanvas);
			Shader lg2 = new LinearGradient(scrollX - contentWidth , scrollY, contentWidth , contentHeight,
					new int[] { 0x00000000, 0x33000000, 0x00000000 }, new float[] { 0.35f, 0.5f, 0.65f },
					Shader.TileMode.CLAMP);
			ps.setShader(lg2);
			mCanvas.drawRect(0, 0, contentWidth , contentHeight, ps);

			arc = arc * Math.PI / 360;
			Path path = new Path();
			double r = Math.sqrt(px * px + py * py);
			double p1 = contentWidth  - r / (2 * Math.cos(arc));
			double p2 = r / (2 * Math.sin(arc));
			Log.d(LOG_TAG, "p1: " + p1 + " p2:" + p2);
			if (arc == 0) {
				path.moveTo(0, 0);
				path.lineTo((float) p1, 0);
				path.lineTo((float) p1, contentHeight);
				path.lineTo(0, contentHeight);
				path.close();
			} else if (p2 > contentHeight || p2 < 0) {
				double p3 = contentWidth  - (p2 - contentHeight) * Math.tan(arc);
				path.moveTo(0, 0);
				path.lineTo((float) p3, 0);
				path.lineTo((float) p1, contentHeight);
				path.lineTo(0, contentHeight);
				path.close();
			} else {
				path.moveTo(0, 0);
				path.lineTo(contentWidth , 0);
				path.lineTo(contentWidth , contentHeight - (float) p2);
				path.lineTo((float) p1, contentHeight);
				path.lineTo(0, contentHeight);
				path.close();
			}
			mCanvas.drawPath(path, ivisiblePaint);
			canvas.drawBitmap(tmpBmp, 0 , 0, null);
		}

		public void drawPrevPageEnd(Canvas canvas) {
			prevPage.draw(mCanvas);
			canvas.drawBitmap(tmpBmp, 0, 0, null);
		}

		public void drawNextPageEnd(Canvas canvas) {
			nextPage.draw(mCanvas);
			canvas.drawBitmap(tmpBmp, contentWidth, 0, null);
		}

		public void drawPage(Canvas canvas) {
			if (mSelectCorner == Corner.LeftTop) {
				Log.d(LOG_TAG, "click left top");
				drawLT(canvas);
			} else if (mSelectCorner == Corner.LeftBottom) {
				Log.d(LOG_TAG, "click left bottom");
				drawLB(canvas);
			} else if (mSelectCorner == Corner.RightTop) {
				Log.d(LOG_TAG, "click right top");
				drawRT(canvas);
			} else if (mSelectCorner == Corner.RightBottom) {
				Log.d(LOG_TAG, "click right bottom");
				drawRB(canvas);
			}
		}

		public void update() {
			Canvas canvas = surfaceHolder.lockCanvas(null);
			try {
				synchronized (surfaceHolder) {
					doDraw(canvas); 
				}
			} catch (Exception e) {
				e.printStackTrace(); 
			} finally {
				if (canvas != null) { 
					surfaceHolder.unlockCanvasAndPost(canvas);
				}
			}
		}

		protected void doDraw(Canvas canvas) {
			Log.d(LOG_TAG, "bookView doDraw");
			mainLayout.draw(canvas);

		}

		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

		}

		public void surfaceCreated(SurfaceHolder holder) {
			update();
		}

		public void surfaceDestroyed(SurfaceHolder holder) {
			if (dt != null) {
				dt.flag = false; 
				dt = null; 
			}
		}
	}

	public boolean getAnimateData() {
		Log.d(LOG_TAG, "getAnimateData");
		long time = aniTime;
		Date date = new Date();
		//Returns this Date as a millisecond value
		long t = date.getTime() - aniStartTime.getTime();
		Log.d(LOG_TAG, t+"");
		t += timeOffset;
		Log.d(LOG_TAG, t+"");
		if (t < 0 || t > time) {
			mState = BookState.ANIMATE_END;
			return false;
		} else {
			mState = BookState.ANIMATING;
			double sx = aniStopPos.x - aniStartPos.x;
			//计算起点至终点在间隔时间内X轴需移动的距离
			scrollX =(float)(sx*t/time+aniStartPos.x);
			double sy = aniStopPos.y-aniStartPos.y;
			//计算起点至终点在间隔时间内Y轴需移动的距离
			scrollY = (float)(sy*t/time+aniStartPos.y);
			return true;
		}
	}

	public void handleAniEnd(Canvas canvas) {
		Log.d(LOG_TAG, "handleAniEnd");
		if (closeBook) {
			closeBook = false;
			//左上角或左下角
			if (mSelectCorner == Corner.LeftTop || mSelectCorner == Corner.LeftBottom) {
				//设置是否触发翻页
				if (scrollX > contentWidth / 2) {
					indexPage -= 1;
					//绘前一页
					mBookView.drawPrevPageEnd(canvas);
					//Causes the Runnable r to be added to the message queue. The runnable will be run on the thread to which this handler is attached. 
					//但要注意此Runnable运行在主线程中，也就是说会阻塞主线程
					aniEndHandle.post(new Runnable() {
						public void run() {
							//更新当前画面
							updatePageView();
						}
					});
				} else {
					mBookView.doDraw(canvas);
				}
			} else if (mSelectCorner == Corner.RightTop || mSelectCorner == Corner.RightBottom) {
				if (scrollX < contentWidth / 2) {
					indexPage += 1;
					mBookView.drawNextPageEnd(canvas);
					aniEndHandle.post(new Runnable() {
						public void run() {
							updatePageView();
						}
					});
				} else {
					mBookView.doDraw(canvas);
				}
			}
			mSelectCorner = Corner.None;
			mState = BookState.READY;
		} else {
			mState = BookState.TRACKING;
		}
		mBookView.stopAnimation();
//		aniEndHandle.post(new Runnable() {
//			public void run() {
////				BookLayout.this.invalidate();
//			}
//		});
	}

	class WhiteView extends View {
		public WhiteView(Context context) {
			super(context);
		}

		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			canvas.drawColor(Color.WHITE);
		}
	}



	public class DrawThread extends Thread {
		BookView bv; 
		SurfaceHolder surfaceHolder;
		boolean flag = false; 
		int sleepSpan = 30; 

		public DrawThread(BookView bv, SurfaceHolder surfaceHolder) {
			this.bv = bv;
			this.surfaceHolder = surfaceHolder; 
			this.flag = true; 
		}

		public void run() {
			Canvas canvas = null;
			while (flag) {
				try {
					//锁定画布的某个区域进行画图，画完图后，会调用下面的unlockCanvasAndPost来改变显示内容
					//内存要求比较高的情况下，建议参数不要为null
					canvas = surfaceHolder.lockCanvas(null);
					if (canvas == null)
						continue;
					synchronized (surfaceHolder) {
						if (mState == BookState.ABOUT_TO_ANIMATE || mState == BookState.ANIMATING) {
							//将布局控件等绘于手机屏幕
							bv.doDraw(canvas);
							//得到X、Y在特定时间内移动的距离
							getAnimateData();
							//绘制翻页效果
							bv.drawPage(canvas);
						} else if (mState == BookState.TRACKING) {
							bv.doDraw(canvas);
							bv.drawPage(canvas);
						} else if (mState == BookState.ANIMATE_END) {
							handleAniEnd(canvas);
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); 
				} finally {
					if (canvas != null) {
						//结束锁定画图，并提交改变
						surfaceHolder.unlockCanvasAndPost(canvas);
					}
				}
				try {
					Thread.sleep(sleepSpan); 
				} catch (Exception e) {
					e.printStackTrace(); 
				}
			}
		}
	}


}

