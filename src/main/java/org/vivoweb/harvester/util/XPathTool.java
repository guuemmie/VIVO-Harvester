/******************************************************************************************************************************
 * Copyright (c) 2011 Christopher Haines, Dale Scheppler, Nicholas Skaggs, Stephen V. Williams, James Pence, Michael Barbieri.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the new BSD license which accompanies this
 * distribution, and is available at http://www.opensource.org/licenses/bsd-license.html
 * Contributors:
 * Christopher Haines, Dale Scheppler, Nicholas Skaggs, Stephen V. Williams, James Pence, Michael Barbieri
 * - initial API and implementation
 *****************************************************************************************************************************/
package org.vivoweb.harvester.util;

import java.io.IOException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vivoweb.harvester.util.args.ArgDef;
import org.vivoweb.harvester.util.args.ArgList;
import org.vivoweb.harvester.util.args.ArgParser;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Export xpath selection from xml file
 * @author Christopher Haines (hainesc@ctrip.ufl.edu)
 */
public class XPathTool {
	/**
	 * SLF4J Logger
	 */
	protected static Logger log = LoggerFactory.getLogger(XPathTool.class);
	/**
	 * path to xml file to read data from
	 */
	private String xml;
	/**
	 * xpath expression to export
	 */
	private String exp;
	
	/**
	 * Constructor
	 * @param xmlFile path to xml file to read data from
	 * @param expression xpath expression to export
	 */
	public XPathTool(String xmlFile, String expression) {
		this.xml = xmlFile;
		this.exp = expression;
	}
	
	/**
	 * Constructor
	 * @param args commandline arguments
	 * @throws IOException error creating task
	 */
	public XPathTool(String[] args) throws IOException {
		this(new ArgList(getParser(), args));
	}
	
	/**
	 * Constructor
	 * @param argList arguments
	 */
	public XPathTool(ArgList argList) {
		this(argList.get("x"), argList.get("e"));
	}
	
	/**
	 * Export xpath selection from xml file
	 * @param xmlFile path to xml file to read data from
	 * @param expression xpath expression to export
	 * @return the value of the selection
	 * @throws IOException error reading xml file
	 */
	public static String getXPathResult(String xmlFile, String expression) throws IOException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true); // never forget this!
			Document doc = factory.newDocumentBuilder().parse(FileAide.getInputStream(xmlFile));
			String value = XPathFactory.newInstance().newXPath().compile(expression).evaluate(doc, XPathConstants.STRING).toString();
//			log.debug("xpath result for '" + expression + "' on file '" + xmlFile + "': '" + value + "'");
			return value;
		} catch(ParserConfigurationException e) {
			throw new IOException(e.getMessage(), e);
		} catch(SAXException e) {
			throw new IOException(e.getMessage(), e);
		} catch(XPathExpressionException e) {
			throw new IOException(e.getMessage(), e);
		}
	}
	
	/**
	 * Runs the xpath
	 * @throws IOException error executing
	 */
	public void execute() throws IOException {
		System.out.println(getXPathResult(this.xml, this.exp));
	}
	
	/**
	 * Get the ArgParser for this task
	 * @return the ArgParser
	 */
	private static ArgParser getParser() {
		ArgParser parser = new ArgParser("XPathTool");
		// Inputs
		parser.addArgument(new ArgDef().setShortOption('x').setLongOpt("xml-file").withParameter(true, "XML_FILE").setDescription("path to xml file to read data from").setRequired(true));
		// Params
		parser.addArgument(new ArgDef().setShortOption('e').setLongOpt("expression").withParameter(true, "XPATH_EXPRESSION").setDescription("xpath expression to export").setRequired(true));
		return parser;
	}
	
	/**
	 * Main method
	 * @param args commandline arguments
	 */
	public static void main(String... args) {
		Exception error = null;
		try {
			String harvLev = System.getProperty("console-log-level");
			System.setProperty("console-log-level", "OFF");
			InitLog.initLogger(args, getParser());
			if(harvLev == null) {
				System.clearProperty("console-log-level");
			} else {
				System.setProperty("console-log-level", harvLev);
			}
			log.info(getParser().getAppName() + ": Start");
			new XPathTool(args).execute();
		} catch(IllegalArgumentException e) {
			log.error(e.getMessage());
			System.out.println(getParser().getUsage());
			error = e;
		} catch(Exception e) {
			log.error(e.getMessage(), e);
			error = e;
		} finally {
			log.info(getParser().getAppName() + ": End");
			if(error != null) {
				System.exit(1);
			}
		}
	}
}
