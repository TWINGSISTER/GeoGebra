package org.geogebra.web.html5.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geogebra.common.awt.GPoint2D;
import org.geogebra.common.euclidian.DrawableND;
import org.geogebra.common.euclidian.EmbedManager;
import org.geogebra.common.euclidian.EuclidianView;
import org.geogebra.common.euclidian.draw.DrawInlineText;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoInlineText;
import org.geogebra.common.main.App;
import org.geogebra.common.move.ggtapi.models.json.JSONArray;
import org.geogebra.common.move.ggtapi.models.json.JSONException;
import org.geogebra.common.move.ggtapi.models.json.JSONObject;
import org.geogebra.common.util.AsyncOperation;
import org.geogebra.common.util.CopyPaste;
import org.geogebra.common.util.InternalClipboard;
import org.geogebra.common.util.StringUtil;
import org.geogebra.common.util.debug.Log;
import org.geogebra.web.html5.Browser;
import org.geogebra.web.html5.gui.util.BrowserStorage;
import org.geogebra.web.html5.main.AppW;
import org.geogebra.web.html5.main.Clipboard;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TextAreaElement;

import elemental2.core.Function;
import elemental2.core.Global;
import elemental2.core.JsArray;
import elemental2.dom.Blob;
import elemental2.dom.BlobPropertyBag;
import elemental2.dom.DomGlobal;
import elemental2.dom.FileReader;
import elemental2.dom.HTMLImageElement;
import elemental2.promise.Promise;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;

public class CopyPasteW extends CopyPaste {

	private static final String pastePrefix = "ggbpastedata";

	private static final int defaultTextWidth = 300;

	@Override
	public void copyToXML(App app, List<GeoElement> geos) {
		String textToSave = InternalClipboard.getTextToSave(app, geos, Global::escape);
		saveToClipboard(textToSave);
	}

	/**
	 * @param toWrite text to be copied
	 * @return whether text is non-empty
	 */
	public static boolean writeToExternalClipboardIfNonempty(String toWrite) {
		if (StringUtil.empty(toWrite)) {
			return false;
		}
		writeToExternalClipboard(toWrite);
		BrowserStorage.LOCAL.setItem(pastePrefix, toWrite);
		return true;
	}

	/**
	 * @param toWrite string to be copied
	 */
	public static void writeToExternalClipboard(String toWrite) {
		if (copyToExternalSupported()) {
			// Supported in Chrome
			BlobPropertyBag bag =
					BlobPropertyBag.create();
			bag.setType("text/plain");
			Blob blob = new Blob(new Blob.ConstructorBlobPartsArrayUnionType[]{
					Blob.ConstructorBlobPartsArrayUnionType.of(toWrite)}, bag);
			Clipboard.ClipboardItem data = new Clipboard.ClipboardItem(JsPropertyMap.of(
				"text/plain", blob));

			Clipboard.write(JsArray.of(data)).then(ignore -> {
				Log.debug("successfully wrote gegeobra data to clipboard");
				return null;
			}, (ignore) -> {
				Log.debug("writing geogebra data to clipboard failed");
				return null;
			});
		} else if (Js.isTruthy(Js.asPropertyMap(DomGlobal.navigator)
				.nestedGet("clipboard.writeText"))) {
			// Supported in Firefox

			Clipboard.writeText(toWrite).then((ignore) -> {
				Log.debug("successfully wrote text to clipboard");
				return null;
			}, (ignore) -> {
				Log.debug("writing text to clipboard failed");
				return null;
			});
		} else {
			// Supported in Safari

			TextAreaElement copyFrom = AppW.getHiddenTextArea();
			copyFrom.setValue(toWrite);
			copyFrom.select();
			Function exec =
					(Function) Js.asPropertyMap(DomGlobal.document)
							.get("execCommand");
			exec.call(DomGlobal.document, "copy");
			DomGlobal.setTimeout((ignore) -> DomGlobal.document.body.focus(), 0);
		}
	}

	private static void saveToClipboard(String toSave) {
		String escapedContent = Global.escape(toSave);
		String encoded = pastePrefix + DomGlobal.btoa(escapedContent);
		if (!Browser.isiOS() || copyToExternalSupported()) {
			writeToExternalClipboard(encoded);
		}
		BrowserStorage.LOCAL.setItem(pastePrefix, encoded);
	}

	private static boolean copyToExternalSupported() {
		return navigatorSupports("clipboard.write");
	}

	@Override
	public void pasteFromXML(final App app) {
		paste(app, text -> pasteText(app, text));
	}

	private static void handleStorageFallback(AsyncOperation<String> callback) {
		callback.callback(BrowserStorage.LOCAL.getItem(pastePrefix));
	}

