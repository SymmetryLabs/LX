/**
 * ##library.name##
 * ##library.sentence##
 * ##library.url##
 *
 * Copyright ##copyright## ##author##
 * All Rights Reserved
 * 
 * @author      ##author##
 * @modified    ##date##
 * @version     ##library.prettyVersion## (##library.version##)
 */

package heronarts.lx;

import heronarts.lx.control.BasicParameter;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.transition.DissolveTransition;
import heronarts.lx.transition.LXTransition;

import java.util.ArrayList;
import java.util.List;

/**
 * A deck is a single component of the engine that has a set of patterns
 * from which it plays and rotates. It also has a fader to control the degree
 * to which this deck is blended with previous decks.
 */
public class LXDeck {
	
	/**
	 * Listener interface for objects which want to be notified when the
	 * internal engine state is modified
	 */
	public interface Listener {
		public void patternWillChange(LXDeck deck, LXPattern pattern, LXPattern nextPattern);
		public void patternDidChange(LXDeck deck, LXPattern pattern);
		public void faderTransitionDidChange(LXDeck deck, LXTransition faderTransition);
	}
	
	/**
	 * Utility class to extend in cases where only some methods need overriding. 
	 */
	public abstract static class AbstractListener implements Listener {
		public void patternWillChange(LXDeck deck, LXPattern pattern, LXPattern nextPattern) {}
		public void patternDidChange(LXDeck deck, LXPattern pattern) {}
		public void faderTransitionDidChange(LXDeck deck, LXTransition faderTransition) {}
	}
	
	private final HeronLX lx;
	public final int index;
	
	
	private LXPattern[] patterns;
	private int activePatternIndex = 0;
	private int nextPatternIndex = 0;
	
	private boolean autoTransitionEnabled = false;
	private int autoTransitionThreshold = 0;
	
	LXTransition faderTransition = null;
	final BasicParameter fader = new BasicParameter("FADER", 0);
	
	private LXTransition transition = null;
	private long transitionMillis = 0;

	private final List<Listener> listeners = new ArrayList<Listener>();
	
	LXDeck(HeronLX lx, int index, LXPattern[] patterns) {
		this.lx = lx;
		this.index = index;
		this.patterns = patterns;
		this.faderTransition = new DissolveTransition(lx);  
		this.transitionMillis = System.currentTimeMillis();
	}
	
	public final void addListener(Listener listener) {
		synchronized(listeners) {
			listeners.add(listener);
		}
	}
	
	public final void removeListener(Listener listener) {
		synchronized(listeners) {
			listeners.remove(listener);
		}
	}
	
	protected final void notifyPatternWillChange(LXPattern pattern, LXPattern nextPattern) {
		synchronized(listeners) {
			for (Listener listener : listeners) {
				listener.patternWillChange(this, pattern, nextPattern);
			}
		}
	}
	
	protected final void notifyPatternDidChange(LXPattern pattern) {
		synchronized(listeners) {
			for (Listener listener : listeners) {
				listener.patternDidChange(this, pattern);
			}
		}
	}

	protected final void notifyFaderTransitionDidChange(LXTransition transition) {
		synchronized(listeners) {
			for (Listener listener : listeners) {
				listener.faderTransitionDidChange(this, transition);
			}
		}
	}
	
	public final BasicParameter getFader() {
		return this.fader;
	}
	
	public synchronized final LXPattern[] getPatterns() {
		return this.patterns;
	}
	
	public synchronized final LXTransition getFaderTransition() {
		return this.faderTransition;
	}
	
	public synchronized final LXDeck setFaderTransition(LXTransition transition) {
		if (this.faderTransition != transition) {
			this.faderTransition = transition;
			notifyFaderTransitionDidChange(transition);
		}
		return this;
	}
	
	public synchronized final LXDeck setPatterns(LXPattern[] patterns) {
		this.getActivePattern().didResignActive();		
		this.patterns = patterns;
		this.activePatternIndex = this.nextPatternIndex = 0;
		this.getActivePattern().willBecomeActive();
		return this;
	}

