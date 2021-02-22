/*
 * Copyright (c) 2020 Jon Chambers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.eatthepath.pushy.apns;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class X509CertificateReaderTest {

    private static final String CERTIFICATE_CHAIN_FILENAME = "apns-certificate-chain.pem";

    @Test
    void testReadCertificatesFromFile() throws URISyntaxException {
        final File pemFile = Paths.get(getClass().getResource(CERTIFICATE_CHAIN_FILENAME).toURI()).toFile();

        final X509Certificate[] certificates = assertDoesNotThrow(() -> X509CertificateReader.readCertificates(pemFile));
        assertEquals(2, certificates.length);

    }

    @Test
    void testReadCertificatesFromInputStream() throws IOException {
        try (final InputStream pemInputStream = getClass().getResourceAsStream(CERTIFICATE_CHAIN_FILENAME)) {
            final X509Certificate[] certificates = assertDoesNotThrow(() -> X509CertificateReader.readCertificates(pemInputStream));
            assertEquals(2, certificates.length);
        }
    }

    @Test
    void testReadBogusCertificate() throws IOException {
        final byte[] nonsenseBytes = new byte[1024];
        new Random().nextBytes(nonsenseBytes);

        try (final ByteArrayInputStream nonsenseInputStream = new ByteArrayInputStream(nonsenseBytes)) {
            assertThrows(CertificateException.class, () -> X509CertificateReader.readCertificates(nonsenseInputStream));
        }
    }
}