	@Override
	public void paste(App app, AsyncOperation<String> plainTextFallback) {
		pasteNative(app, text -> {
			if (text.startsWith(pastePrefix)) {
				pasteEncoded(app, text);
			} else {
				plainTextFallback.callback(text);
			}
		});
	}

	/**
	 * @param app application
	 * @param callback consumer for the pasted string
	 */
	public static void pasteNative(App app, AsyncOperation<String> callback) {
		if (navigatorSupports("clipboard.read")) {
			// supported in Chrome
			Clipboard
				.read()
				.then((data) -> {
						for (int i = 0; i < data.length; i++) {
							for (int j = 0; j < data.getAt(i).types.length; j++) {
								String type = data.getAt(i).types.getAt(j);
								if (type.equals("image/png")) {
									FileReader reader = new FileReader();

									reader.addEventListener("load", (ignore) ->
											pasteImage(app, reader.result.asString()), false);

									data.getAt(i).getType("image/png").then((item) -> {
										reader.readAsDataURL(item);
										return null;
									});
								} else if (type.equals("text/plain")
										|| type.equals("text/uri-list")) {
									data.getAt(i).getType(type).then((item) -> {
										readBlob(item, callback);
										return null;
									});
									return null;
								}
							}
						}
						return null;
					},
					(reason) -> {
						Log.debug("reading data from clipboard failed " + reason);
						handleStorageFallback(callback);
						return null;
					});
		} else if (navigatorSupports("clipboard.readText")) {
			// not sure if any browser enters this at the time of writing
			Clipboard.readText().then(
				(text) -> {
					pasteText(app, text);
					return null;
				},
				(reason) -> {
					Log.debug("reading text from clipboard failed: " + reason);
					handleStorageFallback(callback);
					return null;
				});
		} else {
			handleStorageFallback(callback);
		}
	}

	private static void pasteText(App app, String text) {
		if (text.startsWith(pastePrefix)) {
			pasteEncoded(app, text);
		} else {
			pastePlainText(app, text);
		}
	}

	private static void pasteEncoded(App app, String text) {
		String escapedContent = DomGlobal.atob(text.substring(pastePrefix.length()));
		pasteGeoGebraXML(app, Global.unescape(escapedContent));
	}

	private static void pasteImage(App app, String encodedImage) {
		((AppW) app).urlDropHappened(encodedImage, null, null, null);
	}

