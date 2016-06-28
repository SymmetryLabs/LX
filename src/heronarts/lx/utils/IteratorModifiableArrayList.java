package heronarts.lx.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

/**
 * An ArrayList that can be modified during a for-each loop.
 * @author Kyle Fleming
 */
public class IteratorModifiableArrayList<E> implements Iterable<E>, Collection<E>, List<E>, RandomAccess {

  private List<E> innerList;
  private boolean isDirty = true;

  public IteratorModifiableArrayList(int initialCapacity) {
    innerList = new ArrayList<E>(initialCapacity);
  }

  public IteratorModifiableArrayList() {
    innerList = new ArrayList<E>();
  }

  public IteratorModifiableArrayList(Collection<? extends E> c) {
    innerList = new ArrayList<E>(c);
  }

  private void setDirty(boolean isDirty) {
    if (!this.isDirty && isDirty) {
      innerList = new ArrayList<E>(innerList);
    }
    this.isDirty = isDirty;
  }

  @Override
  public Iterator<E> iterator() {
    setDirty(false);
    return innerList.iterator();
  }

  @Override
  public int size() {
    return innerList.size();
  }

  @Override
  public boolean isEmpty() {
    return innerList.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return innerList.contains(o);
  }

  @Override
  public Object[] toArray() {
    return innerList.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return innerList.toArray(a);
  }

  @Override
  public boolean add(E e) {
    setDirty(true);
    return innerList.add(e);
  }

  @Override
  public boolean remove(Object o) {
    setDirty(true);
    return innerList.remove(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return innerList.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    setDirty(true);
    return innerList.addAll(c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    setDirty(true);
    return innerList.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    setDirty(true);
    return innerList.retainAll(c);
  }

  @Override
  public void clear() {
    setDirty(true);
    innerList.clear();
  }

  @Override
  public boolean addAll(int index, Collection<? extends E> c) {
    setDirty(true);
    return innerList.addAll(index, c);
  }

  @Override
  public E get(int index) {
    return innerList.get(index);
  }

  @Override
  public E set(int index, E element) {
    setDirty(true);
    return innerList.set(index, element);
  }

  @Override
  public void add(int index, E element) {
    setDirty(true);
    innerList.add(index, element);
  }

  @Override
  public E remove(int index) {
    setDirty(true);
    return innerList.remove(index);
  }

  @Override
  public int indexOf(Object o) {
    return innerList.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return innerList.lastIndexOf(o);
  }

  @Override
  public ListIterator<E> listIterator() {
    setDirty(false);
    return innerList.listIterator();
  }

  @Override
  public ListIterator<E> listIterator(int index) {
    setDirty(false);
    return innerList.listIterator(index);
  }

  @Override
  public List<E> subList(int fromIndex, int toIndex) {
    return innerList.subList(fromIndex, toIndex);
  }

}
