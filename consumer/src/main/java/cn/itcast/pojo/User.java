package cn.itcast.pojo;

import lombok.Data;

/**
 * Description: User
 * <br></br>
 * className: User
 * <br></br>
 * packageName: cn.itcast.pojo
 *
 * @author jinhui-huang
 * @version 1.0
 * @email 2634692718@qq.com
 * @Date: 2023/11/25 20:15
 */
@Data
public class User {
    private String username;
    private String password;
    private String phone;
    private Integer age;
}
