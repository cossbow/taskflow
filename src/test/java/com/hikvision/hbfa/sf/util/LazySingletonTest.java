package com.hikvision.hbfa.sf.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class LazySingletonTest {

    @Test
    public void testCreateOnce() {
        var s = LazySingleton.of(TestConnection::new);
        Assertions.assertSame(s.get(), s.get());
    }

    @Test
    public void testRemove() {
        var s = LazySingleton.of(TestConnection::new);
        var c1 = s.get();
        Assertions.assertSame(c1, s.remove());
        Assertions.assertNotSame(c1, s.get());
    }

    @Test
    public void testOnCreate() {
        var r = new AtomicInteger(0);
        var s = LazySingleton.of(TestConnection::new, c -> {
            r.incrementAndGet();
        });
        Assertions.assertEquals(0, r.get());
        s.get();
        Assertions.assertEquals(1, r.get());
        s.remove();
        s.get();
        Assertions.assertEquals(2, r.get());

    }

    @Test
    public void testValid() {
        var r = new AtomicInteger(0);
        var s = LazySingleton.of(TestConnection::new, TestConnection::isOpen, c -> {
            r.incrementAndGet();
        });
        var c = s.get();
        c.setOpen(false);
        Assertions.assertNotSame(c, s.get());
        Assertions.assertEquals(2, r.get());
    }


    private static class TestConnection {
        private volatile boolean open = true;

        public void setOpen(boolean open) {
            this.open = open;
        }

        public boolean isOpen() {
            return open;
        }
    }
}
