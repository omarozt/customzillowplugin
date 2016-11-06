package com.microstrategy.custom.sdk.web.zillow;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.microstrategy.custom.sdk.web.addons.PropertiesSupport;
import com.microstrategy.custom.sdk.web.callreporttask.ExecuteReportTask;
import com.microstrategy.custom.sdk.web.log.Log;
import com.microstrategy.utils.log.Level;

public class CallZillowWS {

	String SqFt = null;
	String lastUpdated = null;
	String amount = null;
	String useCode = null;
	String lotSizeSqFt = null;
	String yearBuilt = null;
	String bathrooms = null;
	String zpid = null;
	String bedrooms = null;
	String lastSoldDate = null;
	String lastSoldPrice = null;
	String high = null;
	String low = null;
	String change30day = null;
	String linkUrl = null;

	String url = null;
	String path = "http://www.zillow.com/webservice/GetDeepSearchResults.htm?";
	String params = "";

	public HashMap<String, String> getInfoFromZillow(String memberCardID,
			String sessionState, String webServerName) {
		String methodName = "getInfoFromZillow";

		ExecuteReportTask reportTask = new ExecuteReportTask();
		String[] addressTab = reportTask.getAddress(memberCardID, sessionState,
				webServerName);
		//add zillow web services id from properties file
		PropertiesSupport properties = PropertiesSupport.getInstance();
		String zwsId = properties.getProperty("zws-id");

		if (addressTab.length > 0 && addressTab != null) {
			params = "zws-id="+zwsId+"&address=" + addressTab[0]
					+ "&citystatezip=" + addressTab[1];
			
			System.out.println("parameters sent ot Zillow: " + params);
			Log.logger.logp(Level.SEVERE, CallZillowWS.class.getName(),
					methodName, "parameters sent ot Zillow (first http call): " +
						path + params);

			HashMap<String, String> map = new HashMap<String, String>();

			// get first response witht text data
			getHttpResponse(path, params);

			map.put("Bathrooms", bathrooms);
			map.put("SqFt", SqFt);
			map.put("Lot size", lotSizeSqFt);
			map.put("Property type", useCode);
			map.put("Year built", yearBuilt);
			map.put("Value", amount);
			map.put("Last updated", lastUpdated);
			map.put("Bedrooms", bedrooms);
			map.put("LastSoldDate", lastSoldDate);
			map.put("LastSoldPrice", lastSoldPrice);
			map.put("RangeHigh", high);
			map.put("RangeLow", low);
			map.put("30dayChange", change30day);
			map.put("LinkUrl", linkUrl);

			// get response with graph image
			String path1 = "http://www.zillow.com/webservice/GetChart.htm?";
			String params1 = "zws-id=X1-ZWz1d4xgkzqm17_2qbsp&unit-type=percent&zpid="
					+ zpid + "&width=300&height=150";

			Log.logger.logp(Level.SEVERE, CallZillowWS.class.getName(),
					methodName, "parameters sent ot Zillow (second call): " +
						path + params1);

			getHttpResponse(path1, params1);

			// add url for img
			map.put("url", url);
			return map;

		} else {
			Log.logger.logp(Level.SEVERE, CallZillowWS.class.getName(),
					methodName, "TaskProc sent an empty response.");

			return null;
		}

	}

