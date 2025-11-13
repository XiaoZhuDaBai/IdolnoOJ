package oj.oj_codesandbox.judge.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class JudgeInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 程序执行信息
     */
    private String cnMessage;
    private String enMessage;
    /**
     * 消耗内存(KB)
     */
    private Long memory;
    /**
     * 消耗时间(ms)
     */
    private Long time;

    private boolean[] correct;
    private boolean outCountEqAnsCount;
    List<String> errorMessages;
    private long exitCode;
}
