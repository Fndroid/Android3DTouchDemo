package com.fndroid.threedtouchdemo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static android.view.View.GONE;

public class MainActivity extends AppCompatActivity implements AdapterView
		.OnItemLongClickListener, View.OnClickListener {
	private static final String TAG = "MainActivity";
	private LinearLayout root;
	private ImageView mCover;
	private FrameLayout mRoot;
	private ListView mListView;
	private Toolbar mToolbar;
	private CardView mCardView;
	private List<Map<String, String>> data;
	private View newView;
	private GestureDetector mGestureDetector;
	private AnimatorSet showCardAnimationSet;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initData();
		initViews();
	}

	private void initViews() {
		mGestureDetector = new GestureDetector(this, new MyOnGestureListener());
		mRoot = (FrameLayout) findViewById(R.id.activity_main);
		root = (LinearLayout) findViewById(R.id.root);
		mCover = (ImageView) findViewById(R.id.cover);
		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(mToolbar);
		mCardView = (CardView) findViewById(R.id.cv);
		mCardView.setVisibility(GONE);
		mCover.setVisibility(GONE);
		mCover.setOnClickListener(this);
		mListView = (ListView) findViewById(R.id.lv);
		mListView.setOnItemLongClickListener(this);
		mListView.setOnTouchListener(new View.OnTouchListener() {
			private float downY;
			private float distanceY;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						distanceY = 0;
						downY = event.getY();
						break;
					case MotionEvent.ACTION_UP:
						Log.d(TAG, "onTouch: distance=" + distanceY);
						if (distanceY < -10) {
							FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)
									mCardView.getLayoutParams();
							layoutParams.topMargin = 10;
							mCardView.setLayoutParams(layoutParams);
						} else {
							if (showCardAnimationSet != null && showCardAnimationSet.isRunning()) {
								showCardAnimationSet.cancel();
							}
							ObjectAnimator reverseAnimation = getReverseAnimation(mCardView);
							reverseAnimation.start();
							reverseAnimation.addListener(new AnimatorListenerAdapter() {
								@Override
								public void onAnimationEnd(Animator animation) {
									mCardView.setAlpha(1);
									clearTop();
								}
							});
						}
						break;
					case MotionEvent.ACTION_MOVE:
						FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)
								mCardView.getLayoutParams();
						distanceY = event.getY() - downY;
						layoutParams.topMargin = (int) event.getY();
						Log.d(TAG, "onTouch: " + downY + " " + event.getY());
						mCardView.setLayoutParams(layoutParams);
						break;
				}
				return false;
			}
		});
		mListView.setAdapter(new SimpleAdapter(this, data, R.layout.item, new String[]{"name"},
				new int[]{R.id.item_tv}));
	}

	private void initData() {
		data = new ArrayList<>();
		for (int i = 0; i < 50; i++) {
			Map<String, String> map = new HashMap<>();
			map.put("name", "BYXD" + i);
			data.add(map);
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		final int num = position;
		final View itemView = view;
		final Bitmap screenBitmap = getScreenImage();
		final float percent = (float) 300 / screenBitmap.getHeight(); // 计算以300为高度的缩放百分比

		Observable.create(new Observable.OnSubscribe<Bitmap>() {
			@Override
			public void call(Subscriber<? super Bitmap> subscriber) {
				Bitmap bitmap = blur(getSmallSizeBitmap(screenBitmap, percent), 25f);
				subscriber.onNext(bitmap);
			}
		}).subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Action1<Bitmap>() {
			@Override
			public void call(Bitmap bitmap) {
				mCover.setImageBitmap(bitmap);
				mCover.setVisibility(View.VISIBLE);
				mCover.setImageAlpha(0);
				showView(num, itemView);
			}
		});

		Observable.interval(10, TimeUnit.MILLISECONDS, Schedulers.newThread()).map(new Func1<Long,
				Integer>() {
			@Override
			public Integer call(Long aLong) {
				return Integer.parseInt(aLong.toString());
			}
		}).take(26).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Integer>() {
			@Override
			public void call(Integer integer) {
				mCover.setImageAlpha(integer * 10 + 5);
			}
		});
		return true;
	}

	// 显示对应的卡片
	private void showView(int position, View view) {
		newView = LayoutInflater.from(this).inflate(R.layout.item, null); // 加载Itme的布局
		TextView tv = (TextView) newView.findViewById(R.id.item_tv); // 获取对应控件
		tv.setText(data.get(position).get("name")); // 将Item对应控件的值设置回去
		newView.setBackgroundColor(Color.WHITE);
		// 设置卡片的样式，位置通过margintop来计算
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(view.getWidth() - 30, view
				.getHeight());
		params.topMargin = (int) (view.getY() + mToolbar.getHeight()); //
		// 卡片的marginTop设置为item的Y加上toolbar的高度
		params.leftMargin = 15;
		params.rightMargin = 15;
		mCardView.setVisibility(View.VISIBLE);
		mCardView.setLayoutParams(params);
		mCardView.addView(newView, view.getLayoutParams()); // 把View加载进CardView，并设置样式为item样式
		showCardAnimationSet = getAnimationSet(mCardView); // 播放动画
		showCardAnimationSet.start();
	}

	private AnimatorSet getAnimationSet(CardView cardView) {
		PropertyValuesHolder pyhScaleX = PropertyValuesHolder.ofFloat("scaleX", 0.5f, 1.05f);
		PropertyValuesHolder pyhScaleY = PropertyValuesHolder.ofFloat("scaleY", 0.5f, 1.05f);
		ObjectAnimator animator_out = ObjectAnimator.ofPropertyValuesHolder(cardView, pyhScaleX,
				pyhScaleY); // 同时缩放X和Y
		animator_out.setInterpolator(new AccelerateDecelerateInterpolator());
		animator_out.setDuration(200);
		PropertyValuesHolder pyhScaleX2 = PropertyValuesHolder.ofFloat("scaleX", 1.05f, 1f);
		PropertyValuesHolder pyhScaleY2 = PropertyValuesHolder.ofFloat("scaleY", 1.05f, 1f);
		ObjectAnimator animator_in = ObjectAnimator.ofPropertyValuesHolder(cardView, pyhScaleX2,
				pyhScaleY2);
		animator_in.setInterpolator(new AccelerateDecelerateInterpolator());
		animator_in.setDuration(100);

		AnimatorSet animatorSet = new AnimatorSet();
		animatorSet.playSequentially(animator_out, animator_in); // 按顺序执行两个动画
		return animatorSet;
	}

	private ObjectAnimator getReverseAnimation(CardView cardView) {
		float scaleX = cardView.getScaleX();
		float scaleY = cardView.getScaleY();
		PropertyValuesHolder pyhAlpha = PropertyValuesHolder.ofFloat("alpha", 0.8f, 0);
		PropertyValuesHolder pyhScaleX = PropertyValuesHolder.ofFloat("scaleX", scaleX, 0f);
		PropertyValuesHolder pyhScaleY = PropertyValuesHolder.ofFloat("scaleY", scaleY, 0f);
		ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(cardView, pyhScaleX,
				pyhScaleY, pyhAlpha); // 同时缩放X和Y
		animator.setInterpolator(new AccelerateDecelerateInterpolator());
		animator.setDuration(200);
		return animator;
	}

	private Bitmap getScreenImage() { // 截取一张屏幕的图片
		View view = root;
		view.setBackgroundColor(Color.WHITE);
		view.setDrawingCacheEnabled(true);
		view.buildDrawingCache();
		Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache(), 0, 0, view.getWidth(), view
				.getHeight());
		view.destroyDrawingCache();
		return bitmap;
	}

	private Bitmap getSmallSizeBitmap(Bitmap source, float percent) {
		if (percent > 1 || percent <= 0) {
			throw new IllegalArgumentException("percent must be > 1 and <= 0");
		}
		Matrix matrix = new Matrix();
		matrix.setScale(percent, percent);
		return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix,
				true);
	}

	private Bitmap blur(Bitmap bitmap, float radius) {
		Bitmap output = Bitmap.createBitmap(bitmap); // 创建输出图片
		RenderScript rs = RenderScript.create(this); // 构建一个RenderScript对象
		ScriptIntrinsicBlur gaussianBlue = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs)); //
		// 创建高斯模糊脚本
		Allocation allIn = Allocation.createFromBitmap(rs, bitmap); // 开辟输入内存
		Allocation allOut = Allocation.createFromBitmap(rs, output); // 开辟输出内存
		gaussianBlue.setRadius(radius); // 设置模糊半径，范围0f<radius<=25f
		gaussianBlue.setInput(allIn); // 设置输入内存
		gaussianBlue.forEach(allOut); // 模糊编码，并将内存填入输出内存
		allOut.copyTo(output); // 将输出内存编码为Bitmap，图片大小必须注意
		rs.destroy(); // 关闭RenderScript对象，API>=23则使用rs.releaseAllContexts()
		return output;
	}

	@Override
	public void onClick(View v) {
		clearTop();
	}

	private void clearTop() {
		mCover.setVisibility(GONE);
		newView.setVisibility(GONE);
		mCardView.setVisibility(GONE);
	}


}
