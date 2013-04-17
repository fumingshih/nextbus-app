package com.danielstiner.cyride.util;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import android.graphics.Color;
import android.location.Location;

public class NextBusAPI {

	public interface CachePolicy {

		boolean shouldUpdateStopPredictions(StopPrediction c);

		boolean shouldUpdateStops(Date lastUpdate);

	}

	public static class Prediction implements Serializable {
		private static final long serialVersionUID = -2480643929223384105L;

		public Date arrival;

		public Prediction(Date arrival) {
			this.arrival = arrival;
		}
	}

	public static class Route implements Serializable {
		private static final long serialVersionUID = 7521922708491520903L;

		public final int color;

		public String direction;
		public final String tag;
		public final String title;

		public Route(String tag, String title, int color) {
			this.tag = tag;
			this.title = title;
			this.color = color;
		}
	}

	public static class RouteStop implements Serializable {
		private static final long serialVersionUID = 7072146394309144554L;

		public final Route route;
		public final Stop stop;

		public RouteStop(Route r, Stop s) {
			this.route = r;
			this.stop = s;
		}
	}

	public static class Stop implements Serializable {
		private static final long serialVersionUID = -426734608965295351L;

		double latitude;

		double longitude;

		public final List<Route> routes = new LinkedList<NextBusAPI.Route>();

		public final String tag;
		public final String title;

		public Stop(String tag, String title, double latitude, double longitude) {
			this.title = title;
			this.tag = tag;
			this.latitude = latitude;
			this.longitude = longitude;
		}
	}

	public static class StopPrediction implements Serializable {
		private static final long serialVersionUID = -5798823753170327173L;

		public final List<Prediction> predictions = new LinkedList<Prediction>();

		public final Route route;
		public final RouteStop routestop;
		public final Stop stop;

		public StopPrediction(RouteStop routestop) {
			this.routestop = routestop;
			this.route = routestop.route;
			this.stop = routestop.stop;
		}
	}

	private static class Urls {

		private static final String BASE_URL = "http://webservices.nextbus.com/service/publicXMLFeed";

		private static URL getMultiStopUrl(String agencyString,
				String queryParameters) throws MalformedURLException {
			return new URL(
					BASE_URL
							+ "?command=predictionsForMultiStops&useShortTitles=true&terse=true&a="
							+ agencyString + "&" + queryParameters);
		}

		private static URL getRouteConfigUrl(String agencyString)
				throws MalformedURLException {
			return new URL(BASE_URL
					+ "?command=routeConfig&useShortTitles=true&terse=true&a="
					+ agencyString);
		}
	}

