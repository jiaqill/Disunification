package de.tudresden.inf.lat.uel.rule;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.tudresden.inf.lat.uel.type.api.Atom;

/**
 * This is a class representing a subsumption or dissubsumption between a conjunction of flat
 * atoms (body) and a flat atom or a list of flat atoms (head).
 * 
 * @author Stefan Borgwardt
 */
public class FlatConstraint {

	private final List<Atom> body;
	private final Atom head;
	private final List<Atom> dissubsumptionHead;
	private final boolean isDissubsumption;
	private boolean solved;
	private final int hashCode;

	/**
	 * Construct a new subsumption from the given atoms.
	 * 
	 * @param body
	 *            the body of the new subsumption
	 * @param head
	 *            the head of the new subsumption
	 */
	public FlatConstraint(List<Atom> body, Atom head, boolean isDissubsumption) {
		if ((body == null) || (head == null)) {
			throw new IllegalArgumentException("Body and head cannot be null.");
		}
		this.body = body;
		if (!isDissubsumption) {
			this.head = head;
			this.dissubsumptionHead = null;
			this.isDissubsumption = false;
		}
		else {
			//throw new IllegalArgumentException("This is a dissubsumption.");
			this.head = null;
			this.dissubsumptionHead = Arrays.asList(new Atom[] { head });
			this.isDissubsumption = true;
		}
		this.solved = false;
		this.hashCode = body.hashCode() * 31 + head.hashCode();
	}

	/**
	 * Construct a new dissubsumption from the given atoms.
	 *
	 * @param body
	 *            the body of the new dissubsumption
	 * @param head
	 *            the head of the new dissubsumption
	 */
	public FlatConstraint(List<Atom> body, List<Atom> head, boolean isDissubsumption) {
		if ((body == null) || (head == null)) {
			throw new IllegalArgumentException("Body and head cannot be null.");
		}
		this.body = body;
		if (isDissubsumption) {
			this.head = null;
			this.dissubsumptionHead = head;
			this.isDissubsumption = true;
		}
		else {
			throw new IllegalArgumentException("This is a subsumption.");
		}
		this.solved = false;
		this.hashCode = body.hashCode() * 31 + head.hashCode();
	}

	/**
	 * Construct a new subsumption with a single-atom body.
	 * 
	 * @param body
	 *            the body of the new subsumption
	 * @param head
	 *            the head of the new subsumption
	 */
	public FlatConstraint(Atom body, Atom head, boolean isDissubsumption) {
		if ((body == null) || (head == null)) {
			throw new IllegalArgumentException("Body and head cannot be null.");
		}
		this.body = Arrays.asList(new Atom[] { body });
		if (!isDissubsumption) {
			this.head = head;
			this.dissubsumptionHead = null;
			this.isDissubsumption = false;
		}
		else {
			//throw new IllegalArgumentException("This is a dissubsumption.");
			this.head = null;
			this.dissubsumptionHead = Arrays.asList(new Atom[] { head });
			this.isDissubsumption = true;
		}
		this.solved = false;
		this.hashCode = body.hashCode() * 31 + head.hashCode();
	}

	/**
	 * Construct a new dissubsumption with a single-atom body.
	 *
	 * @param body
	 *            the body of the new dissubsumption
	 * @param head
	 *            the head of the new dissubsumption
	 */
	public FlatConstraint(Atom body, List<Atom> head, boolean isDissubsumption) {
		if ((body == null) || (head == null)) {
			throw new IllegalArgumentException("Body and head cannot be null.");
		}
		this.body = Arrays.asList(new Atom[] { body });
		if (isDissubsumption) {
			this.head = null;
			this.dissubsumptionHead = head;
			this.isDissubsumption = true;
		}
		else {
			throw new IllegalArgumentException("This is a subsumption.");
		}
		this.solved = false;
		this.hashCode = body.hashCode() * 31 + head.hashCode();
	}

	/**
	 * Retrieve the body of this subsumption or dissubsumption.
	 * 
	 * @return a list containing the atoms of the body of this subsumption or dissubsumption
	 */
	public List<Atom> getBody() {
		return body;
	}

	/**
	 * Retrieve the head of this subsumption.
	 * 
	 * @return the atom that is the head of this subsumption
	 */
	public Atom getHead() {
		if (isDissubsumption) {
			throw new IllegalStateException("This is a dissubsumption.");
		}
		return head;
	}

	/**
	 * Retrieve the head of this dissubsumption.
	 *
	 * @return the atom that is the head of this dissubsumption
	 */
	public List<Atom> getDissubsumptionHead() {
		if(!isDissubsumption){
			throw new IllegalStateException("This is a subsumption.");
		}
		return dissubsumptionHead;
	}

	/**
	 * Check whether this is a dissubsumption.
	 *
	 * @return true if this is a dissubsumption, false if it is a subsumption
	 */
	public boolean isDissubsumption() {
		return isDissubsumption;
	}

	/**
	 * Check whether this subsumption or dissubsumption is already solved.
	 * 
	 * @return true iff this subsumption or dissubsumption is solved
	 */
	boolean isSolved() {
		return solved;
	}

	/**
	 * Set the 'solved' status of this subsumption or dissubsumption.
	 * 
	 * @param solved
	 *            a flag indicating whether this subsumption or dissubsumption is solved
	 */
	void setSolved(boolean solved) {
		this.solved = solved;
	}

	/**
	 * Check whether this subsumption or dissubsumption is ground.
	 * 
	 * @return true iff body and head are both ground
	 */
	public boolean isGround() {
		for (Atom at : body) {
			if (!at.isGround())
				return false;
		}
		if(isDissubsumption) {
			for (Atom at : dissubsumptionHead) {
				if (!at.isGround())
					return false;
			}
		}
		else {
			if (!head.isGround())
				return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof FlatConstraint))
			return false;

		FlatConstraint other = (FlatConstraint) obj;


		if (!body.containsAll(other.body))
			return false;
		if (!other.body.containsAll(body))
			return false;

		if (isDissubsumption && other.isDissubsumption) {
			if (!dissubsumptionHead.containsAll(other.dissubsumptionHead))
				return false;
			if (!other.dissubsumptionHead.containsAll(dissubsumptionHead))
				return false;
		}
		else if (!isDissubsumption && !other.isDissubsumption){
			if (!head.equals(other.head))
				return false;
		}
		return isDissubsumption == other.isDissubsumption;
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		if (body.isEmpty()) {
			buf.append("top");
		} else {
			Iterator<Atom> iter = body.iterator();
			buf.append(iter.next());
			while (iter.hasNext()) {
				buf.append(",");
				buf.append(iter.next());
			}
		}
		buf.append(isDissubsumption ? " ⋢ " : " < ");
		if (isDissubsumption) {
			Iterator<Atom> iter = dissubsumptionHead.iterator();
			buf.append(iter.next());
			while (iter.hasNext()) {
				buf.append(",");
				buf.append(iter.next());
			}
		} else {
			buf.append(head);
		}
		if (solved) {
			buf.append("[s]");
		}
		return buf.toString();

	}

}
