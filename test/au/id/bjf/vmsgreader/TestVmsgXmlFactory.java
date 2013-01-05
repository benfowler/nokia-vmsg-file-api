package au.id.bjf.vmsgreader;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class TestVmsgXmlFactory {

	private static final String TEST_MESSAGE_FILENAME = "au/id/bjf/vmsgreader/test_message.vmg";

	private InputStream getTestMsgInputStream() {
		return ClassLoader.getSystemResourceAsStream(TEST_MESSAGE_FILENAME);
	}

	@Test
	public void testOpenTestFile() throws IOException {
		final InputStream testMsgStream = getTestMsgInputStream();
		assertNotNull("test message file", testMsgStream);

		final BufferedReader testReader = new BufferedReader(
				new InputStreamReader(testMsgStream));
		String buffer;
		while ((buffer = testReader.readLine()) != null) {
			System.out.println(buffer);
		}
	}

	@Test
	public void testXmlFactoryBuildDomFromMessage() throws Exception {
		final InputStream testMsgStream = getTestMsgInputStream();
		final Document dom = VmsgXmlFactory.buildDomFromMessage(testMsgStream);
		assertTrue(dom != null);
		assertTrue(dom.getFirstChild().getNodeType() == Node.ELEMENT_NODE);
		assertTrue(dom.getFirstChild().getNodeName().equals("VMSG"));
		assertTrue(TestUtils.evalXPathBool(dom,
				"contains(//VMSG/VENV/VENV/VBODY, 'Yes thanks')"));
	}
	
	@Test
	public void testXmlFactoryBuildDomFromMessages() throws Exception {
		final String[] DIRS = { 
				"~/Dropbox/Home/phone/20081026-nokia-vmsg/Saved",
				"~/Dropbox/Home/phone/20081026-nokia-vmsg/Saved Sent"};
		for (final String dir : DIRS) {
			// iterate over dir, create fileinput streams
		}
	}

}
