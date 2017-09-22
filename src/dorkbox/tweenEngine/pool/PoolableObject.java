package dorkbox.tweenEngine.pool;

/**
 *
 */
public
interface PoolableObject<T> {
    void onReturn(final T object);
    T create();
}
