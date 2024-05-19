/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.fileupload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.fileupload.portlet.PortletFileUploadTest;
import org.apache.commons.fileupload.servlet.ServletFileUploadTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Common tests for implementations of {@link FileUpload}. This is a parameterized test.
 * Tests must be valid and common to all implementations of FileUpload added as parameter
 * in this class.
 *
 * @see ServletFileUploadTest
 * @see PortletFileUploadTest
 * @since 1.4
 */
@RunWith(Parameterized.class)
public class FileUploadTest {

    /**
     * @return {@link FileUpload} classes under test.
     */
    @Parameters(name="{0}")
    public static Iterable<? extends Object> data() {
        return Util.fileUploadImplementations();
    }

    /**
     * Current parameterized FileUpload.
     */
    @Parameter
    public FileUpload upload;

    // --- Test methods common to all implementations of a FileUpload

    @Test
    public void testFileUpload()
            throws IOException, FileUploadException {
        final List<FileItem> fileItems = Util.parseUpload(upload,
                                               "-----1234\r\n" +
                                               "Content-Disposition: form-data; name=\"file\"; filename=\"foo.tab\"\r\n" +
                                               "Content-Type: text/whatever\r\n" +
                                               "\r\n" +
                                               "This is the content of the file\n" +
                                               "\r\n" +
                                               "-----1234\r\n" +
                                               "Content-Disposition: form-data; name=\"field\"\r\n" +
                                               "\r\n" +
                                               "fieldValue\r\n" +
                                               "-----1234\r\n" +
                                               "Content-Disposition: form-data; name=\"multi\"\r\n" +
                                               "\r\n" +
                                               "value1\r\n" +
                                               "-----1234\r\n" +
                                               "Content-Disposition: form-data; name=\"multi\"\r\n" +
                                               "\r\n" +
                                               "value2\r\n" +
                                               "-----1234--\r\n");
        assertEquals(4, fileItems.size());

        final FileItem file = fileItems.get(0);
        assertEquals("file", file.getFieldName());
        assertFalse(file.isFormField());
        assertEquals("This is the content of the file\n", file.getString());
        assertEquals("text/whatever", file.getContentType());
        assertEquals("foo.tab", file.getName());

        final FileItem field = fileItems.get(1);
        assertEquals("field", field.getFieldName());
        assertTrue(field.isFormField());
        assertEquals("fieldValue", field.getString());

        final FileItem multi0 = fileItems.get(2);
        assertEquals("multi", multi0.getFieldName());
        assertTrue(multi0.isFormField());
        assertEquals("value1", multi0.getString());

        final FileItem multi1 = fileItems.get(3);
        assertEquals("multi", multi1.getFieldName());
        assertTrue(multi1.isFormField());
        assertEquals("value2", multi1.getString());
    }

    @Test
    public void testFilenameCaseSensitivity()
            throws IOException, FileUploadException {
        final List<FileItem> fileItems = Util.parseUpload(upload,
                                               "-----1234\r\n" +
                                               "Content-Disposition: form-data; name=\"FiLe\"; filename=\"FOO.tab\"\r\n" +
                                               "Content-Type: text/whatever\r\n" +
                                               "\r\n" +
                                               "This is the content of the file\n" +
                                               "\r\n" +
                                               "-----1234--\r\n");
        assertEquals(1, fileItems.size());

        final FileItem file = fileItems.get(0);
        assertEquals("FiLe", file.getFieldName());
        assertEquals("FOO.tab", file.getName());
    }

    /**
     * This is what the browser does if you submit the form without choosing a file.
     */
    @Test
    public void testEmptyFile()
            throws UnsupportedEncodingException, FileUploadException {
        final List<FileItem> fileItems = Util.parseUpload (upload,
                                                "-----1234\r\n" +
                                                "Content-Disposition: form-data; name=\"file\"; filename=\"\"\r\n" +
                                                "\r\n" +
                                                "\r\n" +
                                                "-----1234--\r\n");
        assertEquals(1, fileItems.size());

        final FileItem file = fileItems.get(0);
        assertFalse(file.isFormField());
        assertEquals("", file.getString());
        assertEquals("", file.getName());
    }

