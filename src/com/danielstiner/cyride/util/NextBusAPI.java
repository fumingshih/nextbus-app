package com.danielstiner.cyride.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import android.location.Location;

public class NextBusAPI {

	public List<Stop> getStops(final String agencyString)
			throws MalformedURLException, DocumentException {
		final URL u = Urls.getRouteConfigUrl(agencyString);
		SAXReader reader = new SAXReader();
		Document stops_document = reader.read(u);
		return parseStops(stops_document);
	}

//	public Future<List<Stop>> getStopsFutures(final String agencyString) {
//		return new FutureTask<List<Stop>>(new Callable<List<Stop>>() {
//			@Override
//			public List<Stop> call() throws Exception {
//				return getStopsSync(agencyString);
//			}
//		});
//	}

	public static class Route {
		String title;
		String direction;
	}

	public static class Stop {
		String title;
		double latitude;
		double longitude;
		String tag;
		String routeTag;
	}

	public static class StopPrediction {
		Date arrival;
		String stopTitle;
		Stop stop;

		@Override
		public String toString() {
			return stopTitle;
		}
	}

	private static class Urls {

		private static final String BASE_URL = "http://webservices.nextbus.com/service/publicXMLFeed";

		private static URL getRouteConfigUrl(String agencyString)
				throws MalformedURLException {
			return new URL(BASE_URL + "?command=routeConfig&terse=true&a="
					+ agencyString);
		}

		private static URL getMultiStopUrl(String agencyString,
				String queryParameters) throws MalformedURLException {
			return new URL(BASE_URL
					+ "?command=predictionsForMultiStops&terse=true&a="
					+ agencyString + "&" + queryParameters);
		}
	}

	public List<StopPrediction> getStopPredictions(final List<Stop> stops,
			final String agencyString) throws MalformedURLException,
			DocumentException {

		StringBuilder queryParams = new StringBuilder();
		for (Stop s : stops) {
			queryParams.append("&stops=");
			queryParams.append(s.routeTag);
			queryParams.append("|");
			queryParams.append(s.tag);
		}

		final URL u = Urls.getMultiStopUrl(agencyString,
				queryParams.substring(1));

		SAXReader reader = new SAXReader();
		Document predictions_document = reader.read(u);
		return parsePredictions(predictions_document);
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

	private static List<StopPrediction> parsePredictions(
			Document predictions_document) {
		List<StopPrediction> predictions = new ArrayList<StopPrediction>();

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

				StopPrediction p = new StopPrediction();
				// p.seconds = Integer.parseInt(predictionElement
				// .attributeValue("seconds"));
				// p.routeTitle = routeTitle;
				p.stopTitle = stopTitle;
				predictions.add(p);
			}

		}

		return predictions;
	}

	public static FutureTask<List<Stop>> nearestStops(
			final Future<List<Stop>> stops, final Location location,
			final int maxStops) {
		return new FutureTask<List<Stop>>(new Callable<List<Stop>>() {
			@Override
			public List<Stop> call() throws Exception {
				return nearestStops(stops.get(), location, maxStops);
			}
		});
	}

	public static List<Stop> nearestStops(List<Stop> stops, Location location,
			int maxStops) {
		final double lat = location.getLatitude();
		final double lng = location.getLongitude();
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

	public FutureTask<List<StopPrediction>> getStopPredictions(
			final Future<List<Stop>> stops, final String agencyString) {
		return new FutureTask<List<StopPrediction>>(
				new Callable<List<StopPrediction>>() {
					@Override
					public List<StopPrediction> call() throws Exception {
						return getStopPredictions(stops.get(), agencyString);
					}
				});
	}
}