	private String getHttpResponse(String path, String params) {
		String methodName = "getHttpResponse";

		String response = null;
		try {
			
			System.out.println("URL: " + path + params);
			URL tempUrl = new URL(path);
			HttpURLConnection urlConn = (HttpURLConnection) tempUrl
					.openConnection();
			urlConn.setDoOutput(true);
			OutputStreamWriter out = new OutputStreamWriter(
					urlConn.getOutputStream());

			out.write(params);

			out.close();

			int responseCode = urlConn.getResponseCode();
			Log.logger.logp(Level.SEVERE, CallZillowWS.class.getName(),
					methodName, "response code from zillow:" + responseCode);

			if (responseCode == 200) {
				BufferedReader bufferReader = new BufferedReader(
						new InputStreamReader(urlConn.getInputStream(), "UTF-8"));

				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = bufferReader.readLine()) != null) {
					sb.append(line + "\n");
				}

				String stream = sb.toString();
				stream = stream.replaceAll("&amp;", "00amp;");

				Log.logger.logp(Level.SEVERE, CallZillowWS.class.getName(),
						methodName, "resonse from zillow:" + stream);

				InputStream inputStream;
				inputStream = new ByteArrayInputStream(stream.getBytes());
				parseXML(inputStream);
			}
		} catch (IOException e) {
			Log.logger.logp(Level.SEVERE, CallZillowWS.class.getName(),
					methodName, e.getLocalizedMessage());
		}
		return response;

	}

	private void parseXML(InputStream stream) {

		final String methodName = "parseXML";

		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			SAXParser saxParser = factory.newSAXParser();

			DefaultHandler handler = new DefaultHandler() {

				boolean bfinishedSqFt = false;
				boolean buseCode = false;
				boolean bbathrooms = false;
				boolean blotSizeSqFt = false;
				boolean byearBuilt = false;
				boolean bamount = false;
				boolean blastUpdated = false;
				boolean bzpid = false;
				boolean burl = false;
				boolean bbedrooms = false;
				boolean bchange30day = false;
				boolean blastSoldPrice = false;
				boolean blastSoldDate = false;
				boolean bhigh = false;
				boolean blow = false;
				boolean bgraphsanddata = false;

				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException {

					System.out.println("parameters sent ot Zillow: " + params);
					Log.logger.logp(Level.SEVERE, CallZillowWS.class.getName(),
							methodName, "Start Element :" + qName);

					// graphsanddata
					if (qName.equalsIgnoreCase("graphsanddata")) {
						if (linkUrl == null) {
							bgraphsanddata = true;
						}
						
					}

					// change30day
					if (qName.equalsIgnoreCase("valueChange")) {
						if (change30day == null) {
							bchange30day = true;
						}
					}

					// lastSoldPrice
					if (qName.equalsIgnoreCase("lastSoldPrice")) {
						if (lastSoldPrice == null) {
							blastSoldPrice = true;
						}
					}

					// lastSoldDate
					if (qName.equalsIgnoreCase("lastSoldDate")) {
						if (lastSoldDate == null) {
							blastSoldDate = true;
						}
					}

					// high
					if (qName.equalsIgnoreCase("high")) {
						if (high == null) {
							bhigh = true;
						}
					}

					// low
					if (qName.equalsIgnoreCase("low")) {
						if (low == null) {
							blow = true;
						}
					}

					// bedrooms
					if (qName.equalsIgnoreCase("bedrooms")) {
						if (bedrooms == null) {
							bbedrooms = true;
						}
					}

					// zpid
					if (qName.equalsIgnoreCase("zpid")) {
						if (zpid == null) {
							bzpid = true;
						}
					}

					// url
					if (qName.equalsIgnoreCase("url")) {
						if (url == null) {
							burl = true;
						}
					}

					// SqFt
					if (qName.equalsIgnoreCase("finishedSqFt")) {
						if (SqFt == null) {
							bfinishedSqFt = true;
						}
					}

					// family
					if (qName.equalsIgnoreCase("useCode")) {
						if (useCode == null) {
							buseCode = true;
						}
					}
					// bathrooms
					if (qName.equalsIgnoreCase("bathrooms")) {
						if (bathrooms == null) {
							bbathrooms = true;
						}
					}

					// lotSizeSqFt
					if (qName.equalsIgnoreCase("lotSizeSqFt")) {
						if (lotSizeSqFt == null) {
							blotSizeSqFt = true;
						}
					}

					// yearBuilt
					if (qName.equalsIgnoreCase("yearBuilt")) {
						if (yearBuilt == null) {
							byearBuilt = true;
						}
					}

					// value
					if (qName.equalsIgnoreCase("amount")) {
						if (amount == null) {
							bamount = true;
						}
					}

					// last-updated
					if (qName.equalsIgnoreCase("last-updated")) {
						if (lastUpdated == null) {
							blastUpdated = true;
						}
					}

				}

				public void endElement(String uri, String localName,
						String qName) throws SAXException {

					Log.logger.logp(Level.SEVERE, CallZillowWS.class.getName(),
							methodName, "End Element :" + qName);

				}

				public void characters(char ch[], int start, int length)
						throws SAXException {

					if (bgraphsanddata) {
						String s = new String(ch, start, length);
						linkUrl = s;
						bgraphsanddata = false;
					}

					if (bchange30day) {
						String s = new String(ch, start, length);
						change30day = s;
						bchange30day = false;
					}

					if (blastSoldPrice) {
						String s = new String(ch, start, length);
						lastSoldPrice = s;
						blastSoldPrice = false;
					}

					if (blastSoldDate) {
						String s = new String(ch, start, length);
						lastSoldDate = s;
						blastSoldDate = false;
					}

					if (bhigh) {
						String s = new String(ch, start, length);
						high = s;
						bhigh = false;
					}

					if (blow) {
						String s = new String(ch, start, length);
						low = s;
						blow = false;
					}

					if (bbedrooms) {
						String s = new String(ch, start, length);
						bedrooms = s;
						bbedrooms = false;
					}

					if (bzpid) {
						String s = new String(ch, start, length);
						zpid = s;
						bzpid = false;
					}

					if (burl) {
						String s = new String(ch, start, length);
						url = s;
						burl = false;
					}

					if (bfinishedSqFt) {

						String s = new String(ch, start, length);
						SqFt = s;
						bfinishedSqFt = false;
					}

					if (buseCode) {
						String s = new String(ch, start, length);
						useCode = s;
						buseCode = false;
					}

					if (bbathrooms) {
						String s = new String(ch, start, length);
						bathrooms = s;
						bbathrooms = false;
					}

					if (blotSizeSqFt) {
						String s = new String(ch, start, length);
						lotSizeSqFt = s;
						blotSizeSqFt = false;
					}

					if (byearBuilt) {
						String s = new String(ch, start, length);
						yearBuilt = s;
						byearBuilt = false;
					}

					if (bamount) {
						String s = new String(ch, start, length);
						amount = s;
						bamount = false;
					}

					if (blastUpdated) {
						String s = new String(ch, start, length);
						lastUpdated = s;
						blastUpdated = false;
					}

				}
			};

			saxParser.parse(stream, handler);

		} catch (ParserConfigurationException e) {
			Log.logger.logp(Level.SEVERE, CallZillowWS.class.getName(),
					methodName, e.getMessage());
			e.printStackTrace();
		} catch (SAXException e) {
			Log.logger.logp(Level.SEVERE, CallZillowWS.class.getName(),
					methodName, e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			Log.logger.logp(Level.SEVERE, CallZillowWS.class.getName(),
					methodName, e.getMessage());
			e.printStackTrace();
		}

	}
}