	public synchronized final LXPattern getActivePattern() {
		return this.patterns[this.activePatternIndex];
	}

	public synchronized final LXPattern getNextPattern() {
		return this.patterns[this.nextPatternIndex];
	}
	
	protected synchronized final LXTransition getActiveTransition() {
		return this.transition;
	}

	public synchronized final void goPrev() {
		if (this.transition != null) {
			return;
		}
		this.nextPatternIndex = this.activePatternIndex - 1;
		if (this.nextPatternIndex < 0) {
			this.nextPatternIndex = this.patterns.length - 1;
		}
		this.startTransition();
	}
	
	public synchronized final void goNext() {
		if (this.transition != null) {
			return;
		}
		this.nextPatternIndex = this.activePatternIndex;
		do {
			this.nextPatternIndex = (this.nextPatternIndex + 1) % this.patterns.length;
		} while ((this.nextPatternIndex != this.activePatternIndex) &&
				 !this.getNextPattern().isEligible());
		if (this.nextPatternIndex != this.activePatternIndex) {
			this.startTransition();
		}
	}

	public synchronized final void goPattern(LXPattern pattern) {
		for (int i = 0; i < this.patterns.length; ++i) {
			if (this.patterns[i] == pattern) {
				this.goIndex(i);
				return;
			}
		}
	}	
	
	public synchronized final void goIndex(int i) {
		if (this.transition != null) {
			return;
		}
		if (i < 0 || i >= this.patterns.length) {
			return;
		}
		this.nextPatternIndex = i;
		this.startTransition();
	}
	
	protected synchronized void disableAutoTransition() {
		this.autoTransitionEnabled = false;
	}
	
	protected synchronized void enableAutoTransition(int autoTransitionThreshold) {
		this.autoTransitionEnabled = true;
		this.autoTransitionThreshold = autoTransitionThreshold;
		if (this.transition == null) {
			this.transitionMillis = System.currentTimeMillis(); 
		}
	}
	
	protected synchronized boolean isAutoTransitionEnabled() {
		return this.autoTransitionEnabled;
	}

		
	private synchronized void startTransition() {
		if (getActivePattern() == getNextPattern()) {
			return;
		}
		getNextPattern().willBecomeActive();
		notifyPatternWillChange(getActivePattern(), getNextPattern());
		this.transition = getNextPattern().getTransition();
		if (this.transition == null) {
			finishTransition();
		} else {
			getNextPattern().onTransitionStart();
			this.transitionMillis = System.currentTimeMillis();
		}
	}
	
	private synchronized void finishTransition() {
		getActivePattern().didResignActive();		
		this.activePatternIndex = this.nextPatternIndex;
		if (this.transition != null) {
			getActivePattern().onTransitionEnd();
		}
		this.transition = null;
		this.transitionMillis = System.currentTimeMillis();
		notifyPatternDidChange(getActivePattern());
	}
	
	synchronized void run(long nowMillis, double deltaMs) {
		// Run active pattern
		this.getActivePattern().go(deltaMs);
		
		// Run transition if applicable
		if (this.transition != null) {
			int transitionMs = (int) (nowMillis - this.transitionMillis);
			if (transitionMs >= this.transition.getDuration()) {
				this.finishTransition();
			} else {
				this.getNextPattern().go(deltaMs);
				this.transition.blend(
						this.getActivePattern().getColors(),
						this.getNextPattern().getColors(),
						(double) transitionMs / this.transition.getDuration()
						);
			}
		} else {
			if (this.autoTransitionEnabled &&
				(nowMillis - this.transitionMillis > this.autoTransitionThreshold) &&
				!this.getActivePattern().isInInterval()) {
				this.goNext();
			}
		}
	}

	public synchronized int[] getColors() {
		return (this.transition != null) ? this.transition.getColors() : this.getActivePattern().getColors();
	}
}
