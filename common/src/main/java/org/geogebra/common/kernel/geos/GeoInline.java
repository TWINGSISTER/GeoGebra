package org.geogebra.common.kernel.geos;

import org.geogebra.common.awt.GPoint2D;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.arithmetic.NumberValue;
import org.geogebra.common.kernel.arithmetic.ValueType;
import org.geogebra.common.kernel.kernelND.GeoPointND;
import org.geogebra.common.kernel.matrix.Coords;
import org.geogebra.common.util.MyMath;
import org.geogebra.common.util.StringUtil;

public abstract class GeoInline extends GeoElement implements Translateable, PointRotateable,
		RectangleTransformable {

	private GPoint2D location;

	private double width;
	private double height;

	private double angle;

	private double contentWidth;
	private double contentHeight;

	private double xScale;
	private double yScale;

	// only used for loading files that were created before zoom was enabled for text elements
	private boolean zoomingEnabled = true;

	public GeoInline(Construction cons) {
		super(cons);
	}

	@Override
	public ValueType getValueType() {
		return ValueType.TEXT;
	}

	@Override
	public boolean showInAlgebraView() {
		return false;
	}

	@Override
	protected boolean showInEuclidianView() {
		return true;
	}

	@Override
	public boolean isAlgebraViewEditable() {
		return false;
	}

	@Override
	public boolean isLabelShowable() {
		return false;
	}

	@Override
	public HitType getLastHitType() {
		return HitType.ON_FILLING;
	}

	@Override
	public double getHeight() {
		return height;
	}

	@Override
	public double getWidth() {
		return width;
	}

	@Override
	public double getAngle() {
		return angle;
	}

	@Override
	public GPoint2D getLocation() {
		return location;
	}

	@Override
	public void setSize(double width, double height) {
		if (getWidth() != 0) {
			contentWidth = contentWidth * width / getWidth();
		}
		if (getHeight() != 0) {
			contentHeight = contentHeight * height / getHeight();
		}
		this.width = width;
		this.height = height;
	}

	/**
	 * @param angle rotation angle in radians
	 */
	@Override
	public void setAngle(double angle) {
		this.angle = angle;
	}

	/**
	 * @param location
	 *            on-screen location
	 */
	@Override
	public void setLocation(GPoint2D location) {
		this.location = location;
	}

	/**
	 * @param content editor content; encoding depends on editor type
	 */
	public abstract void setContent(String content);

	/**
	 * @return editor content; encoding depends on editor type
	 */
	public abstract String getContent();

	@Override
	public void translate(Coords v) {
		location.setLocation(location.getX() + v.getX(), location.getY() + v.getY());
	}

	@Override
	public boolean isTranslateable() {
		return true;
	}

	@Override
	public void rotate(NumberValue r) {
		angle -= r.getDouble();
	}

	@Override
	public void rotate(NumberValue r, GeoPointND S) {
		angle -= r.getDouble();
		rotate(location, r, S);
	}

	protected static void rotate(GPoint2D location, NumberValue r, GeoPointND S) {
		double phi = r.getDouble();
		double cos = MyMath.cos(phi);
		double sin = Math.sin(phi);
		double qx = S.getInhomCoords().getX();
		double qy = S.getInhomCoords().getY();

		double x = location.getX();
		double y = location.getY();

		location.setLocation((x - qx) * cos + (qy - y) * sin + qx,
				(x - qx) * sin + (y - qy) * cos + qy);
	}

	/**
	 * returns all class-specific xml tags for getXML
	 */
	@Override
	protected void getXMLtags(StringBuilder sb) {
		getXMLfixedTag(sb);
		getXMLvisualTags(sb);

		sb.append("\t<content val=\"");
		StringUtil.encodeXML(sb, getContent());
		sb.append("\"/>\n");

		sb.append("\t<contentSize width=\"");
		sb.append(contentWidth);
		sb.append("\" height=\"");
		sb.append(contentHeight);
		sb.append("\"/>\n");

		XMLBuilder.appendPosition(sb, this);
	}

	/**
	 * Zooming in x direction
	 *
	 * @param factor
	 *            zoom factor;
	 *
	 */
	private void zoomX(double factor) {
		width *= factor;
	}

	/**
	 * Zooming in y direction
	 *
	 * @param factor
	 *            zoom factor;
	 *
	 */
	private void zoomY(double factor) {
		height *= factor;
	}

	/**
	 * Zooms the text element
	 */
	public void zoomIfNeeded() {
		if (xScale == 0) {
			xScale = app.getActiveEuclidianView().getXscale();
			yScale = app.getActiveEuclidianView().getYscale();
			return;
		}

		if (xScale != app.getActiveEuclidianView().getXscale()) {
			zoomX(app.getActiveEuclidianView().getXscale() / xScale);
			xScale = app.getActiveEuclidianView().getXscale();
		}
		if (yScale != app.getActiveEuclidianView().getYscale()) {
			zoomY(app.getActiveEuclidianView().getYscale() / yScale);
			yScale = app.getActiveEuclidianView().getYscale();
		}
	}

	public double getContentWidth() {
		return contentWidth;
	}

	public void setContentWidth(double contentWidth) {
		this.contentWidth = contentWidth;
	}

	public double getContentHeight() {
		return contentHeight;
	}

	public void setContentHeight(double contentHeight) {
		this.contentHeight = contentHeight;
	}

	public void setWidth(double width) {
		this.width = width;
	}

	public void setHeight(double height) {
		this.height = height;
	}

	public boolean isZoomingEnabled() {
		return zoomingEnabled;
	}

	public void setZoomingEnabled(boolean zoomingEnabled) {
		this.zoomingEnabled = zoomingEnabled;
	}
}
