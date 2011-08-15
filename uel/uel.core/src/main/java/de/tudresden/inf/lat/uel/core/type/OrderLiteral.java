package de.tudresden.inf.lat.uel.core.type;

/**
 * 
 * @author Barbara Morawska
 */
public class OrderLiteral implements Literal {

	private final String first;
	private int hashCode = 0;
	private final String second;
	private boolean value = false;

	/**
	 * Constructs an order literal given two names.
	 * 
	 * @param one
	 *            first component
	 * @param two
	 *            second component
	 */
	public OrderLiteral(String one, String two) {
		first = one;
		second = two;
		hashCode = one.hashCode() + 31 * two.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		boolean ret = false;
		if (o instanceof OrderLiteral) {
			OrderLiteral other = (OrderLiteral) o;
			ret = this.value == other.value && this.first.equals(other.first)
					&& this.second.equals(other.second);
		}
		return ret;
	}

	@Override
	public String getFirst() {
		return first;
	}

	@Override
	public String getSecond() {
		return second;
	}

	@Override
	public boolean getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean isOrder() {
		return true;
	}

	@Override
	public boolean isSubsumption() {
		return false;
	}

	@Override
	public void setValue(boolean t) {
		value = t;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder("(");
		str.append(first.toString());
		str.append(">");
		str.append(second.toString());
		str.append(")");
		return str.toString();
	}

}
