package au.id.bjf.vmsgreader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Utility class to process a list of Nokia VMG files, representing text
 * messages or other data, into a single XML file for simpler processing.
 */
public class VmsgXmlFactory {

	/**
	 * Given an {@link Iterable} over a collection of {@link InputStream}, 
	 * convert it into XML for easier processing.
	 */
	public static Document buildDomFromMessage(final Iterable<InputStream> messages) {
		final Document document = newXmlDocument();
		final Stack<Node> tagStack = new Stack<Node>();
		tagStack.push(document);
		
		final Element rootElement = document.createElement("bjf:messages");
		rootElement.setAttribute("xmlns:bjf", "http://bjf.id.au/vmsgreader");
		document.appendChild(rootElement);
		tagStack.push(rootElement);
		
		for (final InputStream inputStream : messages) {
			final BufferedReader msgReader = new BufferedReader(
					new InputStreamReader(inputStream));
			processSingleMessage(msgReader, document, tagStack);
		}
		return null;
	}
	
	/**
	 * Given a VMG file (exported Nokia text message), convert it into XML for
	 * easier processing
	 * 
	 * @param vmsgStream VMG file
	 * @return DOM document version of VMG data
	 * @throws VmsgXmlFactoryException an error occurred
	 */
	public static Document buildDomFromMessage(final InputStream vmsgStream) {
		final BufferedReader msgReader = new BufferedReader(
				new InputStreamReader(vmsgStream));
		final Document document = newXmlDocument();
		final Stack<Node> tagStack = new Stack<Node>();
		tagStack.push(document);
		return processSingleMessage(msgReader, document, tagStack);
	}

	/**
	 * Process a single message.
	 * 
	 * @param vmsgReader a reader for a single message
	 * @param document DOM document from which to create {@link Node}s
	 * @param tagStack the tag stack.  Assumed to have at least the
	 *     {@link Document} node, and possibly nested {@link Element} nodes
	 *     pushed onto the stack upon entry.
	 * @return the built document
	 */
	private static Document processSingleMessage(
			final BufferedReader vmsgReader, final Document document,
			final Stack<Node> tagStack) {
		String rawBuffer;
		while ((rawBuffer = readFromStream(vmsgReader)) != null) {
			final String buffer = cleanUpNokiaString(rawBuffer);
			if (buffer.startsWith("BEGIN:")) {
				processStartTag(buffer, document, tagStack);
			} else if (buffer.startsWith("END:")) {
				processEndTag(buffer, document, tagStack);
			} else {
				// Are we within the root document element?
				if (tagStack.size() > 0
						&& tagStack.peek().getNodeType() == Node.ELEMENT_NODE) {

					// Special handling for VBODY: only "Date:" is treated like
					// an attribute
					if (tagStack.peek().equals("VBODY")) {
						if (buffer.startsWith("Date:")) {
							processAttribute(buffer, document, tagStack);
						} else {
							processTextNode(buffer, document, tagStack);
						}
					} else {
						if (buffer.indexOf(':') > -1) {
							processAttribute(buffer, document, tagStack);
						} else {
							processTextNode(buffer, document, tagStack);
						}
					}
				}
			}
		}
		return document;
	}

	/**
	 * Read from stream, and wrap {@link IOException}s with our own library
	 * exception type
	 * @param rdr reader to read from
	 * @return string data
	 * @throws VmsgXmlFactoryException an error occurred
	 */
	private static String readFromStream(final BufferedReader rdr) {
		try {
			return rdr.readLine();
		} catch (final IOException ioe) {
			throw new VmsgXmlFactoryException(VmsgErrors.IO, ioe.getLocalizedMessage());
		}
	}

	/**
	 * Create a new XML DOM document
	 * @return a new XML DOM document
	 */
	private static Document newXmlDocument() {
		try {
			final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
					.newInstance();
			final DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			return docBuilder.newDocument();
		} catch (final ParserConfigurationException pce) {
			throw new VmsgXmlFactoryException(VmsgErrors.XML_PARSER_CREATION,
					pce.getMessage());
		}
	}

	/**
	 * Remove all null characters from the arguments.  Note that we presently
	 * assume that all null characters can be discarded; this may be totally
	 * wrong, and may bring us badly unstuck with non-ANSI characters.
	 * 
	 * @param buffer input string, possible containing nulls
	 * @return buffer cleaned of null characters
	 */
	private static String cleanUpNokiaString(final String buffer) {
		buffer.getClass(); // null check
		final StringBuilder builder = new StringBuilder(buffer.length() / 2 + 1);
		for (int i = 0; i < buffer.length(); ++i) {
			if (buffer.charAt(i) != '\0') {
				builder.append(buffer.charAt(i));
			}
		}
		return builder.toString();
	}

	private static void processStartTag(final String buffer, final Document document,
			final Stack<Node> tagStack) {
		final String tagName = buffer.substring(6);
		if (tagName.length() == 0) {
			throw new VmsgXmlFactoryException(VmsgErrors.MISSING_TAG_NAME);
		}
		System.out.println("Pushing '" + buffer + "'");
		final Node newNode = document.createElement(tagName);
		tagStack.peek().appendChild(newNode);
		tagStack.push(newNode);
	}

	private static void processEndTag(final String buffer, final Document document,
			final Stack<Node> tagStack) {
		final String tagName = buffer.substring(4);
		if (tagName.length() == 0) {
			throw new VmsgXmlFactoryException(VmsgErrors.MISSING_TAG_NAME);
		}
		final Node peekTag = tagStack.peek();
		if (!tagName.equals(peekTag.getNodeName())) {
			throw new VmsgXmlFactoryException(VmsgErrors.MISMATCHED_TAG,
					"Expected: " + peekTag + ", actual: " + tagName);
		}
		tagStack.pop();
		System.out.println("Popping '" + buffer + "'");
	}

	private static void processTextNode(final String buffer, final Document document,
			final Stack<Node> tagStack) {
		if (tagStack.size() == 0
				|| tagStack.peek().getNodeType() != Node.ELEMENT_NODE) {
			throw new VmsgXmlFactoryException(VmsgErrors.NO_ENCLOSING_ELEMENT);
		}

		final Element innermostElement = (Element) tagStack.peek();
		innermostElement.appendChild(document.createTextNode(buffer));
	}

	private static void processAttribute(final String buffer, final Document document,
			final Stack<Node> tagStack) {
		if (tagStack.size() == 0
				|| tagStack.peek().getNodeType() != Node.ELEMENT_NODE) {
			throw new VmsgXmlFactoryException(VmsgErrors.NO_ENCLOSING_ELEMENT);
		}

		final int separatorIndex = buffer.indexOf(':');
		final String attributeName = (separatorIndex > -1 ?
				buffer.substring(0, separatorIndex) : buffer);
		final String attributeValue = (separatorIndex > -1 ?
				buffer.substring(separatorIndex + 1) : "");

		final Element innermostElement = (Element) tagStack.peek();
		innermostElement.setAttribute(attributeName, attributeValue);
	}

}
