package com.fndroid.threedtouchdemo;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
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
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			int progress = (int) msg.obj;
			mCover.setImageAlpha(progress);
		}
	};
	private View newView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initData();
		initViews();
	}

	private void initViews() {
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
		Bitmap screenBitmap = getScreenImage();
		float percent = (float) 300 / screenBitmap.getHeight(); // 计算以300为高度的缩放百分比
		mCover.setImageBitmap(blur(getSmallSizeBitmap(screenBitmap, percent), 15f)); //
		// 根据缩放百分比得到图片再模糊处理
		mCover.setVisibility(View.VISIBLE);
		mCover.setImageAlpha(0);
		new Thread(new Runnable() {
			int progress = 150;

			@Override
			public void run() {
				while (progress < 255) {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Message msg = new Message();
					msg.obj = progress++;
					mHandler.sendMessage(msg);
				}
			}
		}).start();
		showView(position, view);
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
		startAnimate(mCardView); // 播放动画
	}

	private void startAnimate(CardView cardView) {
		PropertyValuesHolder pyhScaleX = PropertyValuesHolder.ofFloat("scaleX", 0.5f, 1.05f);
		PropertyValuesHolder pyhScaleY = PropertyValuesHolder.ofFloat("scaleY", 0.5f, 1.05f);
		ObjectAnimator animator_out = ObjectAnimator.ofPropertyValuesHolder(mCardView, pyhScaleX,
				pyhScaleY); // 同时缩放X和Y
		animator_out.setInterpolator(new AccelerateDecelerateInterpolator());
		animator_out.setDuration(200);
		PropertyValuesHolder pyhScaleX2 = PropertyValuesHolder.ofFloat("scaleX", 1.05f, 1f);
		PropertyValuesHolder pyhScaleY2 = PropertyValuesHolder.ofFloat("scaleY", 1.05f, 1f);
		ObjectAnimator animator_in = ObjectAnimator.ofPropertyValuesHolder(mCardView, pyhScaleX2,
				pyhScaleY2);
		animator_in.setInterpolator(new AccelerateDecelerateInterpolator());
		animator_in.setDuration(100);

		AnimatorSet animatorSet = new AnimatorSet();
		animatorSet.playSequentially(animator_out, animator_in); // 按顺序执行两个动画
		animatorSet.start();
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
		mCover.setVisibility(GONE);
		newView.setVisibility(GONE);
		mCardView.setVisibility(GONE);
	}
}
