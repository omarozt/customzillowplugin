package com.microstrategy.custom.sdk.web.callreporttask;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.microstrategy.custom.sdk.web.addons.PropertiesSupport;
import com.microstrategy.custom.sdk.web.log.Log;
import com.microstrategy.utils.log.Level;

public class ExecuteReportTask {

	//get report ID (data set) from configuration file
	PropertiesSupport properties = PropertiesSupport.getInstance();
	String reportId = properties.getProperty("rportID");
	
	ArrayList<String> addressList = new ArrayList<String>();

	public String[] getAddress(String cardId, String sessionState, String webServerName) {
		
		String path = webServerName;
		String methodName = "getAddress";
		String[] address = new String[4];
		StringBuilder sb = new StringBuilder();
		String params = sb.append("taskId=reportExecute&")
				.append("taskEnv=xml&").append("taskContentType=xml&")
				.append("sessionState=").append(sessionState)
				.append("&styleName=ReportGridStyle&").append("reportID=")
				.append(reportId).append("&maxWait=-1&")
				.append("valuePromptAnswers=").append(cardId).toString();

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
			System.out.println("resonse:" + responseCode);

			Log.logger.logp(Level.SEVERE, ExecuteReportTask.class.getName(),
					methodName, "HTTP resonse code: " + responseCode);

			if (responseCode == 200) {
				BufferedReader bufferReader = new BufferedReader(
						new InputStreamReader(urlConn.getInputStream(), "UTF-8"));

				StringBuilder strb = new StringBuilder();
				String line;

				while ((line = bufferReader.readLine()) != null) {
					strb.append(line + "\n");
				}

				String stream = strb.toString();
				System.out.println("response: " + stream);

				Log.logger.logp(Level.SEVERE,
						ExecuteReportTask.class.getName(), methodName,
						"HTTP resonse: " + stream);

				stream = stream.replace("&", "&amp;");
				int index = stream.indexOf("</style>");
				stream = "<taskResponse><div>" + stream.substring(index + 8);// to
																				// start
																				// from
																				// end
																				// of
																				// tag
																				// </style>
				System.out.println("response2: " + stream);

				Log.logger.logp(Level.SEVERE,
						ExecuteReportTask.class.getName(), methodName,
						"HTTP resonse substring: " + stream);

				// remove all &nbsp; from report data
				stream = stream.replaceAll("&nbsp;", "");

				Log.logger.logp(Level.SEVERE,
						ExecuteReportTask.class.getName(), methodName,
						"HTTP resonse substring without &nbsp; signs: "
								+ stream);
				System.out.println("response3 without &nbsp; : " + stream);

				InputStream inputStream;
				inputStream = new ByteArrayInputStream(stream.getBytes());
				parseXML(inputStream);

				String allValues = "";
				for (int i = 0; i < addressList.size(); i++) {
					System.out.println("value" + i + ": " + addressList.get(i));
					allValues.concat("value" + i + ": " + addressList.get(i));
				}

				Log.logger.logp(Level.SEVERE,
						ExecuteReportTask.class.getName(), methodName,
						"Address values (array list) : " + allValues);

				address[0] = addressList.get(1);// street
				address[1] = addressList.get(2) + " " + addressList.get(3) + " "
						+ addressList.get(4);// city + state + zip

			}
		} catch (IOException e) {
			Log.logger.logp(Level.SEVERE, ExecuteReportTask.class.getName(),
					methodName, e.getMessage());
		}
		return address;
	}

	private void parseXML(InputStream stream) {
		final String methodName = "parseXML";

		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			SAXParser saxParser = factory.newSAXParser();

			DefaultHandler handler = new DefaultHandler() {

				boolean bstreet = false;

				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException {
					/*
					Log.logger.logp(Level.SEVERE,
							ExecuteReportTask.class.getName(), methodName,
							"Start Element :" + qName);
					*/
					// get row
					if (qName.equalsIgnoreCase("TD")) {
						// result is in should be in row - which class is r-c3
						String value = attributes.getValue("class");
						if (value != null && value.equals("r-c3")) {
							bstreet = true;
						}
					}
				}

				public void endElement(String uri, String localName,
						String qName) throws SAXException {
				}

				public void characters(char ch[], int start, int length)
						throws SAXException {

					if (bstreet) {
						String s = new String(ch, start, length);
						addressList.add(s);
						bstreet = false;
					}
				}
			};
			saxParser.parse(stream, handler);

		} catch (ParserConfigurationException e) {
			Log.logger.logp(Level.SEVERE, ExecuteReportTask.class.getName(),
					methodName, e.getMessage());
			e.printStackTrace();
		} catch (SAXException e) {
			Log.logger.logp(Level.SEVERE, ExecuteReportTask.class.getName(),
					methodName, e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			Log.logger.logp(Level.SEVERE, ExecuteReportTask.class.getName(),
					methodName, e.getMessage());
			e.printStackTrace();
		}
	}
}
