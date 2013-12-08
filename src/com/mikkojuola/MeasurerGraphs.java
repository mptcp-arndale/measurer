package com.mikkojuola;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.util.Log;

import com.mikkojuola.Interfaces;
import java.util.HashMap;
import java.util.regex.*;
import java.util.Set;
import java.util.Iterator;

import org.achartengine.model.*;
import org.achartengine.renderer.*;
import org.achartengine.GraphicalView;
import org.achartengine.ChartFactory;

public class MeasurerGraphs extends Activity
{
    private static final int UPDATE_EVERY_N_SECONDS = 1;
    private Interfaces interfaces;

    private GraphicalView trafficChart;
    private XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();

    private class Series
    {
        private XYSeries currentSeries;
        private XYSeriesRenderer currentRenderer;
        private long last_y = -1;

        private void add( long x, long y )
        {
            currentSeries.add( x, y );
        }
    }

    private HashMap<String, Series> ifaceSeries = new HashMap<String, Series>();
    private int colors[] = { 0xffff0000
                           , 0xff00ff00
                           , 0xff0000ff
                           , 0xffffff00
                           , 0xffff00ff
                           , 0xff00ffff
                           , 0xffffffff };
    private int curcolor = 0;

    private Series obtainChart(String iface)
    {
        if ( ifaceSeries.containsKey(iface) ) {
            return ifaceSeries.get(iface);
        }

        XYSeries currentSeries = new XYSeries(iface);
        dataset.addSeries(currentSeries);
        XYSeriesRenderer currentRenderer = new XYSeriesRenderer();
        renderer.addSeriesRenderer(currentRenderer);

        currentRenderer.setColor(colors[curcolor++]);

        Series s = new Series();
        s.currentSeries = currentSeries;
        s.currentRenderer = currentRenderer;

        ifaceSeries.put( iface, s );
        return s;
    }

    protected void onResume() {
        super.onResume();

        LinearLayout chart = (LinearLayout) findViewById( R.id.chart );
        if ( trafficChart == null ) {
            trafficChart = ChartFactory.getCubeLineChartView( this
                                                            , dataset
                                                            , renderer
                                                            , 0.3f );
            chart.addView( trafficChart );
        } else {
            trafficChart.repaint();
        }
    }

    private long measuredFirst = 500;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        interfaces = new Interfaces();

        final Handler handler = new Handler();

        Runnable r = new Runnable() {
                @Override
                public void run() {
                    long now = interfaces.getCurrentTime();

                    HashMap<String, Interfaces.Traffic> traffic =
                        interfaces.getTraffic();
                    Set<String> ifaces = interfaces.getInterfaces();
                    Iterator<String> it = ifaces.iterator();

                    while ( it.hasNext() ) {
                        String iface = it.next();
                        Series s = obtainChart(iface);

                        Interfaces.Traffic t = traffic.get(iface);
                        for (HashMap.Entry<Long, Long> entry : t.deltas.entrySet()) {
                            if ( entry.getKey() > measuredFirst ) {
                                s.add( entry.getKey()
                                     , entry.getValue() );
                            }
                        }

                        handler.postDelayed(this, UPDATE_EVERY_N_SECONDS * 1000);
                    }
                    trafficChart.repaint();
                    measuredFirst = now;
                }
        };

        handler.postDelayed(r, UPDATE_EVERY_N_SECONDS * 1000);
    }
}

