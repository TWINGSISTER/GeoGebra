package org.geogebra.common.kernel.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.arithmetic.Command;
import org.geogebra.common.kernel.arithmetic.variable.Variable;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.desktop.headless.AppDNoGui;
import org.hamcrest.core.StringStartsWith;
import org.junit.BeforeClass;
import org.junit.Test;

public class SymbolicProcessorTest {

	private static AppDNoGui app;
	private static SymbolicProcessor processor;
	private static Kernel kernel;

	@BeforeClass
	public static void setup() {
		app = AlgebraTest.createApp();
		kernel = app.getKernel();
		processor = new SymbolicProcessor(kernel);
	}

	@Test
	public void symbolicExpressionTest() {
		Variable a = new Variable(kernel, "a");
		GeoElement aPlusA = processor.evalSymbolicNoLabel(a.wrap().plus(a));
		assertEquals("2 * a",
				aPlusA.toValueString(StringTemplate.testTemplate));
	}

	@Test
	public void symbolicCommandTest() {
		Variable a = new Variable(kernel, "a");
		Command integral = new Command(kernel, "Integral", false);
		integral.addArgument(a.wrap().multiply(a));
		GeoElement integralASquared = processor.evalSymbolicNoLabel(integral);
		// the arbitrary constant index may change, use regexp
		assertThat(
				integralASquared.toValueString(StringTemplate.testTemplate),
				StringStartsWith.startsWith("1 / 3 * a^(3) + c_"));
	}
}
