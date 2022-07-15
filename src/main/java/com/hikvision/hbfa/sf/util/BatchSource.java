package com.hikvision.hbfa.sf.util;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class BatchSource<T> implements Iterable<T> {

    /**
     * 负责加载数据
     */
    private final LongFunction<List<T>> loader;
    /**
     * 根据最后一条数据获取游标，指示下一页
     */
    private final ToLongFunction<T> cursorMapper;
    /**
     * 游标初始值
     */
    private final long cursorStart;

    public BatchSource(LongFunction<List<T>> loader,
                       ToLongFunction<T> cursorMapper,
                       long cursorStart) {
        this.loader = loader;
        this.cursorMapper = cursorMapper;
        this.cursorStart = cursorStart;
    }

    @Override
    public Iterator<T> iterator() {
        return new LoadingIterator(cursorStart);
    }

    private class LoadingIterator implements Iterator<T> {
        private long cursor;
        private List<T> list;
        private int size;
        private int index;

        public LoadingIterator(long cursor) {
            this.cursor = cursor;
        }

        @Override
        public boolean hasNext() {
            if (index == size) {
                loading();
            }

            return index < size;
        }

        @Override
        public T next() {
            if (size <= 0 || index == size) {
                throw new NoSuchElementException();
            }
            return list.get(index++);
        }

        private void loading() {
            list = loader.apply(cursor);
            size = list.size();
            if (size > 0) {
                cursor = cursorMapper.applyAsLong(list.get(size - 1));
            }
            index = 0;
        }

    }


    public Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

}
