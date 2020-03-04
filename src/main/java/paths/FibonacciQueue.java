package paths;

import java.util.AbstractQueue;
import java.util.Iterator;

public class FibonacciQueue<E> extends AbstractQueue<E> {

    @Override
    public Iterator<E> iterator() {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean offer(E e) {
        return false;
    }

    @Override
    public E poll() {
        return null;
    }

    @Override
    public E peek() {
        return null;
    }


}
