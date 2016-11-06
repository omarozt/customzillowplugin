package com.microstrategy.custom.sdk.web.addons;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.microstrategy.custom.sdk.web.log.Log;
import com.microstrategy.custom.sdk.web.zillow.CallZillowWS;
import com.microstrategy.utils.log.Level;
import com.microstrategy.utils.serialization.EnumWebPersistableState;
import com.microstrategy.web.app.addons.AbstractAppAddOn;
import com.microstrategy.web.app.beans.PageComponent;
import com.microstrategy.web.beans.RWBean;
import com.microstrategy.web.beans.WebBeanException;
import com.microstrategy.web.objects.WebHyperLink;
import com.microstrategy.web.objects.WebHyperLinks;
import com.microstrategy.web.objects.WebObjectsException;
import com.microstrategy.web.objects.WebPrompts;
import com.microstrategy.web.objects.rw.EnumRWUnitTypes;
import com.microstrategy.web.objects.rw.RWDefinition;
import com.microstrategy.web.objects.rw.RWImageDef;
import com.microstrategy.web.objects.rw.RWInstance;
import com.microstrategy.web.objects.rw.RWTextDef;
import com.microstrategy.web.objects.rw.RWUnitDef;
import com.microstrategy.webapi.EnumDSSXMLStatus;

public class CustomZillowAddon extends AbstractAppAddOn {

	String memberCardID = "";

	@Override
	public String getAddOnDescription() {
		return "Custom Zillow Add-on";
	}

