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

package com.eatthepath.pushy.apns.util;

import com.eatthepath.json.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public abstract class ApnsPayloadBuilderTest {

    private ApnsPayloadBuilder builder;

    protected abstract ApnsPayloadBuilder getBuilder();

    @BeforeEach
    public void setUp() {
        this.builder = getBuilder();
    }

    @Test
    void testSetAlertBody() {
        final String alertBody = "This is a test alert message.";

        this.builder.setLocalizedAlertMessage("test.alert");
        this.builder.setAlertBody(alertBody);

        {
            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

            @SuppressWarnings("unchecked")
            final Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

            assertEquals(alertBody, alert.get("body"));
        }

        this.builder.setPreferStringRepresentationForAlerts(true);

        {
            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());
            assertEquals(alertBody, aps.get("alert"));
        }

        this.builder.setLocalizedAlertMessage("test.alert");
        this.builder.setAlertBody(alertBody);
        this.builder.setShowActionButton(false);

        {
            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

            @SuppressWarnings("unchecked")
            final Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

            assertEquals(alertBody, alert.get("body"));
            assertNull(alert.get("loc-key"));
            assertNull(alert.get("loc-args"));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void testSetLocalizedAlertBody() {
        final String alertKey = "test.alert";

        this.builder.setAlertBody("Alert body!");
        this.builder.setLocalizedAlertMessage(alertKey);

        {
            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());
            final Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

            assertEquals(alertKey, alert.get("loc-key"));
            assertNull(alert.get("loc-args"));
            assertNull(alert.get("body"));
        }

        // We're happy here as long as nothing explodes
        this.builder.setLocalizedAlertMessage(alertKey, (String[]) null);

        final String[] alertArgs = new String[] { "Moose", "helicopter" };
        this.builder.setLocalizedAlertMessage(alertKey, alertArgs);

        {
            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());
            final Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

            assertEquals(alertKey, alert.get("loc-key"));

            final List<Object> argsArray = (List<Object>) alert.get("loc-args");
            assertEquals(alertArgs.length, argsArray.size());
            assertTrue(argsArray.containsAll(java.util.Arrays.asList(alertArgs)));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void testSetLocalizedAlertBodyWithFallback() {
        final String alertKey = "test.alert";
        final String alertBodyFallback = "fallback";

        this.builder.setAlertBody("Alert body!");
        this.builder.setLocalizedAlertMessageWithFallback(alertKey, alertBodyFallback);

        {
            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());
            final Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

            assertEquals(alertKey, alert.get("loc-key"));
            assertEquals(alertBodyFallback, alert.get("body"));
            assertNull(alert.get("loc-args"));
        }

        // We're happy here as long as nothing explodes
        this.builder.setLocalizedAlertMessageWithFallback(alertKey, null, (String[]) null);

        final String[] alertArgs = new String[] { "Moose", "helicopter" };
        this.builder.setLocalizedAlertMessageWithFallback(alertKey, alertBodyFallback, alertArgs);

        {
            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());
            final Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

            assertEquals(alertKey, alert.get("loc-key"));
            assertEquals(alertBodyFallback, alert.get("body"));

            final List<Object> argsArray = (List<Object>) alert.get("loc-args");
            assertEquals(alertArgs.length, argsArray.size());
            assertTrue(argsArray.containsAll(java.util.Arrays.asList(alertArgs)));
        }
    }

    @Test
    void testSetAlertTitle() {
        final String alertTitle = "This is a test alert message.";

        this.builder.setLocalizedAlertTitle("alert.title");
        this.builder.setAlertTitle(alertTitle);

        {
            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

            @SuppressWarnings("unchecked")
            final Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

            assertEquals(alertTitle, alert.get("title"));
            assertNull(alert.get("title-loc-key"));
            assertNull(alert.get("title-loc-args"));
        }
    }

    @Test
    void testSetLocalizedAlertTitle() {
        final String localizedAlertTitleKey = "alert.title";

        this.builder.setAlertTitle("Alert title!");
        this.builder.setLocalizedAlertTitle(localizedAlertTitleKey);

        {
            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

            @SuppressWarnings("unchecked")
            final Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

            assertEquals(localizedAlertTitleKey, alert.get("title-loc-key"));
            assertNull(alert.get("title-loc-args"));
            assertNull(alert.get("title"));
        }

        // We're happy here as long as nothing explodes
        this.builder.setLocalizedAlertTitle(localizedAlertTitleKey, (String[]) null);

        final String[] alertArgs = new String[] { "Moose", "helicopter" };
        this.builder.setLocalizedAlertTitle(localizedAlertTitleKey, alertArgs);

        {
            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

            @SuppressWarnings("unchecked")
            final Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

            assertEquals(localizedAlertTitleKey, alert.get("title-loc-key"));

            @SuppressWarnings("unchecked")
            final List<Object> argsArray = (List<Object>) alert.get("title-loc-args");
            assertEquals(alertArgs.length, argsArray.size());
            assertTrue(argsArray.containsAll(java.util.Arrays.asList(alertArgs)));
        }
    }

    @Test
    void testSetLocalizedAlertTitleWithFallback() {
        final String localizedAlertTitleKey = "alert.title";
        final String alertTitleFallback = "alertFallback";

        this.builder.setAlertTitle("Alert title!");
        this.builder.setLocalizedAlertTitleWithFallback(localizedAlertTitleKey, alertTitleFallback);

        {
            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

            @SuppressWarnings("unchecked")
            final Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

            assertEquals(localizedAlertTitleKey, alert.get("title-loc-key"));
            assertEquals(alertTitleFallback, alert.get("title"));
            assertNull(alert.get("title-loc-args"));
        }

        // We're happy here as long as nothing explodes
        this.builder.setLocalizedAlertTitleWithFallback(localizedAlertTitleKey, null, (String[]) null);

        final String[] alertArgs = new String[] { "Moose", "helicopter" };
        this.builder.setLocalizedAlertTitleWithFallback(localizedAlertTitleKey, alertTitleFallback, alertArgs);

        {
            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

            @SuppressWarnings("unchecked")
            final Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

            assertEquals(localizedAlertTitleKey, alert.get("title-loc-key"));
            assertEquals(alertTitleFallback, alert.get("title"));

            @SuppressWarnings("unchecked")
            final List<Object> argsArray = (List<Object>) alert.get("title-loc-args");
            assertEquals(alertArgs.length, argsArray.size());
            assertTrue(argsArray.containsAll(java.util.Arrays.asList(alertArgs)));
        }
    }

    @Test
    void testSetAlertSubtitle() {
        final String alertSubtitle = "This is a test alert message.";

        this.builder.setLocalizedAlertSubtitle("alert.subtitle");
        this.builder.setAlertSubtitle(alertSubtitle);

        {
            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

            @SuppressWarnings("unchecked")
            final Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

            assertEquals(alertSubtitle, alert.get("subtitle"));
            assertNull(alert.get("subtitle-loc-key"));
            assertNull(alert.get("subtitle-loc-args"));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void testSetLocalizedAlertSubtitle() {
        final String subtitleKey = "test.subtitle";

        this.builder.setAlertSubtitle("Subtitle!");
        this.builder.setLocalizedAlertSubtitle(subtitleKey);

        {
            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());
            final Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

            assertEquals(subtitleKey, alert.get("subtitle-loc-key"));
            assertNull(alert.get("subtitle-loc-args"));
            assertNull(alert.get("subtitle"));
        }

        // We're happy here as long as nothing explodes
        this.builder.setLocalizedAlertSubtitle(subtitleKey, (String[]) null);

        final String[] subtitleArgs = new String[] { "Moose", "helicopter" };
        this.builder.setLocalizedAlertSubtitle(subtitleKey, subtitleArgs);

        {
            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());
            final Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

            assertEquals(subtitleKey, alert.get("subtitle-loc-key"));

            final List<Object> argsArray = (List<Object>) alert.get("subtitle-loc-args");
            assertEquals(subtitleArgs.length, argsArray.size());
            assertTrue(argsArray.containsAll(java.util.Arrays.asList(subtitleArgs)));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void testSetLocalizedAlertSubtitleWithFallback() {
        final String subtitleKey = "test.subtitle";
        final String subtitleFallback = "fallback";

        this.builder.setAlertSubtitle("Subtitle!");
        this.builder.setLocalizedAlertSubtitleWithFallback(subtitleKey, subtitleFallback);

        {
            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());
            final Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

            assertEquals(subtitleKey, alert.get("subtitle-loc-key"));
            assertEquals(subtitleFallback, alert.get("subtitle"));
            assertNull(alert.get("subtitle-loc-args"));
        }

        // We're happy here as long as nothing explodes
        this.builder.setLocalizedAlertTitleWithFallback(subtitleKey, null, (String[]) null);

        final String[] subtitleArgs = new String[] { "Moose", "helicopter" };
        this.builder.setLocalizedAlertTitleWithFallback(subtitleKey, subtitleFallback, subtitleArgs);

        {
            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());
            final Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

            assertEquals(subtitleKey, alert.get("subtitle-loc-key"));
            assertEquals(subtitleFallback, alert.get("subtitle"));

            final List<Object> argsArray = (List<Object>) alert.get("subtitle-loc-args");
            assertEquals(subtitleArgs.length, argsArray.size());
            assertTrue(argsArray.containsAll(java.util.Arrays.asList(subtitleArgs)));
        }
    }

    @Test
    void testSetAlertTitleAndBody() {
        final String alertTitle = "This is a short alert title";
        final String alertBody = "This is a longer alert body";

        this.builder.setAlertBody(alertBody);
        this.builder.setAlertTitle(alertTitle);

        {
            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

            @SuppressWarnings("unchecked")
            final Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

            assertEquals(alertTitle, alert.get("title"));
            assertEquals(alertBody, alert.get("body"));
        }
    }

    @Test
    void testSetLaunchImage() {
        final String launchImageFilename = "launch.png";
        this.builder.setLaunchImageFileName(launchImageFilename);

        final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

        @SuppressWarnings("unchecked")
        final Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

        assertEquals(launchImageFilename, alert.get("launch-image"));
    }

    @Test
    void testSetShowActionButton() {
        this.builder.setShowActionButton(true);

        {
            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

            assertNull(aps.get("alert"));
        }

        this.builder.setShowActionButton(false);

        {
            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

            @SuppressWarnings("unchecked")
            final Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

            assertTrue(alert.containsKey("action-loc-key"));
            assertNull(alert.get("action-loc-key"));
        }

        final String actionButtonKey = "action.key";
        this.builder.setLocalizedActionButtonKey(actionButtonKey);
        this.builder.setShowActionButton(true);

        {
            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

            @SuppressWarnings("unchecked")
            final Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

            assertEquals(actionButtonKey, alert.get("action-loc-key"));
        }

        this.builder.setShowActionButton(false);

        {
            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

            @SuppressWarnings("unchecked")
            final Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

            assertTrue(alert.containsKey("action-loc-key"));
            assertNull(alert.get("action-loc-key"));
        }
    }

    @Test
    void testSetActionButtonLabel() {
        final String actionButtonLabel = "Bam! Pow!";

        this.builder.setLocalizedActionButtonKey("action.key");
        this.builder.setActionButtonLabel(actionButtonLabel);

        final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

        @SuppressWarnings("unchecked")
        final Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

        assertEquals(actionButtonLabel, alert.get("action"));
        assertNull(alert.get("action-loc-key"));
    }

    @Test
    void testSetLocalizedActionButtonKey() {
        final String actionButtonKey = "action.key";

        this.builder.setActionButtonLabel("Literal action button label");
        this.builder.setLocalizedActionButtonKey(actionButtonKey);

        final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

        @SuppressWarnings("unchecked")
        final Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

        assertEquals(actionButtonKey, alert.get("action-loc-key"));
        assertNull(alert.get("action"));
    }

    @Test
    void testSetBadgeNumber() {
        final int badgeNumber = 4;
        this.builder.setBadgeNumber(badgeNumber);

        final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

        assertEquals(badgeNumber, ((Number) aps.get("badge")).intValue());
    }

    @Test
    void testSetSound() {
        final String soundFileName = "dying-giraffe.aiff";
        this.builder.setSound(soundFileName);

        final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

        assertEquals(soundFileName, aps.get("sound"));
    }

    @ParameterizedTest
    @MethodSource("argumentsForSetSoundForCriticalAlert")
    void testSetSoundForCriticalAlert(final boolean isCriticalAlert, final int expectedCriticalValue) {
        final String soundFileName = "dying-giraffe.aiff";
        final double soundVolume = 0.781;

        this.builder.setSound(soundFileName, isCriticalAlert, soundVolume);

        final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

        @SuppressWarnings("unchecked")
        final Map<String, Object> soundDictionary = (Map<String, Object>) aps.get("sound");

        assertEquals(soundFileName, soundDictionary.get("name"));
        assertEquals(expectedCriticalValue, ((Number) soundDictionary.get("critical")).intValue());
        assertEquals(soundVolume, soundDictionary.get("volume"));
    }

    private static Stream<Arguments> argumentsForSetSoundForCriticalAlert() {
        return Stream.of(
                arguments(true, 1),
                arguments(false, 0));
    }

    @Test
    void testSetSoundForCriticalAlertNullFilename() {
        assertThrows(NullPointerException.class, () -> this.builder.setSound(null, true, 0.5));
    }

    @Test
    void testSetSoundForCriticalAlertLowVolume() {
        assertThrows(IllegalArgumentException.class, () -> this.builder.setSound("test", true, -4));
    }

    @Test
    void testSetSoundForCriticalAlertHighVolume() {
        assertThrows(IllegalArgumentException.class, () -> this.builder.setSound("test", true, 4));
    }

    @Test
    void testSetCategoryName() {
        final String categoryName = "INVITE";
        this.builder.setCategoryName(categoryName);

        final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

        assertEquals(categoryName, aps.get("category"));
    }

    @Test
    void testSetContentAvailable() {
        {
            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());
            assertNull(aps.get("content-available"));
        }

        {
            this.builder.setContentAvailable(true);

            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());
            assertEquals(1, ((Number) aps.get("content-available")).intValue());
        }
    }

    @Test
    void testSetMutableContent() {
        {
            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());
            assertNull(aps.get("mutable-content"));
        }

        {
            this.builder.setMutableContent(true);

            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());
            assertEquals(1, ((Number) aps.get("mutable-content")).intValue());
        }
    }

    @Test
    void testSetThreadId() {
        final String threadId = "example.thread.identifier";

        this.builder.setThreadId(threadId);

        final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());
        assertEquals(threadId, aps.get("thread-id"));
    }

    @Test
    public void setTargetContentId() {
        final String targetContentId = "example.window.id";

        this.builder.setTargetContentId(targetContentId);

        final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());
        assertEquals(targetContentId, aps.get("target-content-id"));
    }

    @Test
    void testSetSummaryArgument() {
        final String summaryArgument = "This is a summary argument";

        this.builder.setSummaryArgument(summaryArgument);

        final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

        @SuppressWarnings("unchecked")
        final Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

        assertEquals(summaryArgument, alert.get("summary-arg"));
    }

    @Test
    void testSetSummaryArgumentCount() {
        final int argumentSummaryCount = 3;

        this.builder.setSummaryArgumentCount(argumentSummaryCount);

        final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

        @SuppressWarnings("unchecked")
        final Map<String, Object> alert = (Map<String, Object>) aps.get("alert");

        assertEquals(argumentSummaryCount, ((Number) alert.get("summary-arg-count")).intValue());
    }

    @Test
    void testSetSummaryArgumentCountNonPositive() {
        assertThrows(IllegalArgumentException.class, () -> this.builder.setSummaryArgumentCount(0));
    }

    @Test
    void testAddCustomProperty() throws ParseException {
        final String customKey = "string";
        final String customValue = "Hello";

        this.builder.addCustomProperty(customKey, customValue);

        final Map<String, Object> payload = new JsonParser().parseJsonObject(this.builder.build());

        assertEquals(customValue, payload.get(customKey));
    }

    @Test
    void testSetUrlArgumentsList() {
        {
            final List<String> arguments = new ArrayList<>();
            arguments.add("first");
            arguments.add("second");
            arguments.add("third");

            this.builder.setUrlArguments(arguments);

            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

            assertEquals(arguments, aps.get("url-args"));
        }

        {
            final List<String> arguments = new ArrayList<>();
            this.builder.setUrlArguments(arguments);

            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

            assertEquals(arguments, aps.get("url-args"));
        }

        {
            this.builder.setUrlArguments((List<String>) null);

            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

            assertNull(aps.get("url-args"));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void testSetUrlArgumentsArray() {
        {
            this.builder.setUrlArguments("first", "second", "third");

            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

            final List<String> argumentsFromPayload = (List<String>) aps.get("url-args");

            assertEquals(3, argumentsFromPayload.size());
            assertEquals("first", argumentsFromPayload.get(0));
            assertEquals("second", argumentsFromPayload.get(1));
            assertEquals("third", argumentsFromPayload.get(2));
        }

        {
            @SuppressWarnings("ZeroLengthArrayAllocation") final String[] arguments = new String[0];
            this.builder.setUrlArguments(arguments);

            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

            assertTrue(((List<String>) aps.get("url-args")).isEmpty());
        }

        {
            this.builder.setUrlArguments((String[]) null);

            final Map<String, Object> aps = this.extractApsObjectFromPayloadString(this.builder.build());

            assertNull(aps.get("url-args"));
        }
    }

    @Test
    void testBuildMdmPayload() {
        assertEquals("{\"mdm\":\"Magic!\"}", builder.buildMdmPayload("Magic!"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractApsObjectFromPayloadString(final String payloadString) {
        final Map<String, Object> payload;

        try {
            payload = new JsonParser().parseJsonObject(payloadString);
        } catch (final ParseException e) {
            throw new IllegalArgumentException("Failed to parse JSON: " + payloadString, e);
        }

        return (Map<String, Object>) payload.get("aps");
    }
}
