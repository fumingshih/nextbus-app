package com.danielstiner.cyride.util;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
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

import com.danielstiner.cyride.util.NextBusAPI.RouteStop;
import com.danielstiner.cyride.util.NextBusAPI.StopPrediction;

import android.graphics.Color;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

public class NextBusAPI {

	public interface CachePolicy {

		boolean shouldUpdateStopPredictions(StopPrediction c);

		boolean shouldUpdateStops(Date lastUpdate);

		void saveCache(Cache cache);

		Cache loadCache();

	}

	public static class Cache implements Serializable {
		public Cache(String agency) {
			this.agency = agency;
		}

		private String agency;

		private static final long serialVersionUID = -5003315853529912609L;

		private Map<String, Route> mRoutes = new HashMap<String, NextBusAPI.Route>();

		private Map<RouteStop, StopPrediction> mStopRoutePredictionsCache = new HashMap<NextBusAPI.RouteStop, NextBusAPI.StopPrediction>();

		private Map<String, Stop> mStopsByTitle = new HashMap<String, NextBusAPI.Stop>();

		private Date mStopsLastUpdated;
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

		String routeTag;
		String tag;
		public final String title;

		public Stop(String title) {
			this.title = title;
		}
	}

	public static class StopPrediction implements Serializable {
		private static final long serialVersionUID = -5798823753170327173L;

		public StopPrediction(RouteStop routestop) {
			this.routestop = routestop;
			this.route = routestop.route;
			this.stop = routestop.stop;
		}

		public final List<Prediction> predictions = new LinkedList<Prediction>();
		public final RouteStop routestop;
		public final Route route;
		public final Stop stop;
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

	private CachePolicy mCachePolicy;
	private Cache mCache;

	public NextBusAPI(String agency, CachePolicy cachingPolicy) {
		mCachePolicy = cachingPolicy;
		setAgency(agency);
	}

	private void setAgency(String agency) {
		mAgency = agency;

		mCache = mCachePolicy.loadCache();
		if (mCache == null || !agency.equals(mCache.agency))
			mCache = new Cache(agency);
	}

	public void shutdown() {
		mCachePolicy.saveCache(mCache);
	}

	private StopPrediction getCachedPredictions(Stop s, Route r) {
		return mCache.mStopRoutePredictionsCache.get(new RouteStop(r, s));
	}

	private Route getRoute(String routeTag, String routeTitle, int routeColor) {
		Route r = mCache.mRoutes.get(routeTag);
		if (r == null) {
			r = new Route(routeTag, routeTitle, routeColor);
			mCache.mRoutes.put(routeTag, r);
		}

		return r;
	}

	private RouteStop getRouteStop(String stopTitle, String routeTag) {
		Route r = mCache.mRoutes.get(routeTag);
		Stop s = mCache.mStopsByTitle.get(stopTitle);

		// TODO Better errors
		if (r == null) {
			throw new IllegalArgumentException();
		}
		if (s == null) {
			throw new IllegalArgumentException();
		}

		return new RouteStop(r, s);
	}

	public Collection<StopPrediction> getStopPredictions(
			final Collection<Stop> stops) throws MalformedURLException,
			DocumentException {

		final Collection<StopPrediction> predictions = new ArrayList<NextBusAPI.StopPrediction>();

		StringBuilder queryParams = new StringBuilder();
		for (Stop s : stops) {
			for (Route r : s.routes) {

				StopPrediction c = getCachedPredictions(s, r);

				if (mCachePolicy.shouldUpdateStopPredictions(c)) {
					queryParams.append("&stops=");
					queryParams.append(r.tag);
					queryParams.append("|");
					queryParams.append(s.tag);
				} else {
					predictions.add(c);
				}
			}
		}

		if (0 == queryParams.length())
			return predictions;

		final URL u = Urls.getMultiStopUrl(this.mAgency,
				queryParams.substring(1));

		SAXReader reader = new SAXReader();
		Document predictions_document = reader.read(u);

		Collection<StopPrediction> newPredictions = parsePredictions(predictions_document);

		for (StopPrediction p : newPredictions) {
			setCachedPredictions(p);
		}
		predictions.addAll(newPredictions);

		return predictions;
	}

