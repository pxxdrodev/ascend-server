package com.ascend.core.api.account;

import com.ascend.core.api.rank.Rank;
import com.ascend.core.api.tag.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class Account {

    private final UUID uniqueId;
    private final String username;
    private Rank rank;
    private Tag tag;
    private int level;

    public boolean isStaff() {
        return rank != null && rank.isStaff();
    }
}
