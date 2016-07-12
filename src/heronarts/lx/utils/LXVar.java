package heronarts.lx.utils;

public class LXVar<T> extends LXAbstractObservable<T> {

  private T value;

  public LXVar() {
  }

  public LXVar(T value) {
    this.value = value;
  }

  public void addObserverWithInit(LXObserver<T> observer) {
    addObserver(observer);
    notifyDidChange(value);
  }

  public T get() {
    return value;
  }

  public void set(T value) {
    if (this.value != value) {
      notifyWillChange(this.value, value);
      this.value = value;
      notifyDidChange(value);
    }
  }

}