	private static void pastePlainText(final App app, String plainText) {
		if (app.isWhiteboardActive()) {
			final EuclidianView ev = app.getActiveEuclidianView();

			final GeoInlineText txt = new GeoInlineText(app.getKernel().getConstruction(),
					new GPoint2D(ev.toRealWorldCoordX(-defaultTextWidth), 0));
			txt.setSize(defaultTextWidth, GeoInlineText.DEFAULT_HEIGHT);
			txt.setLabel(null);

			JSONArray array = new JSONArray();
			JSONObject object = new JSONObject();
			try {
				object.put("text", plainText);
			} catch (JSONException e) {
				Log.error(e.getMessage());
				return;
			}
			array.put(object);

			txt.setContent(array.toString());

			final DrawableND drawText =  app.getActiveEuclidianView()
					.getDrawableFor(txt);
			if (drawText != null) {
				drawText.update();
				((DrawInlineText) drawText).updateContent();
				Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
					@Override
					public void execute() {
						int x = (ev.getWidth() - defaultTextWidth) / 2;
						int y = (int) ((ev.getHeight() - txt.getHeight()) / 2);
						txt.setLocation(new GPoint2D(
								ev.toRealWorldCoordX(x), ev.toRealWorldCoordY(y)
						));
						drawText.update();

						ev.getEuclidianController().selectAndShowSelectionUI(txt);
						app.storeUndoInfo();
					}
				});
			}
		}
	}

	private static ArrayList<String> separateXMLLabels(String clipboardContent, int endline) {
		return new ArrayList<>(Arrays.asList(clipboardContent.substring(0, endline).split(" ")));
	}

	private static void pasteGeoGebraXML(App app, String clipboardContent) {
		int endline = clipboardContent.indexOf('\n');

		ArrayList<String> copiedXMLlabels = separateXMLLabels(clipboardContent, endline);

		endline++;
		while (clipboardContent.startsWith(InternalClipboard.imagePrefix, endline)
				|| clipboardContent.startsWith(InternalClipboard.embedPrefix, endline)) {
			int nextEndline = clipboardContent.indexOf('\n', endline);
			String line = clipboardContent
					.substring(endline, nextEndline);

			String[] tokens = line.split(" ", 3);
			if (tokens.length == 3) {
				handleSpecialLine(tokens, app);
			}
			endline = nextEndline + 1;
		}

		String copiedXML = clipboardContent.substring(endline);

		Scheduler.get().scheduleDeferred(
				() -> InternalClipboard.pasteGeoGebraXMLInternal(app, copiedXMLlabels, copiedXML));
	}

	private static void handleSpecialLine(String[] tokens, App app) {
		String prefix = tokens[0];
		String name = Global.unescape(tokens[1]);
		String content = tokens[2];
		if (InternalClipboard.imagePrefix.equals(prefix)) {
			ImageManagerW imageManager = ((AppW) app).getImageManager();
			imageManager.addExternalImage(name, content);
			HTMLImageElement img = imageManager.getExternalImage(name, (AppW) app, true);
			img.src = content;
		} else {
			EmbedManager embedManager = app.getEmbedManager();
			if (embedManager != null) {
				embedManager.setContent(Integer.parseInt(name), content);
			}
		}
	}

	@Override
	public void duplicate(App app, List<GeoElement> geos) {
		InternalClipboard.duplicate(app, geos);
	}

	@Override
	public void clearClipboard() {
		BrowserStorage.LOCAL.setItem(pastePrefix, "");
	}

	@Override
	public void copyTextToSystemClipboard(String text) {
		Log.debug("copying to clipboard " + text);
		writeToExternalClipboard(text);
	}

	public static native void installCutCopyPaste(AppW app, Element target) /*-{
		function incorrectTarget(target) {
			return target.tagName.toUpperCase() === 'INPUT'
				|| target.tagName.toUpperCase() === 'TEXTAREA'
				|| target.tagName.toUpperCase() === 'BR'
				|| target.parentElement.classList.contains('mowTextEditor');
		}

		target.addEventListener('paste', function(a) {
			if (incorrectTarget(a.target)) {
				return;
			}

			if (a.clipboardData.files.length > 0) {
				var reader = new FileReader();

				reader.addEventListener("load", function() {
					@org.geogebra.web.html5.util.CopyPasteW::pasteImage(*)(app, this.result);
				}, false);

				reader.readAsDataURL(a.clipboardData.files[0]);
				return;
			}

			var text = a.clipboardData.getData("text/plain");
			if (text) {
				@org.geogebra.web.html5.util.CopyPasteW::pasteText(*)(app, text);
				return;
			}

			@org.geogebra.web.html5.util.CopyPasteW::pasteInternal(*)(app);
		});

		function cutCopy(event) {
			if (incorrectTarget(event.target)) {
				return;
			}

			@org.geogebra.common.util.CopyPaste::handleCutCopy(*)(app, event.type === 'cut');
		}

		target.addEventListener('copy', cutCopy);
		target.addEventListener('cut', cutCopy)
	}-*/;

	/**
	 * Paste from internal keyboard
	 * @param app application
	 */
	public static void pasteInternal(AppW app) {
		String stored = BrowserStorage.LOCAL.getItem(pastePrefix);
		if (!StringUtil.empty(stored)) {
			pasteGeoGebraXML(app, stored);
		}
	}

	private static void readBlob(Blob blob, AsyncOperation<String> callback) {
		// in Chrome one could use blob.text().then(callback)
		// but the FileReader code is also compatible with Safari 13.1
		FileReader reader = new FileReader();
		reader.addEventListener("loadend", evt -> {
			if (reader.result != null) {
				callback.callback(reader.result.asString());
			}
		});
		reader.readAsText(blob);
	}

	private static void onPermission(AsyncOperation<Boolean> callback) {
		Clipboard.read().then((data) -> {
			if (data.length == 0 || data.getAt(0).types.length == 0) {
				callback.callback(false);
				return null;
			}

			if ("image/png".equals(data.getAt(0).types.getAt(0))) {
				callback.callback(true);
			} else if ("text/plain".equals(data.getAt(0).types.getAt(0))) {
				data.getAt(0).getType("text/plain").then((item) -> {
					callback.callback(item.size > 0);
					return null;
				});
			}
			return null;
		}, (ignore) -> {
			callback.callback(true);
			return null;
		});
	}

	/**
	 * Check if there is any readable content in the system clipboard (if supported),
	 * or the internal clipboard (if not)
	 */
	public static void checkClipboard(AsyncOperation<Boolean> callback) {
		if (navigatorSupports("clipboard.read")) {
			if (navigatorSupports("permissions")) {
				Promise<Permissions.Permission> promise =
						Permissions.query(JsPropertyMap.of("name", "clipboard-read"));

				promise.then((result) -> {
					if ("granted".equals(result.state)) {
						onPermission(callback);
					} else {
						callback.callback(true);
					}
					return null;
				});
			} else {
				// Safari doesn't have navigator.permissions, checking content
				// directly triggers an extra popup on Mac -> just assume we can paste
				callback.callback(true);
			}
		} else {
			callback.callback(!StringUtil.empty(BrowserStorage.LOCAL.getItem(pastePrefix)));
		}
	}

	private static boolean navigatorSupports(String s) {
		return Js.isTruthy(Js.asPropertyMap(DomGlobal.navigator).nestedGet(s));
	}
}
