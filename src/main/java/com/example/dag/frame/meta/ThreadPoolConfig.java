package com.example.dag.frame.meta;

import lombok.Data;

@Data
public class ThreadPoolConfig {
    private int coreSize;
    private int MaxSize;
    private int queueSize;
}
