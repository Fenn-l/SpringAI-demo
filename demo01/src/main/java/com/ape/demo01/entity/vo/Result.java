package com.ape.demo01.entity.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口返回结果（前后端约定的简单结构）。
 *
 * <p>ok=1 表示成功，ok=0 表示失败；msg 为提示信息。
 * 用于 PDF 上传等接口的 JSON 返回。
 */
@Data
@NoArgsConstructor
public class Result {

    /** 状态码：1 成功 / 0 失败。 */
    private Integer ok;

    /** 提示信息。 */
    private String msg;

    /** 私有构造，外部统一走静态工厂方法。 */
    private Result(Integer ok, String msg) {
        this.ok = ok;
        this.msg = msg;
    }

    /** 成功（无额外信息）。 */
    public static Result ok() {
        return new Result(1, "ok");
    }

    /** 失败，携带错误信息。 */
    public static Result fail(String msg) {
        return new Result(0, msg);
    }
}
