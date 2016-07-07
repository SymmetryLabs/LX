package heronarts.lx.utils;

import java.util.ArrayList;
import java.util.List;

public class LXAbstractObservable<T> implements LXObservable<T> {

  private final List<LXObserver<T>> observers = new ArrayList<LXObserver<T>>();

  public void addObserver(LXObserver<T> observer) {
    this.observers.add(observer);
  }

  public void removeObserver(LXObserver<T> observer) {
    this.observers.remove(observer);
  }

  protected void notifyWillChange(T oldValue, T newValue) {
    for (LXObserver<T> observer : this.observers) {
      observer.valueWillChange(oldValue, newValue);
    }
  }

  protected void notifyDidChange(T value) {
    for (LXObserver<T> observer : this.observers) {
      observer.valueDidChange(value);
    }
  }

}
