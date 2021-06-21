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

import com.eatthepath.json.JsonSerializer;

import java.util.*;

/**
 * <p>A base utility class for constructing JSON payloads suitable for inclusion in APNs push notifications. Payload
 * builders are reusable, but are <em>not</em> thread-safe.</p>
 *
 * @author <a href="https://github.com/jchambers">Jon Chambers</a>
 *
 * @see <a href="https://developer.apple.com/documentation/usernotifications/setting_up_a_remote_notification_server/generating_a_remote_notification">Generating a Remote Notification</a>
 *
 * @since 0.14.0
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public abstract class ApnsPayloadBuilder {

    private String alertBody = null;

    private String localizedAlertKey = null;
    private String[] localizedAlertArguments = null;
    private String alertTitle = null;
    private String localizedAlertTitleKey = null;
    private String[] localizedAlertTitleArguments = null;
    private String alertSubtitle = null;
    private String localizedAlertSubtitleKey = null;
    private String[] localizedAlertSubtitleArguments = null;

    private String launchImageFileName = null;

    private boolean showActionButton = true;
    private String actionButtonLabel = null;
    private String localizedActionButtonKey = null;

    private Integer badgeNumber = null;

    private String soundFileName = null;
    private Map<String, Object> soundForCriticalAlert = null;

    private String categoryName = null;

    private boolean contentAvailable = false;
    private boolean mutableContent = false;

    private String threadId = null;
    private String targetContentId = null;

    private String summaryArgument = null;
    private Integer summaryArgumentCount = null;

    private String[] urlArguments = null;

    private final HashMap<String, Object> customProperties = new HashMap<>();

    private boolean preferStringRepresentationForAlerts = false;

    private static final String APS_KEY = "aps";
    private static final String ALERT_KEY = "alert";
    private static final String BADGE_KEY = "badge";
    private static final String SOUND_KEY = "sound";
    private static final String CATEGORY_KEY = "category";
    private static final String CONTENT_AVAILABLE_KEY = "content-available";
    private static final String MUTABLE_CONTENT_KEY = "mutable-content";
    private static final String THREAD_ID_KEY = "thread-id";
    private static final String TARGET_CONTENT_ID_KEY = "target-content-id";
    private static final String SUMMARY_ARGUMENT_KEY = "summary-arg";
    private static final String SUMMARY_ARGUMENT_COUNT_KEY = "summary-arg-count";
    private static final String URL_ARGS_KEY = "url-args";

    private static final String ALERT_TITLE_KEY = "title";
    private static final String ALERT_TITLE_LOC_KEY = "title-loc-key";
    private static final String ALERT_TITLE_ARGS_KEY = "title-loc-args";
    private static final String ALERT_SUBTITLE_KEY = "subtitle";
    private static final String ALERT_SUBTITLE_LOC_KEY = "subtitle-loc-key";
    private static final String ALERT_SUBTITLE_ARGS_KEY = "subtitle-loc-args";
    private static final String ALERT_BODY_KEY = "body";
    private static final String ALERT_LOC_KEY = "loc-key";
    private static final String ALERT_ARGS_KEY = "loc-args";
    private static final String ACTION_KEY = "action";
    private static final String ACTION_LOC_KEY = "action-loc-key";
    private static final String LAUNCH_IMAGE_KEY = "launch-image";

    private static final String MDM_KEY = "mdm";

    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * The name of the iOS default push notification sound.
     *
     * @see ApnsPayloadBuilder#setSoundFileName(String)
     */
    public static final String DEFAULT_SOUND_FILENAME = "default";

    /**
    /**
     * Sets whether this payload builder will attempt to represent alerts as strings when possible. Older versions of
     * the APNs specification recommended representing alerts as strings when only a literal alert body was provided,
     * but recent versions recommend representing alerts as dictionaries regardless. This method is provided primarily
     * for backward-compatibility. By default, payload builders will always represent alerts as dictionaries.
     *
     * @param preferStringRepresentationForAlerts if {@code true}, then this payload builder will represent alerts as
     * strings when possible; otherwise, alerts will always be represented as dictionaries
     *
     * @return a reference to this payload builder
     *
     * @since 0.8.2
     */
    public ApnsPayloadBuilder setPreferStringRepresentationForAlerts(final boolean preferStringRepresentationForAlerts) {
        this.preferStringRepresentationForAlerts = preferStringRepresentationForAlerts;
        return this;
    }

    /**
     * <p>Sets the literal text of the alert message to be shown for the push notification. Clears any previously-set
     * localized alert message key and arguments.</p>
     *
     * <p>By default, no message is shown.</p>
     *
     * @param alertBody the literal message to be shown for this push notification
     *
     * @return a reference to this payload builder
     *
     * @see ApnsPayloadBuilder#setLocalizedAlertMessage(String, String...)
     */
    public ApnsPayloadBuilder setAlertBody(final String alertBody) {
        this.alertBody = alertBody;

        this.localizedAlertKey = null;
        this.localizedAlertArguments = null;

        return this;
    }

    /**
     * <p>Sets the key of a message in the receiving app's localized string list to be shown for the push notification.
     * The message in the app's string list may optionally have placeholders, which will be populated by values from the
     * given {@code alertArguments}. Clears any previously-set literal alert body.</p>
     *
     * <p>By default, no message is shown.</p>
     *
     * @param localizedAlertKey a key to a string in the receiving app's localized string list
     * @param alertArguments arguments to populate placeholders in the localized alert string; may be {@code null}
     *
     * @return a reference to this payload builder
     *
     * @see ApnsPayloadBuilder#setAlertBody(String)
     */
    public ApnsPayloadBuilder setLocalizedAlertMessage(final String localizedAlertKey, final String... alertArguments) {
        this.localizedAlertKey = localizedAlertKey;
        this.localizedAlertArguments = (alertArguments != null && alertArguments.length > 0) ? alertArguments : null;

        this.alertBody = null;

        return this;
    }

    /**
     * <p>Sets the key of a message in the receiving app's localized string list to be shown for the push notification,
     * along with a fallback for apps that do not have the given string in the string list.
     * The message in the app's string list may optionally have placeholders, which will be populated by values from the
     * given {@code alertArguments}. Clears any previously-set literal alert body.</p>
     *
     * <p>By default, no message is shown.</p>
     *
     * @param localizedAlertKey a key to a string in the receiving app's localized string list
     * @param fallback the message to be shown for this push notification if the key does not exist
     * @param alertArguments arguments to populate placeholders in the localized alert string; may be {@code null}
     *
     * @return a reference to this payload builder
     *
     * @see ApnsPayloadBuilder#setAlertBody(String)
     */
    public ApnsPayloadBuilder setLocalizedAlertMessageWithFallback(final String localizedAlertKey, final String fallback, final String... alertArguments) {
        this.localizedAlertKey = localizedAlertKey;
        this.localizedAlertArguments = (alertArguments != null && alertArguments.length > 0) ? alertArguments : null;

        this.alertBody = fallback;

        return this;
    }

    /**
     * <p>Sets a short description of the notification purpose. Clears any previously-set localized title key and
     * arguments. The Apple Watch will display the title as part of the notification. According to Apple's
     * documentation, this should be:</p>
     *
     * <blockquote>A short string describing the purpose of the notification. Apple Watch displays this string as part
     * of the notification interface. This string is displayed only briefly and should be crafted so that it can be
     * understood quickly.</blockquote>
     *
     * <p>By default, no title is included.</p>
     *
     * @param alertTitle the description to be shown for this push notification
     *
     * @return a reference to this payload builder
     *
     * @see ApnsPayloadBuilder#setLocalizedAlertTitle(String, String...)
     */
    public ApnsPayloadBuilder setAlertTitle(final String alertTitle) {
        this.alertTitle = alertTitle;

        this.localizedAlertTitleKey = null;
        this.localizedAlertTitleArguments = null;

        return this;
    }

    /**
     * <p>Sets the key of the title string in the receiving app's localized string list to be shown for the push
     * notification. Clears any previously-set literal alert title. The message in the app's string list may optionally
     * have placeholders, which will be populated by values from the given {@code alertArguments}.</p>
     *
     * @param localizedAlertTitleKey a key to a string in the receiving app's localized string list
     * @param alertTitleArguments arguments to populate placeholders in the localized alert string; may be {@code null}
     *
     * @return a reference to this payload builder
     */
    public ApnsPayloadBuilder setLocalizedAlertTitle(final String localizedAlertTitleKey, final String... alertTitleArguments) {
        this.localizedAlertTitleKey = localizedAlertTitleKey;
        this.localizedAlertTitleArguments = (alertTitleArguments != null && alertTitleArguments.length > 0) ? alertTitleArguments : null;

        this.alertTitle = null;

        return this;
    }

    /**
     * <p>Sets the key of the title string in the receiving app's localized string list to be shown for the push
     * notification, along with a fallback for apps that do not have the given string in the string list.
     * The message in the app's string list may optionally have placeholders, which will be populated by values from
     * the given {@code alertArguments}.</p>
     *
     * @param localizedAlertTitleKey a key to a string in the receiving app's localized string list
     * @param fallback the description to be shown for this push notification if the key does not exist
     * @param alertTitleArguments arguments to populate placeholders in the localized alert string; may be {@code null}
     *
     * @return a reference to this payload builder
     */
    public ApnsPayloadBuilder setLocalizedAlertTitleWithFallback(final String localizedAlertTitleKey, final String fallback, final String... alertTitleArguments) {
        this.localizedAlertTitleKey = localizedAlertTitleKey;
        this.localizedAlertTitleArguments = (alertTitleArguments != null && alertTitleArguments.length > 0) ? alertTitleArguments : null;

        this.alertTitle = fallback;

        return this;
    }

    /**
     * <p>Sets a subtitle for the notification. Clears any previously-set localized subtitle key and arguments.</p>
     *
     * <p>By default, no subtitle is included. Requires iOS 10 or newer.</p>
     *
     * @param alertSubtitle the subtitle for this push notification
     *
     * @return a reference to this payload builder
     *
     * @since 0.8.1
     */
    public ApnsPayloadBuilder setAlertSubtitle(final String alertSubtitle) {
        this.alertSubtitle = alertSubtitle;

        this.localizedAlertSubtitleKey = null;
        this.localizedAlertSubtitleArguments = null;

        return this;
    }




    /**
     * <p>Sets the key of the subtitle string in the receiving app's localized string list to be shown for the push
     * notification. Clears any previously-set literal subtitle. The message in the app's string list may optionally
     * have placeholders, which will be populated by values from the given {@code alertSubtitleArguments}.</p>
     *
     * <p>By default, no subtitle is included. Requires iOS 10 or newer.</p>
     *
     * @param localizedAlertSubtitleKey a key to a string in the receiving app's localized string list
     * @param alertSubtitleArguments arguments to populate placeholders in the localized subtitle string; may be
     * {@code null}
     *
     * @return a reference to this payload builder
     *
     * @since 0.8.1
     */
    public ApnsPayloadBuilder setLocalizedAlertSubtitle(final String localizedAlertSubtitleKey, final String... alertSubtitleArguments) {
        this.localizedAlertSubtitleKey = localizedAlertSubtitleKey;
        this.localizedAlertSubtitleArguments = (alertSubtitleArguments != null && alertSubtitleArguments.length > 0) ? alertSubtitleArguments : null;

        this.alertSubtitle = null;

        return this;
    }

    /**
     * <p>Sets the key of the subtitle string in the receiving app's localized string list to be shown for the push
     * notification, along with a placeholder for apps that do not have the given string in the string list.
     * The message in the app's string list may optionally have placeholders, which will be populated by values from
     * the given {@code alertSubtitleArguments}.</p>
     *
     * <p>By default, no subtitle is included. Requires iOS 10 or newer.</p>
     *
     * @param localizedAlertSubtitleKey a key to a string in the receiving app's localized string list
     * @param fallback the subtitle to be shown for this push notification if the key does not exist
     * @param alertSubtitleArguments arguments to populate placeholders in the localized subtitle string; may be
     * {@code null}
     *
     * @return a reference to this payload builder
     *
     * @since 0.8.1
     */
    public ApnsPayloadBuilder setLocalizedAlertSubtitleWithFallback(final String localizedAlertSubtitleKey, final String fallback, final String... alertSubtitleArguments) {
        this.localizedAlertSubtitleKey = localizedAlertSubtitleKey;
        this.localizedAlertSubtitleArguments = (alertSubtitleArguments != null && alertSubtitleArguments.length > 0) ? alertSubtitleArguments : null;

        this.alertSubtitle = fallback;

        return this;
    }

    /**
     * <p>Sets the image to be shown when the receiving app launches in response to this push notification. According
     * to Apple's documentation, this should be:</p>
     *
     * <blockquote>The filename of an image file in the application bundle; it may include the extension or omit it.
     * The image is used as the launch image when users tap the action button or move the action slider. If this
     * property is not specified, the system either uses the previous snapshot, uses the image identified by the
     * {@code UILaunchImageFile} key in the application’s {@code Info.plist} file, or falls back to
     * {@code Default.png}.</blockquote>
     *
     * @param launchImageFilename the filename of an image file in the receiving app's bundle to be shown when launching
     * the app from the push notification
     *
     * @return a reference to this payload builder
     */
    public ApnsPayloadBuilder setLaunchImageFileName(final String launchImageFilename) {
        this.launchImageFileName = launchImageFilename;
        return this;
    }

    /**
     * <p>Sets whether an &quot;action&quot; button should be shown if the push notification is displayed as an alert.
     * If {@code true} and no localized action button key is set, the default label (defined by the receiving operating
     * system) is used. If @{code true} and a localized action button key is set, the string for that key is used as
     * the label of the action button. If {@code false}, no action button is shown under any circumstances</p>
     *
     * <p>By default, an action button will be shown.</p>
     *
     * @param showActionButton {@code true} to show an action button when the push notification is presented as an alert
     * or {@code false} to show an alert with no action button
     *
     * @return a reference to this payload builder
     */
    public ApnsPayloadBuilder setShowActionButton(final boolean showActionButton) {
        this.showActionButton = showActionButton;
        return this;
    }

    /**
     * <p>Sets the literal text of the action button to be shown for the push notification <em>for Safari push
     * notifications only</em>. Clears any previously-set localized action key. By default, the OS-default label will be
     * used for the action button.</p>
     *
     * @param action the literal label to be shown on the action button for this notification
     *
     * @return a reference to this payload builder
     *
     * @see ApnsPayloadBuilder#setLocalizedActionButtonKey(String)
     * @see ApnsPayloadBuilder#setShowActionButton(boolean)
     *
     * @since 0.8.2
     */
    public ApnsPayloadBuilder setActionButtonLabel(final String action) {
        this.actionButtonLabel = action;
        this.localizedActionButtonKey = null;

        return this;
    }

    /**
     * <p>Sets the key of a string in the receiving app's localized string list to be used as the label of the
     * &quot;action&quot; button if the push notification is displayed as an alert. Clears any previously-set literal
     * action button label. By default, the OS-default label will be used for the action button.</p>
     *
     * @param localizedActionButtonKey a key to a string in the receiving app's localized string list
     *
     * @return a reference to this payload builder
     *
     * @see ApnsPayloadBuilder#setActionButtonLabel(String)
     * @see ApnsPayloadBuilder#setShowActionButton(boolean)
     */
    public ApnsPayloadBuilder setLocalizedActionButtonKey(final String localizedActionButtonKey) {
        this.localizedActionButtonKey = localizedActionButtonKey;
        this.actionButtonLabel = null;

        return this;
    }

    /**
     * <p>Sets the number to display as the badge of the icon of the application that receives the push notification.
     * If the badge number is 0, the badge is removed from the application icon. If {@code null}, the badge is left in
     * its current state. By default, no change is made to the badge.</p>
     *
     * @param badgeNumber the number to display as the badge of application or {@code null} to leave the badge unchanged
     *
     * @return a reference to this payload builder
     */
    public ApnsPayloadBuilder setBadgeNumber(final Integer badgeNumber) {
        this.badgeNumber = badgeNumber;
        return this;
    }

    /**
     * <p>Sets the name of the action category name for interactive remote notifications. According to Apple's
     * documentation, this should be:</p>
     *
     * <blockquote>...a string value that represents the identifier property of the
     * {@code UIMutableUserNotificationCategory} object you created to define custom actions.</blockquote>
     *
     * @param categoryName the action category name
     *
     * @return a reference to this payload builder
     *
     * @see <a href="https://developer.apple.com/documentation/usernotifications/declaring_your_actionable_notification_types">Declaring Your Actionable Notification Types</a>
     */
    public ApnsPayloadBuilder setCategoryName(final String categoryName) {
        this.categoryName = categoryName;
        return this;
    }

    /**
     * <p>Sets the name of the sound file to play when the push notification is received. According to Apple's
     * documentation, the sound filename should be:</p>
     *
     * <blockquote>The name of a sound file in your app’s main bundle or in the {@code Library/Sounds} folder of your
     * app’s container directory.</blockquote>
     *
     * <p>By default, no sound is included in the push notification.</p>
     *
     * @param soundFileName the name of the sound file to play, or {@code null} to send no sound
     *
     * @return a reference to this payload builder
     *
     * @see ApnsPayloadBuilder#DEFAULT_SOUND_FILENAME
     *
     * @deprecated As of v0.13.3, please use {@link #setSound(String)} instead.
     */
    @Deprecated
    public ApnsPayloadBuilder setSoundFileName(final String soundFileName) {
        return this.setSound(soundFileName);
    }

    /**
     * <p>Sets the name of the sound file to play when the push notification is received. According to Apple's
     * documentation, the sound filename should be:</p>
     *
     * <blockquote>The name of a sound file in your app’s main bundle or in the {@code Library/Sounds} folder of your
     * app’s container directory.</blockquote>
     *
     * <p>By default, no sound is included in the push notification.</p>
     *
     * @param soundFileName the name of the sound file to play, or {@code null} to send no sound
     *
     * @return a reference to this payload builder
     *
     * @see ApnsPayloadBuilder#DEFAULT_SOUND_FILENAME
     *
     * @since 0.13.3
     */
    public ApnsPayloadBuilder setSound(final String soundFileName) {
        this.soundFileName = soundFileName;
        this.soundForCriticalAlert = null;

        return this;
    }

    /**
     * <p>Sets the name of the sound file to play when the push notification is received along with its volume and
     * whether it should be presented as a critical alert. According to Apple's documentation, the sound filename
     * should be:</p>
     *
     * <blockquote>The name of a sound file in your app’s main bundle or in the {@code Library/Sounds} folder of your
     * app’s container directory.</blockquote>
     *
     * <p>By default, no sound is included in the push notification.</p>
     *
     * <p>To explicitly specify that no sound should be played as part of this notification, use
     * {@link #setSound(String)} with a {@code null} sound filename.</p>
     *
     * @param soundFileName the name of the sound file to play; must not be {@code null}
     * @param isCriticalAlert specifies whether this sound should be played as a "critical alert"
     * @param soundVolume the volume at which to play the sound; must be between 0.0 (silent) and 1.0 (loudest)
     *
     * @return a reference to this payload builder
     *
     * @see ApnsPayloadBuilder#DEFAULT_SOUND_FILENAME
     *
     * @since 0.13.3
     */
    public ApnsPayloadBuilder setSound(final String soundFileName, final boolean isCriticalAlert, final double soundVolume) {
        Objects.requireNonNull(soundFileName, "Sound file name must not be null.");

        if (soundVolume < 0 || soundVolume > 1) {
            throw new IllegalArgumentException("Sound volume must be between 0.0 and 1.0 (inclusive).");
        }

        this.soundFileName = null;
        this.soundForCriticalAlert = buildSoundForCriticalAlertMap(soundFileName, isCriticalAlert, soundVolume);

        return this;
    }

    private static Map<String, Object> buildSoundForCriticalAlertMap(final String name, final boolean critical, final double volume) {
        final Map<String, Object> soundForCriticalAlertMap = new HashMap<>(3, 1);

        soundForCriticalAlertMap.put("name", name);
        soundForCriticalAlertMap.put("critical", critical ? 1 : 0);
        soundForCriticalAlertMap.put("volume", volume);

        return soundForCriticalAlertMap;
    }

    /**
     * <p>Sets whether the payload under construction should contain a flag that indicates that new content is available
     * to be downloaded in the background by the receiving app. By default, no content availability flag is included
     * in the payload.</p>
     *
     * @param contentAvailable {@code true} to include a flag that indicates that new content is available to be
     * downloaded in the background or {@code false} otherwise
     *
     * @return a reference to this payload builder
     *
     * @see <a href=
     *      "https://developer.apple.com/documentation/usernotifications/setting_up_a_remote_notification_server/pushing_updates_to_your_app_silently">Pushing Updates to Your App Silently</a>
     */
    public ApnsPayloadBuilder setContentAvailable(final boolean contentAvailable) {
        this.contentAvailable = contentAvailable;
        return this;
    }

    /**
     * Sets whether the receiving device may modify the content of the push notification before displaying it. Requires
     * iOS 10 or newer.
     *
     * @param mutableContent {@code true} if the receiving device may modify the push notification before displaying it
     * or {@code false} otherwise
     *
     * @return a reference to this payload builder
     *
     * @see <a href=
     *      "https://developer.apple.com/reference/usernotifications/unnotificationserviceextension">
     *      UNNotificationServiceExtension</a>
     */
    public ApnsPayloadBuilder setMutableContent(final boolean mutableContent) {
        this.mutableContent = mutableContent;
        return this;
    }

    /**
     * <p>Sets the thread ID for this notification. According to to the APNs documentation, the thread ID is:</p>
     *
     * <blockquote>…a string value that represents the app-specific identifier for grouping notifications. The system
     * groups notifications with the same thread identifier together in Notification Center and other system
     * interfaces.</blockquote>
     *
     * <p>By default, no thread ID is included.</p>
     *
     * @param threadId the thread ID for this notification
     *
     * @return a reference to this payload builder
     *
     * @since 0.8.2
     */
    public ApnsPayloadBuilder setThreadId(final String threadId) {
        this.threadId = threadId;
        return this;
    }

    /**
     * Sets the identifier of the window to be brought forward by this notification. By default, no target content ID
     * is included.
     *
     * @param targetContentId the identifier of the window to be brought forward by this notification
     *
     * @return a reference to this payload builder
     *
     * @since 0.13.10
     */
    public ApnsPayloadBuilder setTargetContentId(final String targetContentId) {
        this.targetContentId = targetContentId;
        return this;
    }

    /**
     * <p>Sets the summary argument for this notification. The summary argument is:</p>
     *
     * <blockquote>The string the notification adds to the category’s summary format string.</blockquote>
     *
     * <p>For example in iOS 12, when defining an {@code UNNotificationCategory}, passing a format string like
     * "%u more messages from %@" to the {@code categorySummaryFormat} argument, will produce
     * "x more messages from {summaryArgument}.</p>
     *
     * <p>In iOS 12, the default summary format string in English is "%u more notifications" and does not have a
     * placeholder for a summary argument. By default, no summary argument is included in push notification payloads.</p>
     *
     * @param summaryArgument the summary argument for this notification; if {@code null}, the {@code summary-arg} key
     * is omitted from the payload entirely
     *
     * @return a reference to this payload builder
     *
     * @see <a href=
     *      "https://developer.apple.com/documentation/usernotifications/unnotificationcontent">
     *      UNNotificationContent</a>
     *
     * @since 0.13.6
     */
    public ApnsPayloadBuilder setSummaryArgument(final String summaryArgument) {
        this.summaryArgument = summaryArgument;
        return this;
    }

    /**
     * <p>Sets the summary argument count for this notification. The summary argument count is:</p>
     *
     * <blockquote>The number of items the notification adds to the category’s summary format string.</blockquote>
     *
     * <p>By default, all notifications count as a single "item" in a group of notifications. The summary argument count
     * controls how many "items" in a "stack" of notifications are represented by a specific notification.
     * If, for example, a notification informed a user that seven new podcasts are available, it might be helpful to set
     * the summary argument count to 7. When "stacked," the notification would contribute an item count of 7
     * to the total number of notifications reported in the summary string (for example, "7 more podcasts").
     * </p>
     *
     * <p>By default, notifications count as a single "item" in a group of notifications, and so the default
     * summary argument count is 1 (even if no summary argument count is specified in the notification payload).
     * If specified, summary argument count must be positive.</p>
     *
     * @param summaryArgumentCount the summary argument count for this notification; if {@code null}, the
     * {@code summary-arg-count} key is omitted from the payload entirely
     *
     * @return a reference to this payload builder
     *
     * @see <a href=
     *      "https://developer.apple.com/documentation/usernotifications/unnotificationcontent">
     *      UNNotificationContent</a>
     *
     * @since 0.13.6
     */
    public ApnsPayloadBuilder setSummaryArgumentCount(final Integer summaryArgumentCount) {
        if (summaryArgumentCount != null && summaryArgumentCount < 1) {
            throw new IllegalArgumentException("Summary argument count must be positive.");
        }

        this.summaryArgumentCount = summaryArgumentCount;

        return this;
    }

    /**
     * <p>Sets the list of arguments to populate placeholders in the {@code urlFormatString} associated with a Safari
     * push notification. Has no effect for non-Safari notifications. According to the Notification Programming Guide
     * for Websites:</p>
     *
     * <blockquote>The {@code url-args} key must be included [for Safari push notifications]. The number of elements in
     * the array must match the number of placeholders in the {@code urlFormatString} value and the order of the
     * placeholders in the URL format string determines the order of the values supplied by the {@code url-args} array.
     * The number of placeholders may be zero, in which case the array should be empty. However, it is common practice
     * to always include at least one argument so that the user is directed to a web page specific to the notification
     * received.</blockquote>
     *
     * @param arguments the arguments with which to populate URL placeholders, which may be an empty list; if
     * {@code null}, the {@code url-args} key is omitted from the payload entirely
     *
     * @return a reference to this payload builder
     *
     * @see <a href="https://developer.apple.com/library/content/documentation/NetworkingInternet/Conceptual/NotificationProgrammingGuideForWebsites/PushNotifications/PushNotifications.html#//apple_ref/doc/uid/TP40013225-CH3-SW1">
     *      Notification Programming Guide for Websites - Configuring Safari Push Notifications</a>
     *
     * @since 0.8.2
     */
    public ApnsPayloadBuilder setUrlArguments(final List<String> arguments) {
        return this.setUrlArguments(arguments != null ? arguments.toArray(EMPTY_STRING_ARRAY) : null);
    }

    /**
     * <p>Sets the list of arguments to populate placeholders in the {@code urlFormatString} associated with a Safari
     * push notification. Has no effect for non-Safari notifications. According to the Notification Programming Guide
     * for Websites:</p>
     *
     * <blockquote>The {@code url-args} key must be included [for Safari push notifications]. The number of elements in
     * the array must match the number of placeholders in the {@code urlFormatString} value and the order of the
     * placeholders in the URL format string determines the order of the values supplied by the {@code url-args} array.
     * The number of placeholders may be zero, in which case the array should be empty. However, it is common practice
     * to always include at least one argument so that the user is directed to a web page specific to the notification
     * received.</blockquote>
     *
     * @param arguments the arguments with which to populate URL placeholders, which may be an empty array; if
     * {@code null}, the {@code url-args} key is omitted from the payload entirely
     *
     * @return a reference to this payload builder
     *
     * @see <a href="https://developer.apple.com/library/content/documentation/NetworkingInternet/Conceptual/NotificationProgrammingGuideForWebsites/PushNotifications/PushNotifications.html#//apple_ref/doc/uid/TP40013225-CH3-SW1">
     *      Notification Programming Guide for Websites - Configuring Safari Push Notifications</a>
     *
     * @since 0.8.2
     */
    public ApnsPayloadBuilder setUrlArguments(final String... arguments) {
        this.urlArguments = arguments;
        return this;
    }

    /**
     * <p>Adds a custom property to the payload. According to Apple's documentation:</p>
     *
     * <blockquote>Providers can specify custom payload values outside the Apple-reserved {@code aps} namespace. Custom
     * values must use the JSON structured and primitive types: dictionary (object), array, string, number, and Boolean.
     * You should not include customer information (or any sensitive data) as custom payload data. Instead, use it for
     * such purposes as setting context (for the user interface) or internal metrics. For example, a custom payload
     * value might be a conversation identifier for use by an instant-message client application or a timestamp
     * identifying when the provider sent the notification. Any action associated with an alert message should not be
     * destructive—for example, it should not delete data on the device.</blockquote>
     *
     * <p>The value for the property is serialized to JSON as described in {@link JsonSerializer}.</p>
     *
     * @param key the key of the custom property in the payload object
     * @param value the value of the custom property
     *
     * @return a reference to this payload builder
     */
    public ApnsPayloadBuilder addCustomProperty(final String key, final Object value) {
        this.customProperties.put(key, value);
        return this;
    }

    /**
     * Returns a map representing the push notification payload under construction. Subclasses will generally serialize
     * this map as a JSON string to produce a push notification payload.
     *
     * @return a map representing the push notification payload under construction
     *
     * @since 0.14.0
     */
    protected Map<String, Object> buildPayloadMap() {
        final Map<String, Object> payload = new HashMap<>();

        {
            final Map<String, Object> aps = new HashMap<>();

            if (this.badgeNumber != null) {
                aps.put(BADGE_KEY, this.badgeNumber);
            }

            if (this.soundFileName != null) {
                aps.put(SOUND_KEY, this.soundFileName);
            } else if (this.soundForCriticalAlert != null) {
                aps.put(SOUND_KEY, this.soundForCriticalAlert);
            }

            if (this.categoryName != null) {
                aps.put(CATEGORY_KEY, this.categoryName);
            }

            if (this.contentAvailable) {
                aps.put(CONTENT_AVAILABLE_KEY, 1);
            }

            if (this.mutableContent) {
                aps.put(MUTABLE_CONTENT_KEY, 1);
            }

            if (this.threadId != null) {
                aps.put(THREAD_ID_KEY, this.threadId);
            }

            if (this.targetContentId != null) {
                aps.put(TARGET_CONTENT_ID_KEY, this.targetContentId);
            }

            if (this.urlArguments != null) {
                aps.put(URL_ARGS_KEY, this.urlArguments);
            }

            final Map<String, Object> alert = new HashMap<>();
            {
                if (this.alertBody != null) {
                    alert.put(ALERT_BODY_KEY, this.alertBody);
                }

                if (this.alertTitle != null) {
                    alert.put(ALERT_TITLE_KEY, this.alertTitle);
                }

                if (this.alertSubtitle != null) {
                    alert.put(ALERT_SUBTITLE_KEY, this.alertSubtitle);
                }

                if (this.summaryArgument != null) {
                    alert.put(SUMMARY_ARGUMENT_KEY, this.summaryArgument);
                }

                if (this.summaryArgumentCount != null) {
                    alert.put(SUMMARY_ARGUMENT_COUNT_KEY, this.summaryArgumentCount);
                }

                if (this.showActionButton) {
                    if (this.localizedActionButtonKey != null) {
                        alert.put(ACTION_LOC_KEY, this.localizedActionButtonKey);
                    }

                    if (this.actionButtonLabel != null) {
                        alert.put(ACTION_KEY, this.actionButtonLabel);
                    }
                } else {
                    // To hide the action button, the key needs to be present, but the value needs to be null
                    alert.put(ACTION_LOC_KEY, null);
                }

                if (this.localizedAlertKey != null) {
                    alert.put(ALERT_LOC_KEY, this.localizedAlertKey);

                    if (this.localizedAlertArguments != null) {
                        alert.put(ALERT_ARGS_KEY, Arrays.asList(this.localizedAlertArguments));
                    }
                }

                if (this.localizedAlertTitleKey != null) {
                    alert.put(ALERT_TITLE_LOC_KEY, this.localizedAlertTitleKey);

                    if (this.localizedAlertTitleArguments != null) {
                        alert.put(ALERT_TITLE_ARGS_KEY, Arrays.asList(this.localizedAlertTitleArguments));
                    }
                }

                if (this.localizedAlertSubtitleKey != null) {
                    alert.put(ALERT_SUBTITLE_LOC_KEY, this.localizedAlertSubtitleKey);

                    if (this.localizedAlertSubtitleArguments != null) {
                        alert.put(ALERT_SUBTITLE_ARGS_KEY, Arrays.asList(this.localizedAlertSubtitleArguments));
                    }
                }

                if (this.launchImageFileName != null) {
                    alert.put(LAUNCH_IMAGE_KEY, this.launchImageFileName);
                }
            }

            if (alert.size() == 1 && alert.containsKey(ALERT_BODY_KEY) && this.preferStringRepresentationForAlerts) {
                aps.put(ALERT_KEY, alert.get(ALERT_BODY_KEY));
            } else if (!alert.isEmpty()) {
                aps.put(ALERT_KEY, alert);
            }

            payload.put(APS_KEY, aps);
        }

        for (final Map.Entry<String, Object> entry : this.customProperties.entrySet()) {
            payload.put(entry.getKey(), entry.getValue());
        }

        return payload;
    }

    /**
     * Returns a JSON representation of the push notification payload under construction.
     *
     * @return a JSON representation of the payload under construction
     *
     * @see #buildPayloadMap()
     *
     * @since 0.14.0
     */
    public abstract String build();

    /**
     * Returns a map representing a
     * <a href="https://developer.apple.com/library/content/documentation/Miscellaneous/Reference/MobileDeviceManagementProtocolRef/1-Introduction/Introduction.html#//apple_ref/doc/uid/TP40017387-CH1-SW1">Mobile
     * Device Management</a> "wake up" payload. Subclasses will generally serialize this map as a JSON string to produce
     * an MDM payload.
     *
     * @param pushMagicValue the "push magic" string that the device sends to the MDM server in a {@code TokenUpdate}
     * message
     *
     * @return a map representing an MDM "wake up" notification payload
     *
     * @since 0.14.0
     */
    protected Map<String, String> buildMdmPayloadMap(final String pushMagicValue) {
        return Collections.singletonMap(MDM_KEY, pushMagicValue);
    }

    /**
     * Returns a JSON representation of a
     * <a href="https://developer.apple.com/library/content/documentation/Miscellaneous/Reference/MobileDeviceManagementProtocolRef/1-Introduction/Introduction.html#//apple_ref/doc/uid/TP40017387-CH1-SW1">Mobile
     * Device Management</a> "wake up" payload.
     *
     * @param pushMagicValue the "push magic" string that the device sends to the MDM server in a {@code TokenUpdate}
     * message
     *
     * @return a JSON representation of an MDM "wake up" notification payload
     *
     * @see <a href="https://developer.apple.com/library/content/documentation/Miscellaneous/Reference/MobileDeviceManagementProtocolRef/3-MDM_Protocol/MDM_Protocol.html#//apple_ref/doc/uid/TP40017387-CH3-SW2">Mobile
     * Device Management (MDM) Protocol</a>
     *
     * @since 0.14.0
     *
     * @see #buildMdmPayloadMap(String)
     */
    public abstract String buildMdmPayload(final String pushMagicValue);
}
