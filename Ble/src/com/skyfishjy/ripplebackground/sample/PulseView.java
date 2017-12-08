package com.skyfishjy.ripplebackground.sample;

import java.util.Date;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import com.example.android.bluetoothlegatt.R;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PulseView {
	private String title = "脉搏曲线图";
	private TimeSeries series;
	private XYMultipleSeriesDataset mDataset;
	private GraphicalView chart;
	private View mView;
	private TextView pulseView,titleTv;

	public PulseView(Context context) {
		mView = LayoutInflater.from(context).inflate(R.layout.pulselayout, null);
		// 这里获得main界面上的布局，下面会把图表画在这个布局里面
		titleTv = (TextView) mView.findViewById(R.id.tv_misc_title);
		titleTv.setText(title);
		LinearLayout layout = (LinearLayout)mView.findViewById(R.id.layout);
		pulseView = (TextView) mView.findViewById(R.id.curView);

		// 生成图表
		XYMultipleSeriesRenderer renderer = setRenderer();
		chart = ChartFactory.getTimeChartView(context, buildDateDataset(), renderer, "H:mm:ss");
		LinearLayout.LayoutParams li = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		li.weight = 1;
		layout.addView(chart, li);
		
	}
    
    private XYMultipleSeriesRenderer setRenderer() {
		// TODO Auto-generated method stub
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		int[] colors = new int[] { Color.RED };
		PointStyle[] styles = new PointStyle[] { PointStyle.CIRCLE };
		renderer.setAxisTitleTextSize(16);
		renderer.setChartTitleTextSize(20);
		renderer.setLabelsTextSize(15);
		renderer.setLegendTextSize(15);
		renderer.setPointSize(5f);
		renderer.setMargins(new int[] { 25, 40, 15, 15 });
		int length = colors.length;
		for (int i = 0; i < length; i++) {
			XYSeriesRenderer r = new XYSeriesRenderer();
			r.setLineWidth(3f);//折线宽度
			r.setColor(colors[i]);
			r.setPointStyle(styles[i]);
			renderer.addSeriesRenderer(r);
			((XYSeriesRenderer) renderer.getSeriesRendererAt(i)).setFillPoints(true);//折线点实心
		}
		length = renderer.getSeriesRendererCount();
		
		renderer.setXTitle("时间(s)");
		renderer.setYTitle("脉搏(次/min)");
		renderer.setGridColor(Color.parseColor("#67b8cb"));//网格
		renderer.setAxesColor(Color.BLACK);//轴
		renderer.setLabelsColor(Color.BLACK);//轴标签
		renderer.setXLabelsColor(Color.BLACK);//x轴值
		renderer.setYLabelsColor(0, Color.BLACK);//y轴值
		renderer.setYAxisMin(0);
		renderer.setYAxisMax(100);		
		renderer.setShowGrid(true);
		renderer.setXLabels(10);
		renderer.setYLabels(10);
		renderer.setYLabelsAlign(Align.RIGHT);
		renderer.setInScroll(false); // 调整大小
		renderer.setPanEnabled(false,false);//禁止报表的拖动
		renderer.setMarginsColor(Color.parseColor("#27aae1"));
		return renderer;
	}

	private XYMultipleSeriesDataset buildDateDataset() {
		mDataset = new XYMultipleSeriesDataset();
		series = new TimeSeries("脉搏");
		series.add(new Date(new Date().getTime()), 60);  //默认初始值60，可修改
		mDataset.addSeries(series);
		return mDataset;
	}
    
    void updateChart(int addy) {
    	pulseView.setText(String.valueOf(addy));
    	pulseView.setTextColor(Color.RED);
    	
    	Date[] xcache = new Date[50];
    	int[] ycache = new int[50];
    	
    	//设置好下一个需要增加的节点
    	long addX = new Date().getTime();
		int addY = addy;//需要更新
		
		//判断当前点集中到底有多少点，因为屏幕总共只能容纳100个，所以当点数超过100时，长度永远是100
		int length = series.getItemCount();
		if (length > 50) {
			length = 50;
		}
		
		//将旧的点集中x和y的数值取出来放入backup中，并且将x的值加1，造成曲线向右平移的效果
		for (int i = 0; i < length; i++) {
			xcache[i] = new Date((long) series.getX(i));
			ycache[i] = (int) series.getY(i);
		}
		//点集先清空，为了做成新的点集而准备
		series.clear();
		series.add(new Date(addX), addY);
		for (int k = 0; k < length; k++) {
    		series.add(xcache[k], ycache[k]);
    	}
		mDataset.removeSeries(series);
		mDataset.addSeries(series);
		chart.invalidate();
    }
    
    public void setChat(long[] addX, int[] addY, int averPulse){
    	pulseView.setText(String.valueOf(averPulse));
    	pulseView.setTextColor(Color.RED);
    	//点集先清空，为了做成新的点集而准备
    	series.clear();
    	for (int k = 0; k < addY.length; k++) {
    		series.add(new Date(addX[k]), addY[k]);
    	}
    	mDataset.removeSeries(series);
    	mDataset.addSeries(series);
    	chart.invalidate();
    }
    
    public void setTitleTv(String title){
    	titleTv.setTextSize(20);
    	titleTv.setText(title);
    }

	public View getmView() {
		return mView;
	}
}
