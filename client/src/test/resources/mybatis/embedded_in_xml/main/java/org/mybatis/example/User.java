package org.mybatis.example;

/**
 * Created by ywata on 1/4/14.
 */
public class User {
    private int userid;
    private String username;
    private String email;
    private String tel;
    private String password;
    void setUserid(int uid){
        userid = uid;
    }
    void setUsername(String un){
        username = un;
    }
    void setEmail(String em){
        email = em;
    }
    void setTel(String tl){
        tel = tl;
    }
    void setPassword(String pw){
        password = pw;
    }
}
