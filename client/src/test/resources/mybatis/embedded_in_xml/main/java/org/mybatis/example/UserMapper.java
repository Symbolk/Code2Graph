package org.mybatis.example;
import org.mybatis.example.User;

/**
 * Created by ywata on 1/4/14.
 */
public interface UserMapper {
    User select();
    int insertUser(User user);
}