    /**
     * Internet Explorer 5 for the Mac has a bug where the carriage
     * return is missing on any boundary line immediately preceding
     * an input with type=image. (type=submit does not have the bug.)
     */
    @Test
    public void testIE5MacBug()
            throws UnsupportedEncodingException, FileUploadException {
        final List<FileItem> fileItems = Util.parseUpload(upload,
                                               "-----1234\r\n" +
                                               "Content-Disposition: form-data; name=\"field1\"\r\n" +
                                               "\r\n" +
                                               "fieldValue\r\n" +
                                               "-----1234\n" + // NOTE \r missing
                                               "Content-Disposition: form-data; name=\"submitName.x\"\r\n" +
                                               "\r\n" +
                                               "42\r\n" +
                                               "-----1234\n" + // NOTE \r missing
                                               "Content-Disposition: form-data; name=\"submitName.y\"\r\n" +
                                               "\r\n" +
                                               "21\r\n" +
                                               "-----1234\r\n" +
                                               "Content-Disposition: form-data; name=\"field2\"\r\n" +
                                               "\r\n" +
                                               "fieldValue2\r\n" +
                                               "-----1234--\r\n");

        assertEquals(4, fileItems.size());

        final FileItem field1 = fileItems.get(0);
        assertEquals("field1", field1.getFieldName());
        assertTrue(field1.isFormField());
        assertEquals("fieldValue", field1.getString());

        final FileItem submitX = fileItems.get(1);
        assertEquals("submitName.x", submitX.getFieldName());
        assertTrue(submitX.isFormField());
        assertEquals("42", submitX.getString());

        final FileItem submitY = fileItems.get(2);
        assertEquals("submitName.y", submitY.getFieldName());
        assertTrue(submitY.isFormField());
        assertEquals("21", submitY.getString());

        final FileItem field2 = fileItems.get(3);
        assertEquals("field2", field2.getFieldName());
        assertTrue(field2.isFormField());
        assertEquals("fieldValue2", field2.getString());
    }

    /**
     * Test for <a href="http://issues.apache.org/jira/browse/FILEUPLOAD-62">FILEUPLOAD-62</a>
     */
    @Test
    public void testFILEUPLOAD62() throws Exception {
        final String contentType = "multipart/form-data; boundary=AaB03x";
        final String request =
            "--AaB03x\r\n" +
            "content-disposition: form-data; name=\"field1\"\r\n" +
            "\r\n" +
            "Joe Blow\r\n" +
            "--AaB03x\r\n" +
            "content-disposition: form-data; name=\"pics\"\r\n" +
            "Content-type: multipart/mixed; boundary=BbC04y\r\n" +
            "\r\n" +
            "--BbC04y\r\n" +
            "Content-disposition: attachment; filename=\"file1.txt\"\r\n" +
            "Content-Type: text/plain\r\n" +
            "\r\n" +
            "... contents of file1.txt ...\r\n" +
            "--BbC04y\r\n" +
            "Content-disposition: attachment; filename=\"file2.gif\"\r\n" +
            "Content-type: image/gif\r\n" +
            "Content-Transfer-Encoding: binary\r\n" +
            "\r\n" +
            "...contents of file2.gif...\r\n" +
            "--BbC04y--\r\n" +
            "--AaB03x--";
        final List<FileItem> fileItems = Util.parseUpload(upload, request.getBytes("US-ASCII"), contentType);
        assertEquals(3, fileItems.size());
        final FileItem item0 = fileItems.get(0);
        assertEquals("field1", item0.getFieldName());
        assertNull(item0.getName());
        assertEquals("Joe Blow", new String(item0.get()));
        final FileItem item1 = fileItems.get(1);
        assertEquals("pics", item1.getFieldName());
        assertEquals("file1.txt", item1.getName());
        assertEquals("... contents of file1.txt ...", new String(item1.get()));
        final FileItem item2 = fileItems.get(2);
        assertEquals("pics", item2.getFieldName());
        assertEquals("file2.gif", item2.getName());
        assertEquals("...contents of file2.gif...", new String(item2.get()));
    }

