package com.mizookie.packagemapper.dto.user;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DependencyGraphResponse {
    String source;
    String target;
    String type;

    public DependencyGraphResponse(String s, String t) {
        this.source = s;
        this.target = t;
        this.type = "import";
    }
}
