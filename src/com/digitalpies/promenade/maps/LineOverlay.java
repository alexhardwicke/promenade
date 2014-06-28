package com.digitalpies.promenade.maps;

import java.util.ArrayList;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

/**
 * A custom Overlay used to draw routes on the map.
 * 
 * @author Alex Hardwicke
 */
public class LineOverlay extends Overlay
{
	private ArrayList<GeoPoint> geoPoints;
	private Projection projection;
	private Paint line;
	
	public LineOverlay(ArrayList<GeoPoint> geoPoints, Projection projection)
	{
		this.geoPoints = geoPoints;
		this.projection = projection;
		
		// Create a Paint, and set the settings as needed
		this.line = new Paint();
		this.line.setDither(true);
		this.line.setColor(0xaa33B5E5); // ICS blue
		this.line.setAntiAlias(true);
		this.line.setStyle(Paint.Style.STROKE);
		this.line.setStrokeJoin(Paint.Join.ROUND);
		this.line.setStrokeCap(Paint.Cap.ROUND);
		this.line.setStrokeWidth(5);
	}
	
	/**
	 * Draws a path for the provided geoPoints onto the map.<br>
	 * <br>
	 * Creates a Point array and path object. It then creates a Point object for each GeoPoint, and converts the
	 * geoPoints to pixels which is stored in the Point array.<br>
	 * <br>
	 * It then iterates through the point array in reverse, moving to the last point, adding a line to the next
	 * point in the Path object, and so on until it's added a line to the final point.<br>
	 * <br>
	 * The path object and line style are then drawn onto the canvas.
	 */
	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow)
	{
		super.draw(canvas, mapView, shadow);

		// Create a Point array the same size as the GeoPoint array, and a Path object.
		Point[] points = new Point[this.geoPoints.size()];
		Path path = new Path();
		
		// Set up a Point for each GeoPoint, and convert the GeoPoint to Pixel format to go into the point.
		for (int i = 0; i < this.geoPoints.size(); i++)
		{
			points[i] = new Point();
			this.projection.toPixels(this.geoPoints.get(i), points[i]);
		}
		
		// Add each Point to the path.
		for (int i = points.length - 1; i > 0; i--)
		{
			path.moveTo(points[i].x, points[i].y);
			path.lineTo(points[i - 1].x, points[i - 1].y - 1);
		}

		// Draw the path onto the map.
		canvas.drawPath(path, this.line);
	}
}