    /**
     * Test for <a href="http://issues.apache.org/jira/browse/FILEUPLOAD-111">FILEUPLOAD-111</a>
     */
    @Test
    public void testFoldedHeaders()
            throws IOException, FileUploadException {
        final List<FileItem> fileItems = Util.parseUpload(upload, "-----1234\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"foo.tab\"\r\n" +
                "Content-Type: text/whatever\r\n" +
                "\r\n" +
                "This is the content of the file\n" +
                "\r\n" +
                "-----1234\r\n" +
                "Content-Disposition: form-data; \r\n" +
                "\tname=\"field\"\r\n" +
                "\r\n" +
                "fieldValue\r\n" +
                "-----1234\r\n" +
                "Content-Disposition: form-data;\r\n" +
                "     name=\"multi\"\r\n" +
                "\r\n" +
                "value1\r\n" +
                "-----1234\r\n" +
                "Content-Disposition: form-data; name=\"multi\"\r\n" +
                "\r\n" +
                "value2\r\n" +
                "-----1234--\r\n");
        assertEquals(4, fileItems.size());

        final FileItem file = fileItems.get(0);
        assertEquals("file", file.getFieldName());
        assertFalse(file.isFormField());
        assertEquals("This is the content of the file\n", file.getString());
        assertEquals("text/whatever", file.getContentType());
        assertEquals("foo.tab", file.getName());

        final FileItem field = fileItems.get(1);
        assertEquals("field", field.getFieldName());
        assertTrue(field.isFormField());
        assertEquals("fieldValue", field.getString());

        final FileItem multi0 = fileItems.get(2);
        assertEquals("multi", multi0.getFieldName());
        assertTrue(multi0.isFormField());
        assertEquals("value1", multi0.getString());

        final FileItem multi1 = fileItems.get(3);
        assertEquals("multi", multi1.getFieldName());
        assertTrue(multi1.isFormField());
        assertEquals("value2", multi1.getString());
    }

    /**
     * Test case for <a href="http://issues.apache.org/jira/browse/FILEUPLOAD-130">
     */
    @Test
    public void testFileUpload130()
            throws Exception {
        final String[] headerNames = new String[]
        {
            "SomeHeader", "OtherHeader", "YetAnotherHeader", "WhatAHeader"
        };
        final String[] headerValues = new String[]
        {
            "present", "Is there", "Here", "Is That"
        };
        final List<FileItem> fileItems = Util.parseUpload(upload,
                                               "-----1234\r\n" +
                                               "Content-Disposition: form-data; name=\"file\"; filename=\"foo.tab\"\r\n" +
                                               "Content-Type: text/whatever\r\n" +
                                               headerNames[0] + ": " + headerValues[0] + "\r\n" +
                                               "\r\n" +
                                               "This is the content of the file\n" +
                                               "\r\n" +
                                               "-----1234\r\n" +
                                               "Content-Disposition: form-data; \r\n" +
                                               "\tname=\"field\"\r\n" +
                                               headerNames[1] + ": " + headerValues[1] + "\r\n" +
                                               "\r\n" +
                                               "fieldValue\r\n" +
                                               "-----1234\r\n" +
                                               "Content-Disposition: form-data;\r\n" +
                                               "     name=\"multi\"\r\n" +
                                               headerNames[2] + ": " + headerValues[2] + "\r\n" +
                                               "\r\n" +
                                               "value1\r\n" +
                                               "-----1234\r\n" +
                                               "Content-Disposition: form-data; name=\"multi\"\r\n" +
                                               headerNames[3] + ": " + headerValues[3] + "\r\n" +
                                               "\r\n" +
                                               "value2\r\n" +
                                               "-----1234--\r\n");
        assertEquals(4, fileItems.size());

        final FileItem file = fileItems.get(0);
        assertHeaders(headerNames, headerValues, file, 0);

        final FileItem field = fileItems.get(1);
        assertHeaders(headerNames, headerValues, field, 1);

        final FileItem multi0 = fileItems.get(2);
        assertHeaders(headerNames, headerValues, multi0, 2);

        final FileItem multi1 = fileItems.get(3);
        assertHeaders(headerNames, headerValues, multi1, 3);
    }

