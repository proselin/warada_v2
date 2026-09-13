package com.github.warada_v2.shared.models;

import java.time.Instant;

public class ApiError {
  public String processId;
  public int status;
  public String message;
  public String timestamp;

  public ApiError(String processId, int status, String message) {
    this.processId = processId;
    this.status = status;
    this.message = message;
    this.timestamp = Instant.now().toString();
  }
}
