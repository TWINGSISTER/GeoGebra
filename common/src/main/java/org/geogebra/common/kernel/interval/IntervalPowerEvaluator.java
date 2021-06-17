package org.geogebra.common.kernel.interval;

import static org.geogebra.common.kernel.interval.IntervalConstants.undefined;

import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.arithmetic.ExpressionNode;
import org.geogebra.common.kernel.arithmetic.ExpressionValue;
import org.geogebra.common.kernel.arithmetic.MinusOne;
import org.geogebra.common.kernel.arithmetic.MyDouble;
import org.geogebra.common.plugin.Operation;

/**
 * Class to evaluate expressions on an interval that has power in it.
 */
public class IntervalPowerEvaluator {
	private final ExpressionNode node;

	/**
	 *
	 * @param node expression to evaluate.
	 */
	public IntervalPowerEvaluator(ExpressionNode node) {
		this.node = node;
	}

	/**
	 *
	 * @return if this class can handle the expression.
	 */
	public boolean isAccepted() {
		return node.getOperation().equals(Operation.POWER);
	}

	/**
	 *
	 * @param x interval
	 * @return power expression evaluated on x.
	 */
	public Interval handle(Interval x) throws Exception {
		Interval leftEvaluated = IntervalFunction.evaluate(x, node.getLeft());
		ExpressionValue right = node.getRight();
		Interval rightEvaluated = IntervalFunction.evaluate(x, right);
		return handle(leftEvaluated, rightEvaluated, right);
	}

	private Interval handle(Interval base, Interval exponent, ExpressionValue right) {
		if (MyDouble.exactEqual(base.getLow(), Math.E)) {
			return exponent.exp();
		}

		if (base.isNegative() && right.isExpressionNode()) {
			try {
				Interval negPower = calculateNegPower(right.wrap(), base);
				if (!negPower.isUndefined()) {
					return negPower;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return base.pow(exponent);
	}

	private Interval calculateNegPower(ExpressionNode node, Interval base) throws Exception {
		if (isPositiveFraction(node)) {
			return negativePower(base, node);
		} else if (isNegativeFraction(node)) {
			return negativePower(base, node.getRight().wrap())
					.multiplicativeInverse();
		}

		return undefined();
	}

	private boolean isPositiveFraction(ExpressionNode node) {
		return node.isOperation(Operation.DIVIDE);
	}

	private boolean isNegativeFraction(ExpressionNode node) {
		return node.getOperation() == Operation.MULTIPLY
				&& node.getLeft() instanceof MinusOne
				&& node.getRight().isOperation(Operation.DIVIDE);
	}

	private Interval negativePower(Interval base, ExpressionNode node) throws Exception {
		Interval nominator = IntervalFunction.evaluate(base, node.getLeft());
		if (nominator.isSingletonInteger()) {
			Interval denominator = IntervalFunction.evaluate(base, node.getRight());
			if (denominator.isUndefined()) {
				return undefined();
			} else if (denominator.isSingletonInteger()) {
				return powerFraction(base, (long) nominator.getLow(),
						(long) denominator.getLow());

			}
		}
		return undefined();
	}

	private Interval powerFraction(Interval x, long a, long b) {
		long gcd = Kernel.gcd(a, b);
		if (gcd == 0) {
			return undefined();
		}

		long nominator = a / gcd;
		long denominator = b / gcd;
		Interval interval = new Interval(x);
		Interval base = nominator == 1
				? interval
				: interval.pow(nominator);

		if (base.isPositive()) {
			return base.pow(1d / denominator);
		}

		if (isOdd(denominator)) {
			return base.negative().pow(1d / denominator).negative();
		}

		return undefined();
	}

	private boolean isOdd(long value) {
		return (Math.abs(value) % 2) == 1;
	}
}
