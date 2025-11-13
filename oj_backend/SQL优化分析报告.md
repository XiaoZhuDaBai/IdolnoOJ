# SQL 优化分析报告

## 一、CommitCaseMapper.xml

### 1. `getAllCommitCaseByPage` (第22-25行)
**问题：**
- 使用 `SELECT *` 会查询所有字段，包括大字段（LONGVARCHAR 类型的 `cn_name`, `english_name`）
- 分页参数使用 `limit #{page},#{size}`，当 page 很大时性能会下降（OFFSET 越大越慢）

**优化建议：**
```sql
-- 使用明确的列名，避免查询大字段
SELECT commit_id, input, output, time, memory
FROM commit_case
ORDER BY commit_id DESC  -- 添加排序，使用游标分页
LIMIT #{size} OFFSET #{page}
```

**进一步优化（游标分页）：**
```sql
-- 如果使用游标分页，性能会更好
SELECT commit_id, input, output, time, memory
FROM commit_case
WHERE commit_id > #{lastCommitId}
ORDER BY commit_id ASC
LIMIT #{size}
```

### 2. `getUserCommitCase` (第28-30行)
**问题：**
- SQL 为空，需要实现

---

## 二、QuestionMapper.xml

### 1. `getQuestionsByTag` (第59-80行)
**问题：**
- 使用 `SELECT *` 查询所有字段，包括大字段（`question_description`, `question_examples` 等 LONGVARCHAR）
- LIKE 查询 `question_name LIKE CONCAT('%', #{resource}, '%')` 无法使用索引，会导致全表扫描（注释已提到）
- `question_tag LIKE CONCAT('%', #{tag}, '%')` 同样无法使用索引
- 缺少排序字段，结果顺序不确定

**优化建议：**
```xml
<select id="getQuestionsByTag" resultType="oj.oj_backend.model.Question">
    SELECT 
        question_id, question_name, question_type,
        add_time, author, difficulty,
        question_tag, time_limit, memory_limit,
        stack_limit, question_input_description,
        question_output_description, question_source,
        question_commits, ac, modify_time, status
    FROM question
    <where>
        <if test="platform != null and !platform.isEmpty()">
            AND question_source = #{platform}
        </if>
        <if test="difficulty != null and !difficulty.isEmpty()">
            AND difficulty = #{difficulty}
        </if>
        <!-- 优化：如果可能，使用全文索引或前缀匹配 -->
        <if test="resource != null and !resource.isEmpty()">
            AND question_name LIKE CONCAT(#{resource}, '%')  <!-- 前缀匹配可以使用索引 -->
        </if>
        <!-- 优化：考虑使用 FIND_IN_SET 或 JSON 字段存储标签 -->
        <if test="tags != null and tags.length > 0">
            AND (
            <foreach collection="tags" item="tag" separator=" OR ">
                question_tag LIKE CONCAT('%', #{tag}, '%')
            </foreach>
            )
        </if>
    </where>
    ORDER BY modify_time DESC  <!-- 添加排序 -->
    LIMIT #{page}, #{size}
</select>
```

**数据库层面优化建议：**
- 为 `question_source` 添加索引
- 为 `difficulty` 添加索引
- 为 `question_name` 添加前缀索引：`CREATE INDEX idx_question_name ON question(question_name(20))`
- 考虑使用全文索引（FULLTEXT）或标签表（多对多关系）替代 `question_tag` 的 LIKE 查询

### 2. `getQuestionsCountByTag` (第83-103行)
**问题：**
- 第93行使用 `question_name = #{resource}` 而 `getQuestionsByTag` 使用 `LIKE`，逻辑不一致
- 同样存在 LIKE 查询无法使用索引的问题

**优化建议：**
```xml
<select id="getQuestionsCountByTag" resultType="java.lang.Long">
    SELECT COUNT(*) FROM question
    <where>
        <if test="platform != null and !platform.isEmpty()">
            AND question_source = #{platform}
        </if>
        <if test="difficulty != null and !difficulty.isEmpty()">
            AND difficulty = #{difficulty}
        </if>
        <!-- 修复：与 getQuestionsByTag 保持一致 -->
        <if test="resource != null and !resource.isEmpty()">
            AND question_name LIKE CONCAT('%', #{resource}, '%')
        </if>
        <if test="tags != null and tags.length > 0">
            AND (
            <foreach collection="tags" item="tag" separator=" OR ">
                question_tag LIKE CONCAT('%', #{tag}, '%')
            </foreach>
            )
        </if>
    </where>
</select>
```

### 3. `updateCommitCountById` (第106-108行)
**优化建议：**
- 当前实现已经很好，使用原子操作 `question_commits = question_commits + 1`
- 建议添加 `question_id` 的唯一索引（如果还没有的话）

---

## 三、UserCommitMapper.xml