	public void preCollectData(PageComponent page) {
		
		//page.getBeanContext().
		String methodName = "preCollectData";
		// get properties, if document ID on the list - do customization
		boolean applyCustomization = false;
		PropertiesSupport properties = PropertiesSupport.getInstance();
		RWBean rwb = (RWBean) page.getChildByClass(RWBean.class);

		try {
			String reportID = rwb.getObjectID();
			// check if document ID is on the list
			String[] docIds = properties.getProperty("documentID").split(",");
			for (int i = 0; i < docIds.length; i++) {

				if (docIds[i].equals(reportID)) {
					applyCustomization = true;
					Log.logger
							.logp(Level.SEVERE,
									CustomZillowAddon.class.getName(),
									methodName,
									"document is customized, document ID = "
											+ reportID);
				}
			}
			if (!applyCustomization) {
				Log.logger
						.logp(Level.SEVERE, CustomZillowAddon.class.getName(),
								methodName,
								"document is not customized, document ID = "
										+ reportID);
			}

		} catch (WebBeanException e1) {
			Log.logger.logp(Level.SEVERE, CustomZillowAddon.class.getName(),
					methodName, e1.getMessage());
		}
		
		if (applyCustomization) {
			// read card id from prompt
			RWInstance inst = null;
			try {
				inst = rwb.getRWInstance();
				inst.setMaxWait(-1);
				inst.setAsync(false);
				
				// added by Jerry on 03/17/14 to make sure the add-on does not execute when on the prompts page
				int status = inst.pollStatus();
				Log.logger.logp(Level.SEVERE, CustomZillowAddon.class.getName(), methodName, "status", status);
				if (status != EnumDSSXMLStatus.DssXmlStatusResult & status != EnumDSSXMLStatus.DssXmlStatusXMLResult) {
					Log.logger.logp(Level.SEVERE, CustomZillowAddon.class.getName(), methodName, "Document instance is not ready, exiting");
					return;
				}
				
				rwb.collectData();
				WebPrompts prompts = inst.getPrompts();
				prompts.setClosed(false);
				String promptAnswer = prompts.getShortAnswerXML(true);

				InputStream inputStream;
				inputStream = new ByteArrayInputStream(promptAnswer.getBytes());

				// get prompt answer:
				parsePromptAnswer(inputStream);

				System.out.println("PROMPT answer send to dashboard: "
						+ promptAnswer);

				Log.logger.logp(Level.SEVERE,
						CustomZillowAddon.class.getName(), methodName,
						"PROMPT answer send to dashboard: " + promptAnswer);

				prompts.setClosed(true);

			} catch (WebBeanException e1) {
				Log.logger.logp(Level.SEVERE,
						CustomZillowAddon.class.getName(), methodName,
						e1.getMessage());
				e1.printStackTrace();
			} catch (WebObjectsException e) {
				Log.logger.logp(Level.SEVERE,
						CustomZillowAddon.class.getName(), methodName,
						e.getMessage());
				e.printStackTrace();
			}

			String sessionState = page.getWebIServerSession().saveState(
					EnumWebPersistableState.MAXIMAL_STATE_INFO);
			Log.logger.logp(Level.SEVERE, CustomZillowAddon.class.getName(),
					methodName, "session: " + sessionState);

			// get web server path
			String webServerName = page.getAppContext().getServletPath();
			webServerName = webServerName + "taskProc?";

			Log.logger.logp(Level.SEVERE, CustomZillowAddon.class.getName(),
					methodName, "Task proc server path: " + webServerName);

			// call zillow
			CallZillowWS callZillow = new CallZillowWS();
			HashMap<String, String> map = new HashMap<String, String>();

			if (!memberCardID.isEmpty()) {
				map = callZillow.getInfoFromZillow(memberCardID, sessionState,
						webServerName);
			} else {
				Log.logger.logp(Level.SEVERE,
						CustomZillowAddon.class.getName(), methodName,
						"ERROR: prompt anwser sent to dashboard was empty.");
			}

			if (map != null && map.size() > 0) {
				try {	
					// add by Jerry on 03/14/14
					int status = inst.pollStatus();
					if (status != EnumDSSXMLStatus.DssXmlStatusResult & status != EnumDSSXMLStatus.DssXmlStatusXMLResult) {
						Log.logger.logp(Level.SEVERE, CustomZillowAddon.class.getName(), methodName, "Document instance is not ready, exiting with status "+status);
						return;
					}
					
					RWDefinition def = inst.getDefinition();

					// text fields
					String textName1 = properties.getProperty("textFieldName1");
					String textName2 = properties.getProperty("textFieldName2");
					String textName1a = properties
							.getProperty("textFieldName1a");
					String textName2a = properties
							.getProperty("textFieldName2a");
					String textNameZillowLink = properties
							.getProperty("textNameZillowLink");

					int textType = EnumRWUnitTypes.RWUNIT_TEXT;
					// first column
					String textKey1 = getKey(textName1, def, textType);
					String textKey2 = getKey(textName2, def, textType);
					String textKey1a = getKey(textName1a, def, textType);
					String textKey2a = getKey(textName2a, def, textType);
					String textZillowLinkKey = getKey(textNameZillowLink, def,
							textType);

					if (textKey1 != null) {
						RWTextDef textField1 = (RWTextDef) def
								.findUnit(textKey1);
						StringBuilder textToDisplayFirstColumn = new StringBuilder();

						textToDisplayFirstColumn.append("Bedrooms: \n")
								.append("\n").append("Bathrooms: \n")
								.append("\n").append("SqFt: \n").append("\n")
								.append("Lot size: \n").append("\n")
								.append("Property type: \n").append("\n")
								.append("Year built: ");

						textField1.setText(textToDisplayFirstColumn.toString());
						textField1.copyFormat(textField1);

					}

					if (textKey1a != null) {
						RWTextDef textField1a = (RWTextDef) def
								.findUnit(textKey1a);
						StringBuilder textToDisplayFirstColumnA = new StringBuilder();

						//check Bedrooms
						if (map.get("Bedrooms") != null) {
							textToDisplayFirstColumnA
							.append(map.get("Bedrooms"))
							.append("\n").append("\n");
						} else {
							textToDisplayFirstColumnA
							.append("--")
							.append("\n").append("\n");
						}
						
						//check bathrooms
						if (map.get("Bathrooms") != null) {
							textToDisplayFirstColumnA
							.append(map.get("Bathrooms"))
							.append("\n").append("\n");
						} else {
							textToDisplayFirstColumnA
							.append("--")
							.append("\n").append("\n");
						}
						
						//check SqFt
						if (map.get("SqFt") != null) {
							textToDisplayFirstColumnA
							.append(map.get("SqFt"))
							.append("\n").append("\n");
						} else {
							textToDisplayFirstColumnA
							.append("--")
							.append("\n").append("\n");
						}
						
						//check Lot size
						if (map.get("Lot size") != null) {
							textToDisplayFirstColumnA
							.append(map.get("Lot size"))
							.append("\n").append("\n");
						} else {
							textToDisplayFirstColumnA
							.append("--")
							.append("\n").append("\n");
						}
						
						//check Property type
						if (map.get("Property type") != null) {
							textToDisplayFirstColumnA
							.append(map.get("Property type"))
							.append("\n").append("\n");
						} else {
							textToDisplayFirstColumnA
							.append("--")
							.append("\n").append("\n");
						}
						
						//check year built
						if (map.get("Year built") != null) {
							textToDisplayFirstColumnA
							.append(map.get("Year built"))
							.append("\n");
						} else {
							textToDisplayFirstColumnA
							.append("--")
							.append("\n");
						}
						
//						textToDisplayFirstColumnA.append(map.get("Bedrooms"))
//								.append("\n").append("\n")
//								.append(map.get("Bathrooms")).append("\n")
//								.append("\n").append(map.get("SqFt"))
//								.append("\n").append("\n")
//								.append(map.get("Lot size")).append("\n")
//								.append("\n").append(map.get("Property type"))
//								.append("\n").append("\n")
//								.append(map.get("Year built")).append("\n")
//								.toString();

						textField1a.setText(textToDisplayFirstColumnA
								.toString());
						textField1a.copyFormat(textField1a);
					}

					if (textKey2 != null) {
						RWTextDef textField2 = (RWTextDef) def
								.findUnit(textKey2);
						StringBuilder textToDisplaySecondColumn = new StringBuilder();						
						
						textToDisplaySecondColumn.append("Value: \n")
								.append("\n").append("Range: \n").append("\n")
								.append("30-day change: \n").append("\n")
								.append("Last updated: \n").append("\n")
								.append("Last sold date: \n").append("\n")
								.append("Last Sold Price: ");

						textField2
								.setText(textToDisplaySecondColumn.toString());
						textField2.copyFormat(textField2);
					}

					if (textKey2a != null) {
						RWTextDef textField2a = (RWTextDef) def
								.findUnit(textKey2a);

						StringBuilder textToDisplaySecondColumnA = new StringBuilder();

						//check all values, if value is null display '--'
						//check value
						if (map.get("Value") != null) {
							textToDisplaySecondColumnA
							.append("$")
							.append(addComa(map.get("Value"))).append("\n")
							.append("\n");
						} else {
							textToDisplaySecondColumnA
							.append("--").append("\n")
							.append("\n");
						}
						//check range 
						if (map.get("RangeLow") != null && map.get("RangeHigh") != null){
							textToDisplaySecondColumnA
							.append("$")
							.append(addComa(map.get("RangeLow")))
							.append(" - $")
							.append(addComa(map.get("RangeHigh")))
							.append("\n").append("\n");
						} else {
							textToDisplaySecondColumnA
							.append("--")
							.append("\n").append("\n");;
						}
						
						//check 30dayChange 
						if (map.get("30dayChange") != null){
							textToDisplaySecondColumnA
							.append("$")
							.append(addComa(map.get("30dayChange")))
							.append("\n").append("\n");
							
						} else {
							textToDisplaySecondColumnA
							.append("--")
							.append("\n").append("\n");
						}
						
						//check Last updated date 
						if (map.get("Last updated") != null){ 
							textToDisplaySecondColumnA
							.append(map.get("Last updated"))
							.append("\n");
						} else {
							textToDisplaySecondColumnA
							.append("--")
							.append("\n");
						}
						
						//check Last sold date 
						if (map.get("LastSoldDate") != null){ 
							textToDisplaySecondColumnA
							.append("\n")
							.append(map.get("LastSoldDate"))
							.append("\n").append("\n");
						} else {
							textToDisplaySecondColumnA
							.append("\n")
							.append("--")
							.append("\n").append("\n");
						}
						
						//check LastSoldPrice 
						if (map.get("LastSoldPrice") != null){
							textToDisplaySecondColumnA
							.append("$")
							.append(addComa(map.get("LastSoldPrice")))
							.append("\n");
						} else {
							textToDisplaySecondColumnA
							.append("--")
							.append("\n");
						}

						textField2a.setText(textToDisplaySecondColumnA
								.toString());
						textField2a.copyFormat(textField2a);
					}

					if (textZillowLinkKey != null) {
						RWTextDef textZillowLinkField = (RWTextDef) def
								.findUnit(textZillowLinkKey);

						// set new hyperlink to 'Zestimate (% change)' link
						WebHyperLinks hyperLinks = textZillowLinkField
								.getHyperLinks();
						if (hyperLinks.size() > 0 && hyperLinks != null) {
							WebHyperLink hyperLink = hyperLinks.get(0);

							String linkk = "http://";
							if (map.get("LinkUrl") != null
									&& !map.get("LinkUrl").isEmpty()) {
								hyperLink.setURL(map.get("LinkUrl"));
							} else {
								hyperLink.setURL(linkk);
							}
							String linkText = properties
									.getProperty("zillowLinkText");
							textZillowLinkField.setText(linkText);
							hyperLink.setDisplayText(linkText);

							textZillowLinkField.copyFormat(textZillowLinkField);

							Log.logger.logp(Level.SEVERE,
									CustomZillowAddon.class.getName(),
									methodName,
									"Zillow link: " + map.get("LinkUrl"));
						} else {
							Log.logger.logp(Level.SEVERE,
									CustomZillowAddon.class.getName(),
									methodName,
									"ERROR! Hyperlinks table is empty. ");
						}
					}
					
					

					// image field
					String imageName = properties.getProperty("imageFieldName");
					int imageType = EnumRWUnitTypes.RWUNIT_IMAGE;
					String imageKey = getKey(imageName, def, imageType);
					if (imageKey != null) {
						RWImageDef imgField = (RWImageDef) def
								.findUnit(imageKey);
						String path = map.get("url");
						if (path != null)
							path = path.replace("00amp;", "&");
						imgField.setPath(path);

						// adding hyperlink to the image
						WebHyperLinks hyperLinks = imgField.getHyperLinks();

						String linkk = "http:// ";
						if (hyperLinks.size() > 0 && hyperLinks != null) {
							WebHyperLink hyperLink = hyperLinks.get(0);
							// the same URL like on link
							if (map.get("LinkUrl") != null
									&& !map.get("LinkUrl").isEmpty()) {
								hyperLink.setURL(map.get("LinkUrl"));

							} else {
								hyperLink.setURL(linkk);
								Log.logger.logp(Level.SEVERE,
										CustomZillowAddon.class.getName(),
										methodName,
										"Zillow image link to page is empty.");
							}

							// keep the oryginal format:
							imgField.copyFormat(imgField);

							Log.logger.logp(Level.SEVERE,
									CustomZillowAddon.class.getName(),
									methodName, "Zillow image path = " + path);
						}
					}

				} catch (WebObjectsException e) {
					Log.logger.logp(Level.SEVERE,
							CustomZillowAddon.class.getName(), methodName,
							e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Method get string (big number) and adding commas to separate thousands
	 * 
	 * @param value
	 *            in dollars
	 * @return string with commas
	 */
	private Object addComa(String value) {
		String methodName = "addComa";
		String result = value;
		
		if (value != null) {
		long value1 = Long.parseLong(value);
		result = NumberFormat.getNumberInstance(Locale.US).format(value1);
		System.out.println(result);
		Log.logger.logp(Level.SEVERE,
				CustomZillowAddon.class.getName(), methodName,
				"Value after formating= " + result);
		}
		return result;
	}

	
	private void parsePromptAnswer(InputStream stream) {
		String methodName = "parsePromptAnswer";

		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			SAXParser saxParser = factory.newSAXParser();

			DefaultHandler handler = new DefaultHandler() {

				boolean banswer = false;

				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException {

					// get tag with prompt answer
					if (qName.equalsIgnoreCase("pa")) {
						banswer = true;
					}
				}

				public void endElement(String uri, String localName,
						String qName) throws SAXException {
				}

				public void characters(char ch[], int start, int length)
						throws SAXException {

					if (banswer) {
						String s = new String(ch, start, length);
						memberCardID = s;
						banswer = false;
					}
				}
			};

			saxParser.parse(stream, handler);

		} catch (ParserConfigurationException e) {
			Log.logger.logp(Level.SEVERE, CustomZillowAddon.class.getName(),
					methodName, e.getMessage());
			e.printStackTrace();
		} catch (SAXException e) {
			Log.logger.logp(Level.SEVERE, CustomZillowAddon.class.getName(),
					methodName, e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			Log.logger.logp(Level.SEVERE, CustomZillowAddon.class.getName(),
					methodName, e.getMessage());
			e.printStackTrace();
		}

	}

	private String getKey(String name, RWDefinition def, int type) {
		String methodName = "getKey";

		RWUnitDef[] units = def.findUnitsByType(type);
		String key = null;

		if (units != null && units.length != 0) {
			for (int i = 0; i < units.length; i++) {
				System.out.println("units = " + units[i].getName());
				if (units[i].getName().equals(name)) {
					key = units[i].getKey();
					System.out.println("key = " + key);
					Log.logger.logp(Level.SEVERE,
							CustomZillowAddon.class.getName(), methodName,
							"key of text field or img= " + key);
				}
			}
		}
		return key;
	}
}
