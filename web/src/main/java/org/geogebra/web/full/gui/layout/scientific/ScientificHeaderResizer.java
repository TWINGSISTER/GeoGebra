package org.geogebra.web.full.gui.layout.scientific;

import org.geogebra.web.full.main.HeaderResizer;
import org.geogebra.web.html5.gui.GeoGebraFrameW;
import org.geogebra.web.html5.util.Dom;

import com.google.gwt.dom.client.Element;

public class ScientificHeaderResizer implements HeaderResizer {

	private final GeoGebraFrameW frame;

	public ScientificHeaderResizer(GeoGebraFrameW frame) {
		this.frame = frame;
	}

	@Override
	public void resizeHeader() {
		Element header = Dom.querySelector("GeoGebraHeader");
		if (frame.hasSmallWindowOrCompactHeader()) {
			header.addClassName("smallScreen");
		} else {
			header.removeClassName("smallScreen");
		}
	}

	@Override
	public int getSmallScreenHeight() {
		return 80;
	}
}
