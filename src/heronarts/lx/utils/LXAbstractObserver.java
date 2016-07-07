package heronarts.lx.utils;

public abstract class LXAbstractObserver<T> implements LXObserver<T> {

  @Override
  public void valueWillChange(T oldValue, T newValue) {
  }

  @Override
  public void valueDidChange(T value) {
  }

}
