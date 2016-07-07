package heronarts.lx.utils;

public interface LXObserver<T> {

  public void valueWillChange(T oldValue, T newValue);
  public void valueDidChange(T value);

}
