/*******************************************************************************
 * Copyright (c) 2010-2011 VIVO Harvester Team. For full list of contributors, please see the AUTHORS file provided.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the new BSD license which accompanies this distribution, and is available at http://www.opensource.org/licenses/bsd-license.html
 ******************************************************************************/
package org.vivoweb.harvester.fetch;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import org.vivoweb.harvester.util.FileAide;
import org.vivoweb.harvester.util.InitLog;
import org.vivoweb.harvester.util.args.ArgDef;
import org.vivoweb.harvester.util.args.ArgList;
import org.vivoweb.harvester.util.args.ArgParser;
import org.vivoweb.harvester.util.args.UsageException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Fetches SOAP-XML data from a SOAP compatible site placing the data in the supplied file.
 */
public class SOAPFetch {
	/**
	 * SLF4J Logger
	 */
	private static Logger log = LoggerFactory.getLogger(SOAPFetch.class);
	/**
	 * File to put XML in.
	 */
	private OutputStream outputFile;
	
	/**
	 * URL to send message to
	 */
	private URL url;

	/**
	 * Connection derived from that URL
	 */
	private URLConnection urlCon;
	
	/**
	 * Inputstream with SOAP style XML message
	 */
	private InputStream inputFile;
	
	/**
	 * String of XML gathered from the inputStream
	 */
	private String xmlString;
	
	/**
	 * Constructor
	 * @param url address to connect with
	 * @param output RecordHandler to write data to
	 * @param xmlFile xml file to POST to the url
	 * @throws IOException error talking with database
	 */
	public SOAPFetch(URL url, String output, String xmlFile) throws IOException {
		this(url, FileAide.getOutputStream( output ), FileAide.getInputStream( xmlFile ) );
	}
	
	
	/**
	 * Command line Constructor
	 * @param args commandline arguments
	 * @throws IOException error creating task
	 * @throws UsageException user requested usage message
	 */
	private SOAPFetch(String[] args) throws IOException, UsageException {
		this(getParser().parse(args));
	}
	
	/**
	 * Arglist Constructor
	 * @param args option set of parsed args
	 * @throws IOException error creating task
	 */
	private SOAPFetch(ArgList args) throws IOException {
		this(
			new URL(args.get("u")), 
			FileAide.getOutputStream(args.get("o")),
			FileAide.getInputStream(args.get("m"))
		);
	}
	
	/**
	 * Library style Constructor
	 * @param url URL to connect with.
	 * @param output The stream to the output file
	 * @param xmlFileStream The stream which points to the Soap Message
	 * @throws IOException problem with opening url connection
	 */
	public SOAPFetch(URL url, OutputStream output, InputStream xmlFileStream) throws IOException {
		
		this.outputFile = output;
		this.url = url;
		this.inputFile = xmlFileStream;
		log.debug("Outputfile = " + this.outputFile.toString());
		log.debug("URL = "+ this.url.toString());
		log.debug("Inputfile = " + this.inputFile.toString());
		log.debug("Checking for NULL values");
		if(this.outputFile == null) {
			throw new IllegalArgumentException("Must provide output file!");
		}
		
		if(this.inputFile == null) {
			throw new IllegalArgumentException("Must provide message file!");
		}
		
		if(this.url == null) {
			throw new IllegalArgumentException("Must provide url!");
		}
		log.info("opening the url connection");
		this.urlCon = this.url.openConnection();

	    // specify that we will send output and accept input
		this.urlCon.setDoInput(true);
		this.urlCon.setDoOutput(true);

//		this.urlCon.setConnectTimeout( 20000 );  // long timeout, but not infinite
//		this.urlCon.setReadTimeout( 20000 );

		this.urlCon.setUseCaches (false);
		this.urlCon.setDefaultUseCaches (false);

		this.xmlString = IOUtils.toString(this.inputFile,"UTF-8");

		log.info("Built message");
		log.debug("Message contents:\n" + this.xmlString);
		
	}
	
	
	/**
	 * Executes the task
	 * @throws IOException error processing record handler or jdbc connection
	 */
	public void execute() throws IOException {
	    // tell the web server what we are sending
		this.urlCon.setRequestProperty ( "Content-Type", "application/soap+xml; charset=utf-8" );


		log.debug("getting writer for url connection");
	    OutputStreamWriter osWriter = new OutputStreamWriter( this.urlCon.getOutputStream() );
		log.debug("writting to url connection");
	    osWriter.write(this.xmlString);
	    osWriter.close();

	    // reading the response
		log.debug("getting reader for url connection");
	    InputStreamReader isReader = new InputStreamReader( this.urlCon.getInputStream() );

	    StringBuilder buf = new StringBuilder();
	    char[] cbuf = new char[ 2048 ];
	    int num;

	    while ( -1 != (num=isReader.read( cbuf )))
	    {
	        buf.append( cbuf, 0, num );
	    }

	    String result = buf.toString();

		log.debug("Response contents:\n" + result);
	    OutputStreamWriter outputWriter = new OutputStreamWriter(this.outputFile);
		log.info("writing response");
	    outputWriter.write(result);
	    outputWriter.close();

	}
	