    /**
     * Test for <a href="http://issues.apache.org/jira/browse/FILEUPLOAD-239">FILEUPLOAD-239</a>
     */
    @Test
    public void testContentTypeAttachment()
            throws IOException, FileUploadException {
        final List<FileItem> fileItems = Util.parseUpload(upload,
                "-----1234\r\n" +
                "content-disposition: form-data; name=\"field1\"\r\n" +
                "\r\n" +
                "Joe Blow\r\n" +
                "-----1234\r\n" +
                "content-disposition: form-data; name=\"pics\"\r\n" +
                "Content-type: multipart/mixed, boundary=---9876\r\n" +
                "\r\n" +
                "-----9876\r\n" +
                "Content-disposition: attachment; filename=\"file1.txt\"\r\n" +
                "Content-Type: text/plain\r\n" +
                "\r\n" +
                "... contents of file1.txt ...\r\n" +
                "-----9876--\r\n" +
                "-----1234--\r\n");
        assertEquals(2, fileItems.size());

        final FileItem field = fileItems.get(0);
        assertEquals("field1", field.getFieldName());
        assertTrue(field.isFormField());
        assertEquals("Joe Blow", field.getString());

        final FileItem file = fileItems.get(1);
        assertEquals("pics", file.getFieldName());
        assertFalse(file.isFormField());
        assertEquals("... contents of file1.txt ...", file.getString());
        assertEquals("text/plain", file.getContentType());
        assertEquals("file1.txt", file.getName());
    }

    private void assertHeaders(final String[] pHeaderNames, final String[] pHeaderValues,
            final FileItem pItem, final int pIndex) {
        for (int i = 0; i < pHeaderNames.length; i++) {
            final String value = pItem.getHeaders().getHeader(pHeaderNames[i]);
            if (i == pIndex) {
                assertEquals(pHeaderValues[i], value);
            } else {
                assertNull(value);
            }
        }
    }

    /**
     * Test for multipart/related without any content-disposition Header.
     * <p/>
     * This kind of Content-Type is commonly used by SOAP-Requests with Attachments (MTOM)
     */
    @Test
    public void testMultipartRelated() throws FileUploadException {
        final String soapEnvelope =
                "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">\r\n" +
                        "  <soap:Header></soap:Header>\r\n" +
                        "  <soap:Body>\r\n" +
                        "    <ns1:Test xmlns:ns1=\"http://www.test.org/some-test-namespace\">\r\n" +
                        "      <ns1:Attachment>\r\n" +
                        "        <xop:Include xmlns:xop=\"http://www.w3.org/2004/08/xop/include\"" +
                        " href=\"ref-to-attachment%40some.domain.org\"/>\r\n" +
                        "      </ns1:Attachment>\r\n" +
                        "    </ns1:Test>\r\n" +
                        "  </soap:Body>\r\n" +
                        "</soap:Envelope>";

        final String content = "-----1234\r\n" +
                "content-type: application/xop+xml; type=\"application/soap+xml\"\r\n" +
                "\r\n" +
                soapEnvelope + "\r\n" +
                "-----1234\r\n" +
                "Content-type: text/plain\r\n" +
                "content-id: <ref-to-attachment@some.domain.org>\r\n" +
                "\r\n" +
                "some text/plain content\r\n" +
                "-----1234--\r\n";

        final List<FileItem> fileItems = Util.parseUpload(upload, content.getBytes(StandardCharsets.US_ASCII),
                "multipart/related; boundary=---1234; type=\"application/xop+xml\"; start-info=\"application/soap+xml\"");
        assertEquals(2, fileItems.size());

        final FileItem part1 = fileItems.get(0);
        assertNull(part1.getFieldName());
        assertFalse(part1.isFormField());
        assertEquals(soapEnvelope, part1.getString());

        final FileItem part2 = fileItems.get(1);
        assertNull(part2.getFieldName());
        assertFalse(part2.isFormField());
        assertEquals("some text/plain content", part2.getString());
        assertEquals("text/plain", part2.getContentType());
        assertNull(part2.getName());
    }
}
