package com.bin.actx.message;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话集合
 *
 * @author Bin
 * @since 2026/5/15 16:48
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionCollection implements Serializable {

    private String userId;

    private List<SessionInfo> sessions;

}
