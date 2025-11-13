package oj.oj_codesandbox.model;

import oj.oj_codesandbox.judge.entity.JudgeInfo;
import lombok.Data;

import java.io.Serializable;

/**
 * 判题结果消息
 */
@Data
public class JudgeResultMessage implements Serializable {
    
    private String commitId;
    private String problemId;
    private String userId;
    private JudgeInfo judgeInfo;
    private ExecuteCodeResponse executeCodeResponse;
    private long processTime;
    private boolean success;
    private String errorMessage;
    
    public JudgeResultMessage() {}
    
    public JudgeResultMessage(String commitId, String problemId, String userId) {
        this.commitId = commitId;
        this.problemId = problemId;
        this.userId = userId;
        this.success = false;
    }
    
    public static JudgeResultMessage success(String commitId, String problemId, String userId, 
                                          JudgeInfo judgeInfo, ExecuteCodeResponse executeCodeResponse, 
                                          long processTime) {
        JudgeResultMessage message = new JudgeResultMessage(commitId, problemId, userId);
        message.setSuccess(true);
        message.setJudgeInfo(judgeInfo);
        message.setExecuteCodeResponse(executeCodeResponse);
        message.setProcessTime(processTime);
        return message;
    }
    
    public static JudgeResultMessage error(String commitId, String problemId, String userId, 
                                        String errorMessage, long processTime) {
        JudgeResultMessage message = new JudgeResultMessage(commitId, problemId, userId);
        message.setSuccess(false);
        message.setErrorMessage(errorMessage);
        message.setProcessTime(processTime);
        return message;
    }
}
