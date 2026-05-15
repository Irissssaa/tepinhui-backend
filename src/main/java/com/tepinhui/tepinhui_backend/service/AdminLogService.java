package com.tepinhui.tepinhui_backend.service;

import com.tepinhui.tepinhui_backend.dto.admin.AdminLogQueryRequest;
import com.tepinhui.tepinhui_backend.vo.admin.AdminLogPageVO;

public interface AdminLogService {

    AdminLogPageVO getLogs(AdminLogQueryRequest queryRequest);
}
