package com.danielstiner.cyride;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final String AGENCY = "cyride";
	private static final String nextbus_url_base = "http://webservices.nextbus.com/service/publicXMLFeed";

	private static final String routes_url = nextbus_url_base
			+ "?command=routeConfig&terse=true&a=" + AGENCY;

	private static final String multistop_url = nextbus_url_base
			+ "?command=predictionsForMultiStops&terse=true&a=" + AGENCY;

	private static final int MAX_STOPS = 4;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onResume() {

		Button b = (Button) findViewById(R.id.button);

		// b.
		update();

		super.onResume();
	}

	private void update() {
		new ShowNearbyTask().execute();
	}

	private double[] getGPS() {
		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		List<String> providers = lm.getProviders(true);

		/*
		 * Loop over the array backwards, and if you get an accurate location,
		 * then break out the loop
		 */
		Location l = null;

		for (int i = providers.size() - 1; i >= 0; i--) {
			l = lm.getLastKnownLocation(providers.get(i));
			if (l != null)
				break;
		}

		double[] gps = new double[2];
		if (l != null) {
			gps[0] = l.getLatitude();
			gps[1] = l.getLongitude();
		}
		return gps;
	}

	private class ShowNearbyTask extends AsyncTask<Void, Void, Void> {

		private String result = null;

		protected Void doInBackground(Void... urls) {

			URL u;
			try {
				u = new URL(routes_url);
				SAXReader reader = new SAXReader();
				Document stops_document = reader.read(u);
				List<Stop> stops = parseStops(stops_document);

				double[] location = getGPS();

				String multiurl = multistop_url;
				List<Stop> closest_stops = filterStops(stops, location[0],
						location[1], MAX_STOPS);
				for (Stop s : closest_stops) {
					multiurl += "&stops=" + s.routeTag + "|" + s.tag;
				}
				u = new URL(multiurl);
				Document predictions_document = reader.read(u);
				List<Prediction> predictions = parsePredictions(predictions_document);

				this.result = "";
				for (Prediction p : predictions) {
					this.result += p.seconds + "s at " + p.stopTitle + " for "
							+ p.routeTitle + "\n";
				}

			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			} catch (DocumentException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}

			return null;

		}

		protected void onPostExecute(Void result) {
			TextView tv = (TextView) findViewById(R.id.text);
			tv.setText(this.result);
		}
	}

	private static List<Stop> filterStops(List<Stop> stops, final double lat,
			final double lng, int maxStops) {
		PriorityQueue<Stop> sortedStops = new PriorityQueue<Stop>(stops.size(),
				new Comparator<Stop>() {

					@Override
					public int compare(Stop lhs, Stop rhs) {
						Double lh_dist = Math.abs(lhs.latitude - lat)
								+ Math.abs(lhs.longitude - lng);
						Double rh_dist = Math.abs(rhs.latitude - lat)
								+ Math.abs(rhs.longitude - lng);
						return lh_dist.compareTo(rh_dist);
					}
				});

		sortedStops.addAll(stops);

		List<Stop> newStops = new ArrayList<Stop>();
		HashSet<String> stopIds = new HashSet<String>();
		while (stopIds.size() < maxStops) {
			Stop s = sortedStops.poll();
			newStops.add(s);
			stopIds.add(s.tag);
		}

		return newStops;
	}

	private static List<Stop> parseStops(Document stops_document) {

		List<Stop> stops = new ArrayList<Stop>();

		Element root = stops_document.getRootElement();

		// iterate through child elements of root
		for (Iterator<Element> i = root.elementIterator("route"); i.hasNext();) {
			Element routeElement = i.next();

			String routeTag = routeElement.attributeValue("tag");

			for (Iterator<Element> j = routeElement.elementIterator("stop"); j
					.hasNext();) {
				Element stopElement = j.next();

				Stop s = new Stop();
				s.routeTag = routeTag;
				s.tag = stopElement.attributeValue("tag");
				s.latitude = Double.parseDouble(stopElement
						.attributeValue("lat"));
				s.longitude = Double.parseDouble(stopElement
						.attributeValue("lon"));
				s.title = stopElement.attributeValue("title");
				stops.add(s);
			}

		}

		return stops;

	}

	private static List<Prediction> parsePredictions(
			Document predictions_document) {
		List<Prediction> predictions = new ArrayList<Prediction>();

		Element root = predictions_document.getRootElement();

		// iterate through child elements of root
		for (Iterator<Element> i = root.elementIterator("predictions"); i
				.hasNext();) {
			Element predictionsElement = i.next();

			String routeTitle = predictionsElement.attributeValue("routeTitle");
			String stopTitle = predictionsElement.attributeValue("stopTitle");

			if (null == predictionsElement.element("direction"))
				continue;

			for (Iterator<Element> j = predictionsElement.element("direction")
					.elementIterator("prediction"); j.hasNext();) {
				Element predictionElement = j.next();

				Prediction p = new Prediction();
				p.seconds = Integer.parseInt(predictionElement
						.attributeValue("seconds"));
				p.routeTitle = routeTitle;
				p.stopTitle = stopTitle;
				predictions.add(p);
			}

		}

		return predictions;
	}
}