**问题：**
- 只有 resultMap 定义，没有实际查询语句
- 建议添加常用的 CRUD 操作

---

## 四、UserMapper.xml

### 1. `getMyRecentSubmission` (第50-63行)
**问题：**
- `INNER JOIN user u ON uc.uid = u.uuid` 中的 `u.uuid = #{uuid}` 条件可以提前到 JOIN 条件中
- `LEFT JOIN commit_case cc` 和 `LEFT JOIN question q` 在 WHERE 中已经过滤了用户，可以优化
- 缺少索引可能导致性能问题

**优化建议：**
```xml
<select id="getMyRecentSubmission" resultType="oj.oj_backend.model.response.MyRecentSubmissionResponse">
    SELECT
        q.question_name AS title,
        cc.cn_name,
        uc.create_time
    FROM user_commit uc
    LEFT JOIN commit_case cc ON uc.commit_id = cc.commit_id
    LEFT JOIN question q ON uc.qid = q.question_id
    WHERE uc.uid = #{uuid}  -- 直接使用 uc.uid，不需要 JOIN user 表
      AND uc.create_time >= DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY)
    ORDER BY uc.create_time DESC
    LIMIT #{page}, 5
</select>
```

**数据库索引建议：**
- `user_commit(uid, create_time)` 复合索引
- `user_commit(commit_id)` 索引
- `user_commit(qid)` 索引

### 2. `getMyRecentSubmissionCount` (第65-74行)
**问题：**
- 与 `getMyRecentSubmission` 一样，不需要 JOIN user 表
- 不需要 JOIN question 和 commit_case 表（COUNT 不需要这些字段）

**优化建议：**
```xml
<select id="getMyRecentSubmissionCount" resultType="java.lang.Integer">
    SELECT COUNT(uc.id)
    FROM user_commit uc
    WHERE uc.uid = #{uuid}
      AND uc.create_time >= DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY)
</select>
```

### 3. `getMyRecentWrongSubmission` (第76-98行)
**问题：**
- 复杂的子查询，性能可能较差
- 多个 LEFT JOIN 和子查询嵌套
- `HAVING` 子句在 GROUP BY 之后执行，效率较低

**优化建议：**
```xml
<select id="getMyRecentWrongSubmission" resultType="oj.oj_backend.model.response.MyRecentWrongSubmissionResponse">
    SELECT
        q.question_id AS problemId,
        q.question_name AS problemName,
        COUNT(DISTINCT uc.id) AS tryCount
    FROM user_commit uc
    INNER JOIN question q ON uc.qid = q.question_id
    LEFT JOIN commit_case cc ON uc.commit_id = cc.commit_id
    WHERE uc.uid = #{uuid}
      AND uc.create_time >= DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY)
      AND q.question_id IS NOT NULL
      -- 优化：使用 NOT EXISTS 替代子查询
      AND NOT EXISTS (
          SELECT 1
          FROM user_commit uc_pass
          INNER JOIN commit_case cc_pass ON uc_pass.commit_id = cc_pass.commit_id
          WHERE uc_pass.uid = #{uuid}
            AND uc_pass.qid = q.question_id
            AND cc_pass.cn_name = '通过'
            AND uc_pass.create_time >= DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY)
      )
      -- 优化：将 HAVING 条件移到 WHERE 中
      AND (cc.cn_name IS NULL OR cc.cn_name != '通过')
    GROUP BY q.question_id, q.question_name
    ORDER BY tryCount DESC
</select>
```

### 4. `getMySubmissionData` (第100-123行)
**问题：**
- 使用 UNION ALL 生成日期序列，可以优化
- `DATE(uc.create_time) = d.date_value` 无法使用索引

**优化建议：**
```xml
<select id="getMySubmissionData" resultType="oj.oj_backend.model.response.MySubmissionDataResponse">
    SELECT
        d.date_value AS time,
        IFNULL(COUNT(DISTINCT uc.id), 0) AS commitCount,
        IFNULL(COUNT(DISTINCT CASE WHEN cc.cn_name = '通过' THEN uc.id END), 0) AS acCount
    FROM (
        SELECT DATE_SUB(CURRENT_DATE(), INTERVAL 0 DAY) AS date_value UNION ALL
        SELECT DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY) UNION ALL
        SELECT DATE_SUB(CURRENT_DATE(), INTERVAL 2 DAY) UNION ALL
        SELECT DATE_SUB(CURRENT_DATE(), INTERVAL 3 DAY) UNION ALL
        SELECT DATE_SUB(CURRENT_DATE(), INTERVAL 4 DAY) UNION ALL
        SELECT DATE_SUB(CURRENT_DATE(), INTERVAL 5 DAY) UNION ALL
        SELECT DATE_SUB(CURRENT_DATE(), INTERVAL 6 DAY)
    ) d
    LEFT JOIN user_commit uc ON uc.uid = #{uuid}
        AND uc.create_time >= d.date_value
        AND uc.create_time < DATE_ADD(d.date_value, INTERVAL 1 DAY)  -- 优化：使用范围查询替代 DATE() 函数
    LEFT JOIN commit_case cc ON uc.commit_id = cc.commit_id
    GROUP BY d.date_value
    ORDER BY d.date_value DESC
</select>
```