	public static Collection<Stop> nearestStopPerRoute(Collection<Stop> stops,
			Location l) {

		final Map<Route, Stop> stopsByRoute = new HashMap<NextBusAPI.Route, NextBusAPI.Stop>();

		for (Stop s : stops) {
			// TODO: More advanced filtering based on how long it would take to
			// walk to the stop from the user's current location

			for (Route r : s.routes) {
				Stop ss = stopsByRoute.get(r);
				if (ss == null
						|| LocationUtil.distance(s.latitude, s.longitude, l) < LocationUtil
								.distance(ss.latitude, ss.longitude, l))
					stopsByRoute.put(r, s);
			}

		}

		return stopsByRoute.values();
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

	private String mAgency;

	public NextBusAPI(String agency) {
		mAgency = agency;
	}

	public Collection<StopPrediction> getStopPredictions(
			final Collection<Stop> stops) throws MalformedURLException,
			DocumentException {

		final Collection<RouteStop> routeStops = new ArrayList<RouteStop>();

		for (Stop s : stops) {
			for (Route r : s.routes) {
				routeStops.add(new RouteStop(r, s));
			}
		}

		return getRouteStopPredictions(routeStops);
	}

	public Collection<StopPrediction> getRouteStopPredictions(
			final Collection<RouteStop> routeStops)
			throws MalformedURLException, DocumentException {

		final Collection<StopPrediction> predictions = new ArrayList<StopPrediction>();

		final Map<String, RouteStop> routeStopMap = new HashMap<String, RouteStop>();

		if (routeStops == null || routeStops.isEmpty())
			return predictions;

		StringBuilder queryParams = new StringBuilder();
		for (RouteStop rs : routeStops) {
			queryParams.append("&stops=");
			queryParams.append(rs.route.tag);
			queryParams.append("|");
			queryParams.append(rs.stop.tag);

			routeStopMap.put(keyForRouteStopTags(rs.stop.tag, rs.route.tag), rs);
		}

		final URL u = Urls.getMultiStopUrl(this.mAgency,
				queryParams.substring(1));

		SAXReader reader = new SAXReader();
		Document predictions_document = reader.read(u);

		Collection<StopPrediction> newPredictions = parsePredictions(
				predictions_document, routeStopMap);

		predictions.addAll(newPredictions);

		return predictions;
	}

	public Collection<Stop> getStops() throws MalformedURLException,
			DocumentException {

		URL u = Urls.getRouteConfigUrl(this.mAgency);

		SAXReader reader = new SAXReader();
		Document stops_document;
		stops_document = reader.read(u);

		return parseStops(stops_document);
	}

	private static String keyForRouteStopTags(String stopTag, String routeTag) {
		return routeTag + "|" + stopTag;
	}

	private static List<StopPrediction> parsePredictions(
			Document predictions_document, Map<String, RouteStop> routeStopMap) {
		List<StopPrediction> predictions = new ArrayList<StopPrediction>();

		Element root = predictions_document.getRootElement();

		// iterate through child elements of root
		for (Iterator<Element> i = root.elementIterator("predictions"); i
				.hasNext();) {
			Element predictionsElement = i.next();

			String routeTag = predictionsElement.attributeValue("routeTag");
			String routeTitle = predictionsElement.attributeValue("routeTitle");
			String stopTag = predictionsElement.attributeValue("stopTag");
			String stopTitle = predictionsElement.attributeValue("stopTitle");
			// String dirTitleBecauseNoPredictions = predictionsElement
			// .attributeValue("dirTitleBecauseNoPredictions");
			// String agencyTitle = predictionsElement
			// .attributeValue("agencyTitle");

			if (null == predictionsElement.element("direction"))
				continue;

			StopPrediction p = new StopPrediction(getRouteStop(stopTag,
					routeTag, routeStopMap));

			for (Iterator<Element> j = predictionsElement.element("direction")
					.elementIterator("prediction"); j.hasNext();) {
				Element predictionElement = j.next();

				Date arrival = new Date();
				arrival.setTime(Long.parseLong(predictionElement
						.attributeValue("epochTime")));

				p.predictions.add(new Prediction(arrival));

			}

			predictions.add(p);

		}

		return predictions;
	}

	private static RouteStop getRouteStop(String stopTag, String routeTag,
			Map<String, RouteStop> routeStopMap) {
		// TODO Null check
		return routeStopMap.get(keyForRouteStopTags(stopTag, routeTag));
	}

	private static Route getRoute(String routeTag, String routeTitle,
			int routeColor) {
		return new Route(routeTag, routeTitle, routeColor);
	}

	private static Route parseRouteElement(Element routeElement) {
		String routeTag = routeElement.attributeValue("tag");
		String routeTitle = routeElement.attributeValue("title");
		int routeColor = Color.parseColor("#"
				+ routeElement.attributeValue("color"));
		return getRoute(routeTag, routeTitle, routeColor);
	}

	private static Collection<Stop> parseStops(Document stops_document) {

		Collection<Stop> stops = new ArrayList<Stop>();

		Element root = stops_document.getRootElement();

		// iterate through child elements of root
		for (Iterator<Element> i = root.elementIterator("route"); i.hasNext();) {
			Element routeElement = i.next();

			Route r = parseRouteElement(routeElement);

			for (Iterator<Element> j = routeElement.elementIterator("stop"); j
					.hasNext();) {
				Element stopElement = j.next();

				String title = stopElement.attributeValue("title");
				String tag = stopElement.attributeValue("tag");
				double lat = Double.parseDouble(stopElement
						.attributeValue("lat"));
				double lon = Double.parseDouble(stopElement
						.attributeValue("lon"));

				Stop s = new Stop(tag, title, lat, lon);
				s.routes.add(r);
				stops.add(s);
			}
		}

		return stops;
	}
}
