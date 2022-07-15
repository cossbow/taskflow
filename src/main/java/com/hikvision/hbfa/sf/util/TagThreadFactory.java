package com.hikvision.hbfa.sf.util;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class TagThreadFactory implements ThreadFactory {

    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;
    private final boolean daemon;


    public TagThreadFactory(String tag) {
        this(tag, false);
    }

    public TagThreadFactory(String tag, boolean daemon) {
        Objects.requireNonNull(tag);

        var s = System.getSecurityManager();

        group = (s != null) ? s.getThreadGroup() :
                Thread.currentThread().getThreadGroup();
        if (tag.charAt(tag.length() - 1) != '-') tag += '-';
        namePrefix = tag;

        this.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable r) {
        var t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement());
        if (t.isDaemon() != daemon)
            t.setDaemon(daemon);
        if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }

}
