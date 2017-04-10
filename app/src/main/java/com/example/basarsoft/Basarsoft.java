/*
Author: Erdem Adacal
Used Libs: JTS(WKBReader), WebSocket
*/
package com.example.basarsoft;

import java.net.URI;
import java.net.URISyntaxException;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.ParseException;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import android.widget.Toast;

public class Basarsoft extends AppCompatActivity
        implements OnMapLongClickListener,OnMapReadyCallback,OnMarkerDragListener {

    private TextView mTapTextView;
    private WebSocketClient mWebSocketClient;
    private GoogleMap mMap;
    private Marker first;
    private Marker second;
    private int pointnum=0;

    private static class Counter {
        private int clickCount;

        public Counter() {
            clickCount = 0;
        }

        public void incrementClickCount() {
            clickCount++;
        }
    }

    private void connectWebSocket() {
        URI uri;
        try {
            uri = new URI("ws://212.156.70.230:9060/TASK/service.js");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");
                mWebSocketClient.send("Hello from " + Build.MANUFACTURER + " " + Build.MODEL);
            }
            @Override
            public void onMessage(String s) {
                final String message = s;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(count.clickCount==0)
                        {
                        }
                        else
                        {
                            try {
                                WKBParser(message);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
            }
        };
        mWebSocketClient.connect();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mTapTextView = (TextView) findViewById(R.id.tap_text);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        new Background().execute();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerDragListener(this);
    }

    private void addObjectsToMap(LatLng point) {
        if (count.clickCount == 1)
            first = mMap.addMarker(new MarkerOptions().position(point).title("Point " + count.clickCount));
        else
        {
            second = mMap.addMarker(new MarkerOptions().position(point).title("Point " + count.clickCount).draggable(true));
            first.setDraggable(true);
        }
    }
    private void addNewPoint(LatLng point) {
        mMap.addMarker(new MarkerOptions().position(point).title("New Point"));
    }
    private void WKBParser(String s) throws ParseException {
        pointnum=s.length()/120;
        int x=0;
        int y=120;
        for(int i=0; i<pointnum;i++)
        {
            String point= s.substring(x,y);
            String real= "0101000021"+ point.substring(4,44);
        final GeometryFactory gm = new GeometryFactory(new PrecisionModel(), 4326);
        final WKBReader wkbr = new WKBReader(gm);

            byte[] wkbBytes = wkbr.hexToBytes(real);
            final Geometry geom = wkbr.read(wkbBytes);
            LatLng newP = new LatLng(geom.getCoordinate().y,geom.getCoordinate().x);
            addNewPoint(newP);
            x+=120;
            y+=120;
        }
        String add= pointnum + " points added!";
        Toast.makeText(this, add, Toast.LENGTH_SHORT).show();
        }

    Counter count = new Counter();

    private boolean checkReady() {
        if (mMap == null) {
            Toast.makeText(this, R.string.map_not_ready, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public void onResetMap(View view) {
        if (!checkReady()) {
            return;
        }
        mMap.clear();
        count.clickCount = 0;
        mTapTextView.setText(R.string.tap_instructions);
    }

    public void onServer(View view) {
        if (!checkReady()) {
            return;
        }
        if (count.clickCount < 2)
            Toast.makeText(this, R.string.notYet, Toast.LENGTH_SHORT).show();
        else {
            double minX, maxX, minY, maxY;
            double y1 = first.getPosition().latitude;
            double y2 = second.getPosition().latitude;
            double x1 = first.getPosition().longitude;
            double x2 = second.getPosition().longitude;
            if (x1 < x2) {
                minX = x1;
                maxX = x2;
            } else {
                maxX = x1;
                minX = x2;
            }
            if (y1 < y2) {
                minY = y1;
                maxY = y2;
            } else {
                maxY = y1;
                minY = y2;
            }
            maxX = Math.floor(maxX * 1000000) / 1000000;
            minX = Math.floor(minX * 1000000) / 1000000;
            maxY = Math.floor(maxY * 1000000) / 1000000;
            minY = Math.floor(minY * 1000000) / 1000000;
            mTapTextView.setText("Four Points of the Minimum Bounding Rectangle --> " + minX + ", " + minY + ", " + maxX + ", " + maxY);
            mWebSocketClient.send(minX + ", " + minY + ", " + maxX + ", " + maxY);
        }

    }

    @Override
    public void onMapLongClick(LatLng point) {
        if (count.clickCount < 2) {
            count.incrementClickCount();
            if (count.clickCount == 1)
                mTapTextView.setText("Point" + count.clickCount + ":" + point);
            if (count.clickCount == 2)
                mTapTextView.append("\nPoint" + count.clickCount + ":" + point);
            addObjectsToMap(point);
        } else
            Toast.makeText(this, R.string.only2points, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
    }
    @Override
    public void onMarkerDragEnd(Marker marker) {
        mTapTextView.setText("Point 1:" + first.getPosition() + "\nPoint 2:" + second.getPosition());
    }

    @Override
    public void onMarkerDrag(final Marker marker) {
        if (first.equals(marker))
            mTapTextView.setText("Current Position of Point 1: " + first.getPosition() + "\nPoint 2:" + second.getPosition());
        else
            mTapTextView.setText("Point 1:" + first.getPosition() + "\nCurrent Position of Point 2: " + marker.getPosition());
    }

    private class Background extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params)
        {
            connectWebSocket();
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
        }
        @Override
        protected void onPreExecute()
        {
        }
    }
}