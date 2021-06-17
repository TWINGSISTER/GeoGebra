package org.geogebra.web.full.main.activity;

import org.geogebra.common.main.Localization;
import org.geogebra.common.main.settings.config.AppConfigNotes;
import org.geogebra.web.full.css.MaterialDesignResources;
import org.geogebra.web.full.gui.MessagePanel;
import org.geogebra.web.full.gui.layout.DockPanelW;
import org.geogebra.web.full.gui.layout.panels.AlgebraDockPanelW;
import org.geogebra.web.html5.Browser;
import org.geogebra.web.html5.gui.GeoGebraFrameW;
import org.geogebra.web.html5.gui.laf.VendorSettings;
import org.geogebra.web.html5.main.AppW;

/**
 * Activity class for the notes app
 */
public class NotesActivity extends BaseActivity {

	private static final String MESSAGE_PANEL_STYLE_NAME = "unsupportedBrowserMessage";

	/**
	 * New notes activity
	 */
	public NotesActivity() {
		super(new AppConfigNotes());
	}

	@Override
	public DockPanelW createAVPanel() {
		return new AlgebraDockPanelW(null, true);
	}

	@Override
	public void start(AppW appW) {
		super.start(appW);
		if (Browser.isIE()) {
			showBrowserNotSupportedMessage(appW);
		}
	}

	private void showBrowserNotSupportedMessage(AppW app) {
		Localization localization = app.getLocalization();
		VendorSettings vendorSettings = app.getVendorSettings();
		MessagePanel messagePanel = createBrowserNotSupportedMessage(localization, vendorSettings);
		GeoGebraFrameW frame = app.getAppletFrame();
		frame.clear();
		frame.add(messagePanel);
		frame.forceHeaderHidden(true);
	}

	private MessagePanel createBrowserNotSupportedMessage(
			Localization localization, VendorSettings vendorSettings) {
		MessagePanel messagePanel = new MessagePanel();
		messagePanel.addStyleName(MESSAGE_PANEL_STYLE_NAME);
		messagePanel.setImageUri(MaterialDesignResources.INSTANCE.mow_lightbulb());
		messagePanel.setPanelTitle(localization.getMenu("UnsupportedBrowser"));
		String messageKey = vendorSettings.getMenuLocalizationKey("UnsupportedBrowser.Message");
		messagePanel.setPanelMessage(localization.getMenu(messageKey));

		return messagePanel;
	}

	@Override
	public boolean isWhiteboard() {
		return true;
	}
}
