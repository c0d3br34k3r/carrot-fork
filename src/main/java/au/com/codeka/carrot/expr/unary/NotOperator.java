package au.com.codeka.carrot.expr.unary;

import au.com.codeka.carrot.CarrotException;
import au.com.codeka.carrot.ValueHelper;
import au.com.codeka.carrot.expr.TokenType;

/**
 * The unary "Not" operator like in {@code ! value}.
 *
 * @author Marten Gajda
 */
public final class NotOperator implements UnaryOperator {
	@Override
	public Object apply(Object value) throws CarrotException {
		return !ValueHelper.isTrue(value);
	}

	@Override
	public String toString() {
		return TokenType.NOT.toString();
	}
}
