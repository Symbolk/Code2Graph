package org.mybatis.example;

/**
 * Created by ywata on 1/4/14.
 */
public class Group {
    private int groupid;
    private String groupname;
    void setGroupid(int gid){
        groupid = gid;
    }
    int getGroupid(){
        return groupid;
    }

    void setGroupname(String gn){
        groupname = gn;
    }
    String getGroupname(){
        return groupname;
    }
}