	public Collection<Stop> getStops() {

		if (mCachePolicy.shouldUpdateStops(mCache.mStopsLastUpdated)) {
			updateRouteConfig();
		}

		return mCache.mStopsByTitle.values();
	}

	private List<StopPrediction> parsePredictions(Document predictions_document) {
		List<StopPrediction> predictions = new ArrayList<StopPrediction>();

		Element root = predictions_document.getRootElement();

		// iterate through child elements of root
		for (Iterator<Element> i = root.elementIterator("predictions"); i
				.hasNext();) {
			Element predictionsElement = i.next();

			String routeTag = predictionsElement.attributeValue("routeTag");
			String routeTitle = predictionsElement.attributeValue("routeTitle");
			String stopTitle = predictionsElement.attributeValue("stopTitle");
			String dirTitleBecauseNoPredictions = predictionsElement
					.attributeValue("dirTitleBecauseNoPredictions");
			String agencyTitle = predictionsElement
					.attributeValue("agencyTitle");

			if (null == predictionsElement.element("direction"))
				continue;

			StopPrediction p = new StopPrediction(getRouteStop(stopTitle,
					routeTag));

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

	private Route parseRouteElement(Element routeElement) {
		String routeTag = routeElement.attributeValue("tag");
		String routeTitle = routeElement.attributeValue("title");
		int routeColor = Color.parseColor("#"
				+ routeElement.attributeValue("color"));
		return getRoute(routeTag, routeTitle, routeColor);
	}

	private Collection<Stop> parseStops(Document stops_document) {

		Collection<Stop> stops = new ArrayList<Stop>();

		Element root = stops_document.getRootElement();

		// iterate through child elements of root
		for (Iterator<Element> i = root.elementIterator("route"); i.hasNext();) {
			Element routeElement = i.next();

			Route r = parseRouteElement(routeElement);

			for (Iterator<Element> j = routeElement.elementIterator("stop"); j
					.hasNext();) {
				Element stopElement = j.next();

				Stop s = new Stop(stopElement.attributeValue("title"));
				s.tag = stopElement.attributeValue("tag");
				s.latitude = Double.parseDouble(stopElement
						.attributeValue("lat"));
				s.longitude = Double.parseDouble(stopElement
						.attributeValue("lon"));
				s.routes.add(r);
				stops.add(s);
			}
		}

		return stops;
	}

	private void setCachedPredictions(StopPrediction p) {
		mCache.mStopRoutePredictionsCache
				.put(new RouteStop(p.route, p.stop), p);
	}

	private void updateRouteConfig() {
		mCache.mStopsLastUpdated = new Date();

		URL u;
		try {
			u = Urls.getRouteConfigUrl(this.mAgency);

			SAXReader reader = new SAXReader();
			Document stops_document;
			stops_document = reader.read(u);

			cacheStops(parseStops(stops_document));

		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void cacheStops(Collection<Stop> stops) {
		mCache.mStopsByTitle.clear();

		for (Stop s : stops) {
			mCache.mStopsByTitle.put(s.title, s);
		}
	}

	public StopPrediction getRouteStopPrediction(RouteStop rs)
			throws MalformedURLException, DocumentException {

		StopPrediction c = getCachedPredictions(rs.stop, rs.route);

		if (mCachePolicy.shouldUpdateStopPredictions(c)) {

			final URL u = Urls.getMultiStopUrl(this.mAgency, "&stops="
					+ rs.route.tag + "|" + rs.stop.tag);

			SAXReader reader = new SAXReader();
			Document predictions_document = reader.read(u);

			Collection<StopPrediction> newPredictions = parsePredictions(predictions_document);

			for (StopPrediction p : newPredictions) {
				setCachedPredictions(p);
			}

			c = newPredictions.iterator().next();

		}

		return c;
	}
}
