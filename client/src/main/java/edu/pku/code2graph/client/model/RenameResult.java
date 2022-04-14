package edu.pku.code2graph.client.model;

import java.util.List;

public class RenameResult {
  RenameStatusCode status; // 状态码，如success, failed, ...
  List<RenameInfo> renameInfoList;

  public RenameStatusCode getStatus() {
    return status;
  }

  public void setStatus(RenameStatusCode status) {
    this.status = status;
  }

  public List<RenameInfo> getRenameInfoList() {
    return renameInfoList;
  }

  public void setRenameInfoList(List<RenameInfo> renameInfoList) {
    this.renameInfoList = renameInfoList;
  }
}
