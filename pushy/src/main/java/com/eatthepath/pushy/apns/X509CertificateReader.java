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

import java.io.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * An X.509 certificate reader reads X.509 certificates from PEM files and PEM input streams.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7468">Textual Encodings of PKIX, PKCS, and CMS Structures</a>
 */
class X509CertificateReader {

    private static final X509Certificate[] CERTIFICATE_ARRAY = new X509Certificate[0];

    /**
     * Reads all available X.509 certificates from the given PEM file.
     *
     * @param pemFile a PEM file containing any number of X.509 certificates
     *
     * @return an array of X.509 certificates found in the given PEM file; may be empty, but not null
     *
     * @throws CertificateException if a certificate in the PEM file could not be parsed
     * @throws IOException if the given PEM file could not be read for any reason
     */
    static X509Certificate[] readCertificates(final File pemFile) throws CertificateException, IOException {
        try (final FileInputStream fileInputStream = new FileInputStream(pemFile)) {
            return readCertificates(fileInputStream);
        }
    }

    /**
     * Reads all available X.509 certificates from the given PEM input stream.
     *
     * @param pemInputStream an input stream of PEM-encoded data containing any number of X.509 certificates
     *
     * @return an array of X.509 certificates found in the given PEM input stream; may be empty, but not null
     *
     * @throws CertificateException if a certificate in the PEM input stream could not be parsed
     * @throws IOException if the given PEM input stream could not be read for any reason
     */
    static X509Certificate[] readCertificates(final InputStream pemInputStream) throws CertificateException, IOException {
        try (final BufferedInputStream bufferedInputStream = new BufferedInputStream(pemInputStream)) {
            return readCertificates(bufferedInputStream);
        }
    }

    /**
     * Reads all available X.509 certificates from the given PEM input stream.
     *
     * @param bufferedPemInputStream a buffered input stream of PEM-encoded data containing any number of X.509
     *                               certificates
     *
     * @return an array of X.509 certificates found in the given PEM input stream; may be empty, but not null
     *
     * @throws CertificateException if a certificate in the PEM input stream could not be parsed
     * @throws IOException if the given PEM input stream could not be read for any reason
     */
    private static X509Certificate[] readCertificates(final BufferedInputStream bufferedPemInputStream) throws CertificateException, IOException {
        final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        final List<X509Certificate> certificates = new ArrayList<>();

        while (bufferedPemInputStream.available() > 0) {
            final Certificate certificate = certificateFactory.generateCertificate(bufferedPemInputStream);

            if (certificate instanceof X509Certificate) {
                certificates.add((X509Certificate) certificate);
            }
        }

        return certificates.toArray(CERTIFICATE_ARRAY);
    }
}
