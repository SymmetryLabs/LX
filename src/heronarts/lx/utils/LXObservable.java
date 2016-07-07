package heronarts.lx.utils;

public interface LXObservable<T> {

  public void addObserver(LXObserver<T> observer);
  public void removeObserver(LXObserver<T> observer);

}