	private String SOAPRequest(URL url, OutputStream output, InputStream xmlFileStream){
		String result = new String();
	    try {
	        SOAPConnectionFactory sfc = SOAPConnectionFactory.newInstance();
	        SOAPConnection connection = sfc.createConnection();

	        MessageFactory mf = MessageFactory.newInstance();
	        SOAPMessage sm = mf.createMessage();

	        SOAPHeader sh = sm.getSOAPHeader();
	        SOAPBody sb = sm.getSOAPBody();
	        sh.detachNode();
	        QName bodyName = new QName("http://quoteCompany.com", "GetQuote", "d");
	        SOAPBodyElement bodyElement = sb.addBodyElement(bodyName);
	        QName qn = new QName("aName");
	        SOAPElement messageBody = bodyElement.addChildElement(qn);

	        messageBody.addTextNode("TextMode");

	        System.out.println("\n Soap Request:\n");
	        sm.writeTo(System.out);
	        System.out.println();

	        SOAPMessage response = connection.call(sm, url);
	        System.out.println(response.getContentDescription());
	      } catch (Exception ex) {
	        ex.printStackTrace();
	      }
		return result;
	}
	
	/**
	 * Get the ArgParser for this task
	 * @return the ArgParser
	 */
	private static ArgParser getParser() {
		ArgParser parser = new ArgParser("SOAPFetch");
		parser.addArgument(new ArgDef().setShortOption('u').setLongOpt("url").withParameter(true, "URL").setDescription("The URL which will receive the MESSAGE.").setRequired(true));
		parser.addArgument(new ArgDef().setShortOption('m').setLongOpt("message").withParameter(true, "MESSAGE").setDescription("The MESSAGE file path.").setRequired(true));
		parser.addArgument(new ArgDef().setShortOption('o').setLongOpt("output").withParameter(true, "OUTPUT_FILE").setDescription("XML result file path").setRequired(false));
		//parser.addArgument(new ArgDef().setShortOption('n').setLongOpt("namespaceBase").withParameter(true, "NAMESPACE_BASE").setDescription("the base namespace to use for each node created").setRequired(false));
		return parser;
	}
	
	/**
	 * Main method
	 * @param args commandline arguments
	 */
	public static void main(String... args) {
		Exception error = null;
		try {
			InitLog.initLogger(args, getParser());
			log.info(getParser().getAppName() + ": Start");
			new SOAPFetch(args).execute();
		} catch(IllegalArgumentException e) {
			log.error(e.getMessage());
			log.debug("Stacktrace:",e);
			System.out.println(getParser().getUsage());
			error = e;
		} catch(UsageException e) {
			log.info("Printing Usage:");
			System.out.println(getParser().getUsage());
			error = e;
		} catch(Exception e) {
			log.error(e.getMessage());
			log.debug("Stacktrace:",e);
			error = e;
		} finally {
			log.info(getParser().getAppName() + ": End");
			if(error != null) {
				System.exit(1);
			}
		}
	}
}