### 5. `getCommits` 和 `getCommitsCount` (第125-190行)
**问题：**
- 两个查询的 WHERE 条件完全重复，可以提取为公共片段
- `ORDER BY uc.create_time DESC` 在 COUNT 查询中不需要
- LIKE 查询无法使用索引

**优化建议：**
```xml
<!-- 提取公共 WHERE 条件 -->
<sql id="Commits_Where_Clause">
    <where>
        <if test="problemName != null and problemName != ''">
            AND q.question_name LIKE CONCAT('%', #{problemName}, '%')
        </if>
        <if test="language != null and language != ''">
            AND uc.language = #{language}
        </if>
        <if test="status != null and status != ''">
            AND cc.cn_name = #{status}
        </if>
        <choose>
            <when test="userType == 'me'">
                AND uc.uid = #{uuid}
            </when>
            <when test="userType == 'others'">
                AND uc.uid != #{uuid}
            </when>
        </choose>
    </where>
</sql>

<select id="getCommits" resultType="oj.oj_backend.model.response.SubmissionResponse">
    SELECT
        q.question_name AS problemName,
        u.avatar AS avatar,
        u.nickname AS nickname,
        cc.cn_name AS commitCase,
        cc.time AS time,
        cc.memory AS memory,
        uc.create_time AS createTime,
        uc.code AS code,
        uc.language AS language
    FROM user_commit uc
    LEFT JOIN user u ON uc.uid = u.uuid
    LEFT JOIN commit_case cc ON uc.commit_id = cc.commit_id
    LEFT JOIN question q ON uc.qid = q.question_id
    <include refid="Commits_Where_Clause"/>
    ORDER BY uc.create_time DESC
    LIMIT #{page}, 15
</select>

<select id="getCommitsCount" resultType="java.lang.Integer">
    SELECT COUNT(*)
    FROM user_commit uc
    LEFT JOIN user u ON uc.uid = u.uuid
    LEFT JOIN commit_case cc ON uc.commit_id = cc.commit_id
    LEFT JOIN question q ON uc.qid = q.question_id
    <include refid="Commits_Where_Clause"/>
</select>
```

**数据库索引建议：**
- `user_commit(uid, create_time)` 复合索引
- `user_commit(language)` 索引
- `question(question_name)` 前缀索引或全文索引
- `commit_case(commit_id, cn_name)` 复合索引

---

## 五、通用优化建议

### 1. 索引优化
建议创建以下索引（根据实际查询频率调整）：

```sql
-- user_commit 表
CREATE INDEX idx_uid_createtime ON user_commit(uid, create_time DESC);
CREATE INDEX idx_commit_id ON user_commit(commit_id);
CREATE INDEX idx_qid ON user_commit(qid);
CREATE INDEX idx_language ON user_commit(language);

-- question 表
CREATE INDEX idx_source ON question(question_source);
CREATE INDEX idx_difficulty ON question(difficulty);
CREATE INDEX idx_question_name ON question(question_name(20));  -- 前缀索引
CREATE INDEX idx_modify_time ON question(modify_time DESC);

-- commit_case 表
CREATE INDEX idx_commit_id ON commit_case(commit_id);
CREATE INDEX idx_cn_name ON commit_case(cn_name);

-- user 表
CREATE INDEX idx_email ON user(email);
CREATE INDEX idx_uuid ON user(uuid);  -- 主键通常已有索引
```

### 2. 查询优化原则
1. **避免 SELECT ***：明确指定需要的列，特别是避免查询大字段（LONGVARCHAR/TEXT）
2. **合理使用索引**：为 WHERE、JOIN、ORDER BY 的字段创建索引
3. **优化 LIKE 查询**：尽量使用前缀匹配 `LIKE 'prefix%'` 或全文索引
4. **减少不必要的 JOIN**：只 JOIN 需要的表
5. **使用 EXISTS 替代子查询**：在某些情况下性能更好
6. **避免在 WHERE 子句中使用函数**：如 `DATE(create_time)`，改用范围查询

### 3. 分页优化
- 对于大数据量分页，考虑使用游标分页（基于 ID）替代 OFFSET
- 如果必须使用 OFFSET，确保有合适的索引支持 ORDER BY

### 4. 代码层面优化
- 提取公共 SQL 片段，使用 `<sql>` 和 `<include>` 标签
- 统一查询逻辑，避免 COUNT 和 SELECT 查询条件不一致

