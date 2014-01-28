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

package heronarts.lx.ui.control;

import heronarts.lx.LXDeck;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.ui.UI;
import heronarts.lx.ui.UIWindow;
import heronarts.lx.ui.component.UIKnob;
import heronarts.lx.ui.component.UIScrollList;

import java.util.ArrayList;
import java.util.List;

public class UIPatternDeck extends UIWindow {

	private final LXDeck deck;

	private final static int NUM_KNOBS = 12;
	private final static int KNOBS_PER_ROW = 4;

	public UIPatternDeck(UI ui, LXDeck deck, String label, float x, float y, float w, float h) {
		super(ui, label, x, y, w, h);
		this.deck = deck;
		int yp = TITLE_LABEL_HEIGHT;

		List<UIScrollList.Item> items = new ArrayList<UIScrollList.Item>();
		for (LXPattern p : deck.getPatterns()) {
			items.add(new PatternScrollItem(p));
		}
		final UIScrollList patternList = new UIScrollList(1, yp, w - 2, 140).setItems(items);
		patternList.addToContainer(this);
		yp += patternList.getHeight() + 10;

		final UIKnob[] knobs = new UIKnob[NUM_KNOBS];
		for (int ki = 0; ki < knobs.length; ++ki) {
			knobs[ki] = new UIKnob(5 + 34 * (ki % KNOBS_PER_ROW), yp
					+ (ki / KNOBS_PER_ROW) * 48);
			knobs[ki].addToContainer(this);
		}

		LXDeck.Listener lxListener = new LXDeck.AbstractListener() {
			public void patternWillChange(LXDeck deck, LXPattern pattern,
					LXPattern nextPattern) {
				patternList.redraw();
			}

			public void patternDidChange(LXDeck deck, LXPattern pattern) {
				patternList.redraw();
				int pi = 0;
				for (LXParameter parameter : pattern.getParameters()) {
					if (pi >= knobs.length) {
						break;
					}
					if (parameter instanceof LXListenableNormalizedParameter) {
						knobs[pi++].setParameter((LXListenableNormalizedParameter)parameter);
					}
				}
				while (pi < knobs.length) {
					knobs[pi++].setParameter(null);
				}
			}
		};

		deck.addListener(lxListener);
		lxListener.patternDidChange(deck, deck.getActivePattern());
	}

	private class PatternScrollItem extends UIScrollList.AbstractItem {

		private LXPattern pattern;

		private String label;

		PatternScrollItem(LXPattern pattern) {
			this.pattern = pattern;
			this.label = UI.uiClassName(pattern, "Pattern");
		}

		public String getLabel() {
			return this.label;
		}

		public boolean isSelected() {
			return deck.getActivePattern() == this.pattern;
		}

		public boolean isPending() {
			return deck.getNextPattern() == this.pattern;
		}

		public void onMousePressed() {
			deck.goPattern(this.pattern);
		}
	}
}
